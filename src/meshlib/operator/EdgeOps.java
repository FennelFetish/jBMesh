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
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertexData());
    }


    public Vector3f calcCenter(Edge edge) {
        Vector3f v0 = propPosition.get(edge.vertex0);
        Vector3f v1 = propPosition.get(edge.vertex1);
        return v0.addLocal(v1).multLocal(0.5f);
    }
}
