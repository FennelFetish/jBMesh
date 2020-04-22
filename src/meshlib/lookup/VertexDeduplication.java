package meshlib.lookup;

import com.jme3.math.Vector3f;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;

public interface VertexDeduplication {
    Vertex getOrCreateVertex(BMesh mesh, Vector3f location);
}
