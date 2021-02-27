package ch.alchemists.jbmesh.lookup;

import com.jme3.math.Vector3f;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;

public interface VertexDeduplication {
    void addExisting(Vertex vertex);
    Vertex getVertex(Vector3f location);
    Vertex getOrCreateVertex(BMesh bmesh, Vector3f location);
}
