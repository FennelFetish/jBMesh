package ch.alchemists.jbmesh.operator.triangulation;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
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
    private final Vec3Attribute<Vertex> positions;


    public SeidelTriangulation(BMesh bmesh) {
        this.bmesh = bmesh;
        this.positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
    }


    public void apply(Face face) {
        System.out.println("SeidelTriangulation.apply ----------------------------------------------------");

        PlanarCoordinateSystem coordSys = new PlanarCoordinateSystem().forFace(face, positions);

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

        Vector3f p0 = new Vector3f();
        Vector3f p1 = new Vector3f();

        for(Loop l : loops) {
            DebugVisual.next("Seidel");

            positions.get(l.edge.vertex0, p0);
            Vector2f v0 = coordSys.project(p0);

            positions.get(l.edge.vertex1, p1);
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
