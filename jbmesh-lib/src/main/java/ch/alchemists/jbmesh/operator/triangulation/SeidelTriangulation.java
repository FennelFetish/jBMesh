package ch.alchemists.jbmesh.operator.triangulation;

import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.operator.FaceOps;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.DebugVisual;
import ch.alchemists.jbmesh.util.PlanarCoordinateSystem;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class SeidelTriangulation {
    private final BMesh bmesh;
    private final FaceOps faceOps;
    private final Vec3Property<Vertex> propPosition;


    public SeidelTriangulation(BMesh bmesh) {
        this.bmesh = bmesh;
        this.faceOps = new FaceOps(bmesh);
        this.propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    public void apply(Face face) {
        System.out.println("SeidelTriangulation.apply ----------------------------------------------------");

        Vector3f p0 = propPosition.get(face.loop.vertex);
        Vector3f p1 = propPosition.get(face.loop.nextFaceLoop.vertex);
        Vector3f n = faceOps.normal(face);
        PlanarCoordinateSystem coordSys = new PlanarCoordinateSystem(p0, p1, n);

        ArrayList<Loop> loops = new ArrayList<>();
        face.getLoops(loops);

        Random rnd = new Random(1234);
        Collections.shuffle(loops, rnd);

        DebugVisual.clear("Seidel");
        DebugVisual.setPointTransformation("Seidel", p -> {
            return coordSys.unproject(new Vector2f(p.x, p.y));
        });

        TrapezoidTree tree = new TrapezoidTree();
        tree.printTree();

        for(Loop l : loops) {
            DebugVisual.next("Seidel");

            propPosition.get(l.edge.vertex0, p0);
            Vector2f v0 = coordSys.project(p0);

            propPosition.get(l.edge.vertex1, p1);
            Vector2f v1 = coordSys.project(p1);

            System.out.println("Adding Edge " + p0 + " -> " + p1 + " (" + v0 + " -> " + v1 + ")");
            tree.addEdge(v0, v1);

            tree.printTree();

            //break;
        }

        //System.out.println("=== Finish ===");
        //tree.printTree();
    }
}
