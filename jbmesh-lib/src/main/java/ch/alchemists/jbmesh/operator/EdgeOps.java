package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Edge;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;

/**
 * Functions that depend on properties.
 */
public class EdgeOps {
    private final BMesh bmesh;
    private final Vec3Property<Vertex> propPosition;


    public EdgeOps(BMesh bmesh) {
        this.bmesh = bmesh;
        propPosition = Vec3Property.get(Vertex.Position, bmesh.vertices());
    }


    public Vector3f calcCenter(Edge edge) {
        return calcCenter(edge, new Vector3f());
    }

    public Vector3f calcCenter(Edge edge, Vector3f store) {
        propPosition.get(edge.vertex0, store);
        propPosition.addLocal(store, edge.vertex1);
        return store.multLocal(0.5f);
    }


    public boolean collinear(Edge edge1, Edge edge2) {
        // TODO: Are they only collinear if on the exact same line -> edges must be connected to eachother?

        Vector3f v1 = propPosition.get(edge1.vertex1);
        propPosition.subtractLocal(v1, edge1.vertex0);
        v1.normalizeLocal();

        Vector3f v2 = propPosition.get(edge2.vertex1);
        propPosition.subtractLocal(v2, edge2.vertex0);
        v2.normalizeLocal();

        return Math.abs(v1.dot(v2)) > 0.999f;
    }


    public Vertex splitAtCenter(Edge edge) {
        Vector3f center = calcCenter(edge);
        Vertex vertex = bmesh.splitEdge(edge);
        propPosition.set(vertex, center);
        return vertex;
    }


    public float length(Edge edge) {
        Vector3f d = propPosition.get(edge.vertex0);
        propPosition.subtractLocal(d, edge.vertex1);
        return d.length();
    }
}
