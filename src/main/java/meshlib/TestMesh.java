package meshlib;

import com.jme3.math.Vector3f;
import com.jme3.scene.shape.Sphere;
import java.util.List;
import meshlib.conversion.Import;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.operator.Inset;
import meshlib.operator.ScaleFace;
import meshlib.operator.Smooth;
import meshlib.operator.SubdivideFace;
import meshlib.operator.bool.Subtract;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Vertex;
import meshlib.util.Profiler;

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
                propPosition.execute(v, (p) -> {
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


    public static void smoothSpikes(BMesh bmesh) {
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

        SubdivideFace subdiv = new SubdivideFace(bmesh, 2);
        subdiv.apply(bmesh.faces().getAll());

        try(Profiler p = Profiler.start("Catmull-Clark")) {
            Smooth smooth = new Smooth(bmesh);
            for(int i = 0; i < 4; ++i)
                smooth.apply(bmesh.faces().getAll());

            // TODO: Make faces planar after smoothing?
        }

        // TODO: Operator for removing collinear loops (those that were generated using the edge split above)
        //       It would collapse all vertices which lie between exactly two collinear edges.
    }


    public static void subtract(BMesh bmesh) {
        /*SubdivideFace subdiv = new SubdivideFace(bmesh, 1);
        subdiv.apply(bmesh.faces().getAll());

        Smooth smooth = new Smooth(bmesh);
        for(int i = 0; i < 1; ++i)
            smooth.apply(bmesh.faces().getAll());*/

        //Subtract.DistanceFunc dfunc = new Subtract.Plane(new Vector3f(0.0f, 0, 0), Vector3f.UNIT_X);
        //Subtract.DistanceFunc dfunc = new Subtract.Plane(new Vector3f(0, 0, 0), new Vector3f(1, 0.2f, 0.3f).normalizeLocal());
        Subtract.DistanceFunc dfunc = new Subtract.Sphere(new Vector3f(1.0f, 1.3f, 1.0f), 1.0f);

        try(Profiler p = Profiler.start("Boolean Subtract")) {
            Subtract sub = new Subtract(bmesh, dfunc);
            sub.apply(bmesh.faces().getAll());
        }

        // TODO: Wrong normals because it can lead to concave faces
        //       Or not? Some normals are simply missing
        // -> It was because of remaining elements
    }
}
