package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.operator.normalgen.NewellNormal;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;
import java.util.Iterator;

/**
 * Geometric functions that depend on vertex position attribute.
 */
public class FaceOps {
    private final BMesh bmesh;
    private final Vec3Attribute<Vertex> positions;


    public FaceOps(BMesh bmesh) {
        this.bmesh = bmesh;
        positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
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
        positions.get(current.vertex, vCurrent);
        store.zero();

        do {
            positions.get(next.vertex, vNext);
            NewellNormal.addToNormal(store, vCurrent, vNext);

            vCurrent.set(vNext);
            current = next;
            next = next.nextFaceLoop;
        } while(current != face.loop);

        return store.normalizeLocal();
    }


    public Vector3f normalConvex(Face face) {
        return normalConvex(face.loop, new Vector3f());
    }

    public Vector3f normalConvex(Face face, Vector3f store) {
        return normalConvex(face.loop, store);
    }

    public Vector3f normalConvex(Loop loop, Vector3f store) {
        Vertex vertex = loop.vertex;
        Vertex vNext = loop.nextFaceLoop.vertex;
        Vertex vPrev = loop.prevFaceLoop.vertex;

        Vector3f v1 = positions.get(vertex);
        store.set(v1);
        positions.subtractLocal(store, vNext);
        positions.subtractLocal(v1, vPrev);

        return store.crossLocal(v1).normalizeLocal();
    }


    public Vector3f centroid(Face face) {
        return centroid(face, new Vector3f());
    }

    public Vector3f centroid(Face face, Vector3f store) {
        int numVertices = 0;
        store.zero();

        for(Vertex vertex : face.vertices()) {
            positions.addLocal(store, vertex);
            numVertices++;
        }

        return store.divideLocal(numVertices);
    }


    public boolean coplanar(Face face1, Face face2) {
        Vector3f normal1 = normal(face1);
        Vector3f normal2 = normal(face2);
        return normal1.dot(normal2) > 0.9999f;
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
        Iterator<Vertex> it = face.vertices().iterator();
        Vertex firstVertex = it.next();

        Vector3f p1 = positions.get(firstVertex);
        Vector3f p2 = new Vector3f();
        Vector3f sum = new Vector3f();

        // Stoke's theorem? Green's theorem?
        while(it.hasNext()) {
            positions.get(it.next(), p2);
            sum.addLocal( p1.crossLocal(p2) );
            p1.set(p2);
        }

        // Close loop. Will be zero if p1 == p2 (when face has only one side).
        positions.get(firstVertex, p2);
        sum.addLocal( p1.crossLocal(p2) );

        float area = sum.dot(normal) * 0.5f;
        return Math.abs(area);
    }


    public float areaTriangle(Face face) {
        Vector3f v0 = positions.get(face.loop.vertex);
        Vector3f v1 = v0.clone();
        positions.subtractLocal(v0, face.loop.nextFaceLoop.vertex);
        positions.subtractLocal(v1, face.loop.nextFaceLoop.nextFaceLoop.vertex);

        return v0.crossLocal(v1).length() * 0.5f;
    }


    public void makePlanar(Face face) {
        // TODO: ... Find plane with smallest deviation from existing vertices?
    }
}
