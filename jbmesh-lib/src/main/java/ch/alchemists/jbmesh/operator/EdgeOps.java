package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Edge;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;

/**
 * Geometric functions that depend on vertex position attribute.
 */
public class EdgeOps {
    private final BMesh bmesh;
    private final Vec3Attribute<Vertex> positions;


    public EdgeOps(BMesh bmesh) {
        this.bmesh = bmesh;
        positions = Vec3Attribute.get(Vertex.Position, bmesh.vertices());
    }


    public Vector3f calcCenter(Edge edge) {
        return calcCenter(edge, new Vector3f());
    }

    public Vector3f calcCenter(Edge edge, Vector3f store) {
        positions.get(edge.vertex0, store);
        positions.addLocal(store, edge.vertex1);
        return store.multLocal(0.5f);
    }


    public boolean collinear(Edge edge1, Edge edge2) {
        // TODO: Are they only collinear if on the exact same line -> edges must be connected to eachother?

        Vector3f v1 = positions.get(edge1.vertex1);
        positions.subtractLocal(v1, edge1.vertex0);
        v1.normalizeLocal();

        Vector3f v2 = positions.get(edge2.vertex1);
        positions.subtractLocal(v2, edge2.vertex0);
        v2.normalizeLocal();

        return Math.abs(v1.dot(v2)) > 0.999f;
    }


    public Vertex splitAtCenter(Edge edge) {
        Vector3f center = calcCenter(edge);
        Vertex vertex = bmesh.splitEdge(edge);
        positions.set(vertex, center);
        return vertex;
    }


    public float length(Edge edge) {
        Vector3f d = positions.get(edge.vertex0);
        positions.subtractLocal(d, edge.vertex1);
        return d.length();
    }
}
