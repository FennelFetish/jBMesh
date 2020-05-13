package meshlib.operator;

import com.jme3.math.Vector3f;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Edge;
import meshlib.structure.Vertex;

/**
 * Functions that depend on properties.
 */
public class EdgeOps {
    private final BMesh bmesh;
    private final Vec3Property<Vertex> propPosition;


    public EdgeOps(BMesh bmesh) {
        this.bmesh = bmesh;
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    public Vector3f calcCenter(Edge edge) {
        return calcCenter(edge, new Vector3f());
    }

    public Vector3f calcCenter(Edge edge, Vector3f store) {
        propPosition.get(edge.vertex0, store);
        propPosition.add(edge.vertex1, store);
        return store.multLocal(0.5f);
    }


    public boolean collinear(Edge edge1, Edge edge2) {
        // TODO: Are they only collinear if on the exact same line -> edges must be connected to eachother?

        Vector3f v1 = propPosition.get(edge1.vertex1);
        propPosition.subtract(edge1.vertex0, v1);
        v1.normalizeLocal();

        Vector3f v2 = propPosition.get(edge2.vertex1);
        propPosition.subtract(edge2.vertex0, v2);
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
        propPosition.subtract(edge.vertex1, d);
        return d.length();
    }
}
