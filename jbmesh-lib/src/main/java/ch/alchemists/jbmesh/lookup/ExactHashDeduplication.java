package ch.alchemists.jbmesh.lookup;

import com.jme3.math.Vector3f;
import java.util.HashMap;
import java.util.Map;
import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;

public class ExactHashDeduplication implements VertexDeduplication {
    private final Map<Vector3f, Vertex> map = new HashMap<>();
    private final Vec3Property<Vertex> propPosition;


    public ExactHashDeduplication(BMesh bmesh) {
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }

    public ExactHashDeduplication(Vec3Property<Vertex> propPosition) {
        this.propPosition = propPosition;
    }


    @Override
    public void addExisting(Vertex vertex) {
        Vector3f location = propPosition.get(vertex);
        map.put(location, vertex);
    }


    @Override
    public Vertex getVertex(Vector3f location) {
        return map.get(location);
    }


    @Override
    public Vertex getOrCreateVertex(BMesh bmesh, Vector3f location) {
        Vertex vertex = map.get(location);
        if(vertex == null) {
            vertex = bmesh.createVertex(location);
            map.put(location.clone(), vertex);
        }

        return vertex;
    }
}
