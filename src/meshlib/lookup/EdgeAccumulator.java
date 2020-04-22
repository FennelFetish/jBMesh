package meshlib.lookup;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import meshlib.structure.Edge;
import meshlib.structure.Vertex;

public class EdgeAccumulator {
    // Doesn't work when vertices are moved after adding the Edge here
    //private final HashGrid<List<Edge>> edgeGrid = new HashGrid<>(0.01f);

    private final Map<Long, Edge> edges = new HashMap<>();


    public Edge getEdge(Vertex v0, Vertex v1) {
        long key = normalizedKey(v0, v1);
        return edges.get(key);
    }


    public void putEdge(Edge edge) {
        long key = key(edge.vertex0, edge.vertex1);
        edges.put(key, edge);
    }


    public void removeEdge(Edge edge) {
        long key = key(edge.vertex0, edge.vertex1);
        edges.remove(key);
    }


    private static long key(Vertex v0, Vertex v1) {
        long key = v0.getIndex();
        key <<= 32;
        key |= v1.getIndex();
        return key;
    }

    private static long normalizedKey(Vertex v0, Vertex v1) {
        if(v0.getIndex() <= v1.getIndex())
            return key(v0, v1);
        else
            return key(v1, v0);
    }


    public Collection<Edge> getEdges() {
        return Collections.unmodifiableCollection(edges.values());
    }
}
