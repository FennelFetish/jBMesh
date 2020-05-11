package meshlib.lookup;

import com.jme3.math.Vector3f;
import java.util.HashMap;
import java.util.Map;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;

public class ExactHashDeduplication implements VertexDeduplication {
    // No epsilon, only hash table lookup

    private final Map<Vector3f, Vertex> map = new HashMap<>();


    public ExactHashDeduplication() {}


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
