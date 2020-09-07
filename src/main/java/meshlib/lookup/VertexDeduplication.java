package meshlib.lookup;

import com.jme3.math.Vector3f;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;

public interface VertexDeduplication {
    void addExisting(Vertex vertex);
    Vertex getVertex(Vector3f location);
    Vertex getOrCreateVertex(BMesh bmesh, Vector3f location);
}
