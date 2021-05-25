package ch.alchemists.jbmesh.lookup;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;
import java.util.HashMap;
import java.util.Map;

public class ExactHashDeduplication implements VertexDeduplication {
    private final BMesh bmesh;
    private final Map<Vector3f, Vertex> map = new HashMap<>();
    private final Vec3Attribute<Vertex> positions;


    public ExactHashDeduplication(BMesh bmesh) {
        this.bmesh = bmesh;
        positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
    }

    public ExactHashDeduplication(BMesh bmesh, Vec3Attribute<Vertex> attrPosition) {
        this.bmesh = bmesh;
        this.positions = attrPosition;
    }


    @Override
    public void addExisting(Vertex vertex) {
        Vector3f p = positions.get(vertex);
        map.put(p, vertex);
    }


    @Override
    public void clear() {
        map.clear();
    }


    @Override
    public Vertex getVertex(Vector3f position) {
        return map.get(position);
    }


    @Override
    public Vertex getOrCreateVertex(Vector3f position) {
        Vertex vertex = map.get(position);
        if(vertex == null) {
            vertex = bmesh.createVertex(position);
            map.put(position.clone(), vertex);
        }

        return vertex;
    }
}
