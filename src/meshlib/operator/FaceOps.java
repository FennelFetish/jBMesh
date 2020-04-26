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

    // Newell's Method: https://www.khronos.org/opengl/wiki/Calculating_a_Surface_Normal
    public Vector3f calcNormal(Face face, Vector3f store) {
        Loop current = face.loop;
        Loop next = current.nextFaceLoop;

        store.zero();
        Vector3f vCurrent = new Vector3f();
        Vector3f vNext = new Vector3f();

        do {
            propPosition.get(current.vertex, vCurrent);
            propPosition.get(next.vertex, vNext);

            store.x += (vCurrent.y - vNext.y) * (vCurrent.z + vNext.z);
            store.y += (vCurrent.z - vNext.z) * (vCurrent.x + vNext.x);
            store.z += (vCurrent.x - vNext.x) * (vCurrent.y + vNext.y);

            current = next;
            next = next.nextFaceLoop;
        } while(current != face.loop);

        return store.normalizeLocal();
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
