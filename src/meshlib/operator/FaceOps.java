package meshlib.operator;

import com.jme3.math.Vector3f;
import java.util.Iterator;
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
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    public Vector3f normal(Face face) {
        return normal(face, new Vector3f());
    }

    // Newell's Method that also works for concave polygons: https://www.khronos.org/opengl/wiki/Calculating_a_Surface_Normal
    public Vector3f normal(Face face, Vector3f store) {
        Loop current = face.loop;
        Loop next = current.nextFaceLoop;

        Vector3f vCurrent = new Vector3f();
        Vector3f vNext = new Vector3f();
        propPosition.get(current.vertex, vCurrent);
        store.zero();

        do {
            propPosition.get(next.vertex, vNext);

            store.x += (vCurrent.y - vNext.y) * (vCurrent.z + vNext.z);
            store.y += (vCurrent.z - vNext.z) * (vCurrent.x + vNext.x);
            store.z += (vCurrent.x - vNext.x) * (vCurrent.y + vNext.y);

            vCurrent.set(vNext);
            current = next;
            next = next.nextFaceLoop;
        } while(current != face.loop);

        return store.normalizeLocal();
    }


    public Vector3f centroid(Face face) {
        return centroid(face, new Vector3f());
    }

    public Vector3f centroid(Face face, Vector3f store) {
        int numVertices = 0;
        store.zero();

        for(Loop loop : face.loops()) {
            propPosition.add(loop.vertex, store);
            numVertices++;
        }

        return store.divideLocal(numVertices);
    }


    public boolean coplanar(Face face1, Face face2) {
        Vector3f normal1 = normal(face1);
        Vector3f normal2 = normal(face2);
        return normal1.dot(normal2) > 0.999f;
    }


    /**
     * Face needs to be planar.
     * @param face
     * @return Area of polygon.
     */
    public float area(Face face) {
        Vector3f normal = normal(face);
        return area(face, normal);
    }

    public float area(Face face, Vector3f normal) {
        Iterator<Loop> it = face.loops().iterator();
        Vertex firstVertex = it.next().vertex;

        Vector3f p1 = propPosition.get(firstVertex);
        Vector3f p2 = new Vector3f();
        Vector3f sum = new Vector3f();

        // Stoke's theorem? Green's theorem?
        while(it.hasNext()) {
            propPosition.get(it.next().vertex, p2);
            sum.addLocal( p1.crossLocal(p2) );
            p1.set(p2);
        }

        // Close loop. Will be zero if p1 == p2 (when face has only one side).
        propPosition.get(firstVertex, p2);
        sum.addLocal( p1.crossLocal(p2) );

        float area = sum.dot(normal) * 0.5f;
        return Math.abs(area);
    }


    public float areaTriangle(Face face) {
        Vector3f v0 = propPosition.get(face.loop.vertex);
        Vector3f v1 = v0.clone();
        propPosition.subtract(face.loop.nextFaceLoop.vertex, v0);
        propPosition.subtract(face.loop.nextFaceLoop.nextFaceLoop.vertex, v1);

        return v0.crossLocal(v1).length() * 0.5f;
    }
}
