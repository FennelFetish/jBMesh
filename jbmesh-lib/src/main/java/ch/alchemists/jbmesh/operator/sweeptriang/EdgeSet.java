package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.util.DebugVisual;
import com.jme3.math.Vector3f;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

public class EdgeSet {
    private static final class Key {
        public final float x;
        public final SweepEdge edge;

        public Key(float x) {
            this.x = x;
            this.edge = null;
        }

        public Key(SweepEdge edge) {
            this.x = 0;
            this.edge = edge;
        }

        public float getX(float y) {
            if(edge != null)
                return edge.getXAtY(y);
            return x;
        }
    }


    private static final class KeyComparator implements Comparator<Key> {
        private float y = 0;

        public void setY(float y) {
            this.y = y;
        }

        @Override
        public int compare(Key k1, Key k2) {
            int comp = Float.compare(k1.getX(y), k2.getX(y));
            if(comp != 0)
                return comp;

            if(k1.edge == null)
                return 1;
            if(k1.edge == k2.edge)
                return 0;

            assert k2.edge == null;
            return -1;
        }
    }


    private final KeyComparator comparator = new KeyComparator();
    private final TreeSet<Key> edges = new TreeSet<>(comparator);


    public void clear() {
        edges.clear();
    }


    public void addEdge(SweepEdge edge) {
        Key key = new Key(edge);
        comparator.setY(edge.start.p.y);
        boolean success = edges.add(key);
        assert success;
    }


    public SweepEdge getEdge(float x, float y) {
        Key key = new Key(x);
        comparator.setY(y);
        key = edges.floor(key);
        return (key == null) ? null : key.edge;
    }


    // Fastest..?
    public SweepEdge removeEdge(SweepVertex v) {
        Key key = new Key(v.p.x);
        comparator.setY(v.p.y);

        // Get and remove first edge that is <= key
        Key remove = edges.floor(key);
        boolean removeSuccess = edges.remove(remove);
        assert removeSuccess;

        SweepEdge adjacentEdge = remove.edge;
        assert adjacentEdge.end == v;
        //System.out.println("removeEdge: Removed " + adjacentEdge);

        // Handle lastMergeVertex of deleted edge
        if(adjacentEdge.lastMergeVertex != null)
            adjacentEdge.lastMergeVertex.connectMonotonePath(v);

        // Return second edge to the left if it exists
        Key left = edges.floor(key);
        return (left == null) ? null : left.edge;
    }


    public SweepEdge removeEdgeAlt(SweepVertex v) {
        Key key = new Key(v.p.x);
        comparator.setY(v.p.y);

        // Get and remove first edge that is <= key
        NavigableSet<Key> lower = edges.headSet(key, true);
        SweepEdge adjacentEdge = lower.pollLast().edge;
        assert adjacentEdge.end == v;
        //System.out.println("removeEdge: Removed " + adjacentEdge);

        // Handle lastMergeVertex of deleted edge
        if(adjacentEdge.lastMergeVertex != null)
            adjacentEdge.lastMergeVertex.connectMonotonePath(v);

        // Return second edge to the left if it exists
        if(lower.isEmpty())
            return null;
        SweepEdge leftEdge = lower.last().edge;
        return leftEdge;
    }


    public void debug(float y) {
        DebugVisual dbg = DebugVisual.get("SweepTriangulation");

        int i = 1;
        for(Key k : edges) {
            float x = k.edge.getXAtY(y);
            Vector3f p = new Vector3f(x, y, 0);
            dbg.addText(p, "" + i);
            i++;
        }
    }

    public void printEdges() {
        System.out.println("--- Edges:");
        for(Key k : edges) {
            System.out.println("  - " + k.edge);
        }
    }
}
