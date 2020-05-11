package meshlib.lookup;

import com.jme3.math.Vector3f;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;

public interface VertexDeduplication {
    /*void add(int index, Vector3f location);
    Vertex getVertex(int originalIndex);*/

    Vertex getOrCreateVertex(BMesh bmesh, Vector3f location);
}
