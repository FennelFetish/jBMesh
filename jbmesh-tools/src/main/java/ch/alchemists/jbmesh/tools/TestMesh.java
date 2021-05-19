package ch.alchemists.jbmesh.tools;

import ch.alchemists.jbmesh.conversion.Import;
import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.lookup.OptimizedGridDeduplication;
import ch.alchemists.jbmesh.lookup.VertexDeduplication;
import ch.alchemists.jbmesh.operator.*;
import ch.alchemists.jbmesh.operator.bool.Subtract;
import ch.alchemists.jbmesh.operator.meshgen.DistanceFunction;
import ch.alchemists.jbmesh.operator.meshgen.MarchingCube;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.Profiler;
import com.jme3.math.Vector3f;
import com.jme3.scene.shape.Sphere;
import java.util.List;

public class TestMesh {
    public static BMesh testSphere() {
        Sphere sphere = new Sphere(16, 16, 2.0f);
        BMesh bmesh = Import.convertExactMapped(sphere);

        Vec3Property<Vertex> propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
        List<Vertex> vertices = bmesh.vertices().getAll();
        for(Vertex v : vertices) {
            Vector3f pos = propPosition.get(v);
            if(pos.z < 0 /*|| pos.z > 0.7f || pos.x < 0.9f || pos.y < -0.2f*/)
                bmesh.removeVertex(v);
            else
                propPosition.modify(v, (p) -> {
                    //p.x = 1.0f;
                });
        }

        return bmesh;
    }


    public static BMesh crease() {
        BMesh bmesh = new BMesh();
        Vertex c0 = bmesh.createVertex(0, 1, 0);
        Vertex c1 = bmesh.createVertex(1, 1, 0);

        Vertex f1 = bmesh.createVertex(1, 1, -1);
        Vertex f2 = bmesh.createVertex(0, 1, -1);

        Vertex b1 = bmesh.createVertex(1, 0, 0);
        Vertex b2 = bmesh.createVertex(0, 0, 0);

        bmesh.createFace(c0, c1, f1, f2);
        bmesh.createFace(c1, c0, b2, b1);

        return bmesh;
    }


    public static void spikes(BMesh bmesh) {
        // Inset
        // TODO: This Inset operator doesn't create nice topology and that's probably the reason why the normals aren't smooth.
        //       Insead, it should subdive the face 2 times and use the resulting vertices for forming the inset. -> Make nice quad strips
        Inset inset = new Inset(bmesh, 0.6f, -0.4f);
        ScaleFace scale = new ScaleFace(bmesh, 0.8f);
        for(Face face : bmesh.faces().getAll()) {
            //if(Math.random() > 0.03f) continue;
            inset.apply(face);
            scale.apply(face);
            inset.apply(face);
            scale.apply(face);
        }

        /*SubdivideFace subdiv = new SubdivideFace(bmesh, 2);
        subdiv.apply(bmesh.faces().getAll());

        try(Profiler p = Profiler.start("Catmull-Clark")) {
            Smooth smooth = new Smooth(bmesh);
            for(int i = 0; i < 4; ++i)
                smooth.apply(bmesh.faces().getAll());

            // TODO: Make faces planar after smoothing?
        }*/

        // TODO: Operator for removing collinear loops (those that were generated using the edge split above)
        //       It would collapse all vertices which lie between exactly two collinear edges.
    }

    public static void hollow(BMesh bmesh) {
        Inset inset = new Inset(bmesh, 0.7f, -0.4f);
        Inset inset2 = new Inset(bmesh, 0.8f, 0.8f);
        ScaleFace scale = new ScaleFace(bmesh, 0.7f);
        ScaleFace scale2 = new ScaleFace(bmesh, 1.5f);
        for(Face face : bmesh.faces().getAll()) {
            //if(Math.random() > 0.03f) continue;
            inset.apply(face);
            scale.apply(face);
            inset2.apply(face);
            scale2.apply(face);
        }
    }


    public static void subdiv(BMesh bmesh) {
        try(Profiler p = Profiler.start("Subdiv")) {
            SubdivideFace subdiv = new SubdivideFace(bmesh, 1);
            subdiv.apply(bmesh.faces().getAll());
        }

        try(Profiler p = Profiler.start("Smooth")) {
            Smooth smooth = new Smooth(bmesh);
            //smooth.setProcessNonmanifolds(true);
            for(int i = 0; i < 3; ++i)
                smooth.apply(bmesh.faces().getAll());
        }
    }


    public static DistanceFunction dfunc() {
        //return new DistanceFunction.Plane(new Vector3f(0.0f, 0, 0), Vector3f.UNIT_X);
        //return new DistanceFunction.Plane(new Vector3f(0, 0, 0), new Vector3f(1, 0.8f, 0.3f).normalizeLocal());
        return new DistanceFunction.Sphere(new Vector3f(1.0f, 1.3f, 1.0f), 1.0f);
        //return new DistanceFunction.Ellipsoid(new Vector3f(1.0f, 0.5f, 1.0f), new Vector3f(1.0f, 0.3f, 1.0f));
        //return new DistanceFunction.Box(new Vector3f(1, 1.07f, 1), new Vector3f(0.7f, 0.3f, 0.7f));
    }


    public static void subtract(BMesh bmesh) {
        try(Profiler p = Profiler.start("Boolean Subtract")) {
            Subtract sub = new Subtract(bmesh, dfunc());
            sub.apply(bmesh.faces().getAll());
        }

        // TODO: Wrong normals because it can lead to concave faces
        //       Or not? Some normals are simply missing
        // -> It was because of remaining elements
    }


    public static BMesh marchingCubes(BMesh bmesh) {
        if(bmesh == null)
            bmesh = new BMesh();

        //VertexDeduplication dedup = new ExactHashDeduplication(bmesh);
        //VertexDeduplication dedup = new GridVertexDeduplication(bmesh, 0.001f);
        VertexDeduplication dedup = new OptimizedGridDeduplication(bmesh, 0.0001f);

        DistanceFunction dfunc = dfunc();

        Vector3f start = new Vector3f(-2f, -2f, -2f);
        Vector3f end   = new Vector3f(2f, 2f, 2f);
        Vector3f p     = start.clone();

        float cellSize = 0.05f;
        MarchingCube cube = new MarchingCube(bmesh, dedup, cellSize,true);
        for(; p.x <= end.x; p.x += cellSize) {
            p.y = start.y;
            for(; p.y <= end.y; p.y += cellSize) {
                p.z = start.z;
                for(; p.z <= end.z; p.z += cellSize) {
                    cube.setPosition(p);
                    cube.process(dfunc);
                }
            }
        }

        return bmesh;
    }
}
