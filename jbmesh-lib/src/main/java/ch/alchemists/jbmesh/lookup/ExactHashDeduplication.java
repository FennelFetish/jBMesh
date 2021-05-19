package ch.alchemists.jbmesh.lookup;

import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;
import java.util.HashMap;
import java.util.Map;

public class ExactHashDeduplication implements VertexDeduplication {
    private final BMesh bmesh;
    private final Map<Vector3f, Vertex> map = new HashMap<>();
    private final Vec3Property<Vertex> propPosition;


    public ExactHashDeduplication(BMesh bmesh) {
        this.bmesh = bmesh;
        propPosition = Vec3Property.get(Vertex.Position, bmesh.vertices());
    }

    public ExactHashDeduplication(BMesh bmesh, Vec3Property<Vertex> propPosition) {
        this.bmesh = bmesh;
        this.propPosition = propPosition;
    }


    @Override
    public void addExisting(Vertex vertex) {
        Vector3f location = propPosition.get(vertex);
        map.put(location, vertex);
    }


    @Override
    public void clear() {
        map.clear();
    }


    @Override
    public Vertex getVertex(Vector3f location) {
        return map.get(location);
    }


    @Override
    public Vertex getOrCreateVertex(Vector3f location) {
        Vertex vertex = map.get(location);
        if(vertex == null) {
            vertex = bmesh.createVertex(location);
            map.put(location.clone(), vertex);
        }

        return vertex;
    }
}
