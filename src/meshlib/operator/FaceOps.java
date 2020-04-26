package meshlib.operator;

import com.jme3.math.Vector3f;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Loop;
import meshlib.structure.Vertex;

/**
 * Functions that depend on properties.
 */
public class FaceOps {
    private final BMesh bmesh;
    private final Vec3Property<Vertex> propPosition;


    public FaceOps(BMesh bmesh) {
        this.bmesh = bmesh;
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertexData());
    }


    public Vector3f calcNormal(Face face) {
        return calcNormal(face, new Vector3f());
    }

    public Vector3f calcNormal(Face face, Vector3f store) {
        Loop l1 = face.loop;
        Loop l2 = l1.nextFaceLoop; // center

        Vector3f edge1 = new Vector3f(); // l1 -> l2
        Vector3f edge2 = store;          // l3 -> l2

        // Find a corner where adjacent edges are not collinear
        do {
            Loop l3 = l2.nextFaceLoop;

            propPosition.get(l2.vertex, edge2);
            edge1.set(edge2);

            propPosition.subtract(l1.vertex, edge1);
            propPosition.subtract(l3.vertex, edge2);
            edge1.normalizeLocal();
            edge2.normalizeLocal();

            // Use abs() to catch not only straight continuations
            // but also degenerate corners where edges are collinear in opposite directions
            if(Math.abs(edge1.dot(edge2)) < 0.999f)
                return edge2.crossLocal(edge1);

            l1 = l2;
            l2 = l3;
        } while(l1 != face.loop);

        return store.zero();
    }


    public Vector3f calcCentroid(Face face) {
        return calcCentroid(face, new Vector3f());
    }

    public Vector3f calcCentroid(Face face, Vector3f store) {
        int numVertices = 0;
        store.zero();

        for(Loop loop : face.loops()) {
            propPosition.add(loop.vertex, store);
            numVertices++;
        }

        return store.divideLocal(numVertices);
    }


    public boolean coplanar(Face face1, Face face2) {
        Vector3f normal1 = calcNormal(face1);
        Vector3f normal2 = calcNormal(face2);
        return normal1.dot(normal2) > 0.999f;
    }
}
