package ch.alchemists.jbmesh.lookup;

import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;

public interface VertexDeduplication {
    void addExisting(Vertex vertex);
    void clear();

    Vertex getVertex(Vector3f location);
    Vertex getOrCreateVertex(Vector3f location);
}
