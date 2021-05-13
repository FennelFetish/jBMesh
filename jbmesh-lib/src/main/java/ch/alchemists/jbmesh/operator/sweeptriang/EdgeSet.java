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

        @Override
        public String toString() {
            return "Key{" +
                "x=" + x +
                ", edge=" + edge +
                '}';
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

            // Allow duplicate keys for bow-tie vertices at same position
            if(k1.edge == null)
                return 1;
            if(k1.edge == k2.edge)
                return 0;
            if(k2.edge == null)
                return -1;

            return k1.edge.start.compareTo(k2.edge.start);

            //assert k2.edge == null;
            //return -1;
        }
    }


    private final KeyComparator comparator = new KeyComparator();
    private final TreeSet<Key> edges = new TreeSet<>(comparator);


    public void clear() {
        edges.clear();
    }


    public void addEdge(SweepEdge edge) {
        //System.out.println("Add edge: " + edge);

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


    public SweepEdge removeEndEdge(SweepVertex v) {
        Key key = new Key(v.p.x);
        comparator.setY(v.p.y);

        // Get and remove first edge that is <= key
        Key adjacent = edges.floor(key);
        assert adjacent.edge.end == v;
        //System.out.println("Removing " + adjacent.edge + " (key x=" + key.x + ")");
        boolean removeSuccess = edges.remove(adjacent);
        assert removeSuccess;

        return adjacent.edge;
    }


    // Fastest..?
    public SweepEdge removeEdge(SweepVertex v) {
        Key key = new Key(v.p.x);
        comparator.setY(v.p.y);

        // Get and remove first edge that is <= key
        Key adjacent = edges.floor(key);
        assert adjacent.edge.end == v;
        //System.out.println("Removing " + adjacent.edge + " (key x=" + key.x + ")");
        boolean removeSuccess = edges.remove(adjacent);
        assert removeSuccess;

        // Handle lastMergeVertex of deleted edge
        if(adjacent.edge.lastMergeVertex != null) {
            adjacent.edge.lastMergeVertex.connectMonotonePath(v);
            adjacent.edge.waitingMonotoneSweep.processLeft(v);

            adjacent.edge.monotoneSweep.processEnd(v);
            adjacent.edge.monotoneSweep = adjacent.edge.waitingMonotoneSweep;
        }
        else {
            adjacent.edge.monotoneSweep.processLeft(v);
        }

        // Return second edge to the left if it exists
        Key left = edges.floor(key);
        assert left != null;
        if(left == null)
            return null;

        if(left.edge.lastMergeVertex != null) {
            left.edge.lastMergeVertex.connectMonotonePath(v);
            left.edge.lastMergeVertex = v;

            left.edge.waitingMonotoneSweep.processEnd(v);
        }

        left.edge.waitingMonotoneSweep = adjacent.edge.monotoneSweep;
        left.edge.lastMergeVertex = v;
        return left.edge;
    }


    public SweepEdge removeEdgeAlt(SweepVertex v) {
        Key key = new Key(v.p.x);
        comparator.setY(v.p.y);

        // Get and remove first edge that is <= key
        NavigableSet<Key> lower = edges.headSet(key, true);
        SweepEdge adjacentEdge = lower.pollLast().edge;
        System.out.println("Removed " + adjacentEdge + " (key x=" + key.x + ")");
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
