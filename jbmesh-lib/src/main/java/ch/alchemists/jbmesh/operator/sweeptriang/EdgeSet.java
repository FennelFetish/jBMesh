package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.util.DebugVisual;
import com.jme3.math.Vector3f;
import java.util.*;

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

        /*@Override
        public boolean equals(Object o) {
            assert false;

            if(o == null)
                return false;

            return edge == ((Key)o).edge;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, edge);
        }*/
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


    public void addEdge(SweepEdge edge) {
        System.out.println("Adding edge: " + edge);

        Key key = new Key(edge);
        comparator.setY(edge.start.p.y);
        boolean success = edges.add(key);
        assert success;
    }


    public SweepEdge getEdge(float x, float y) {
        Key key = new Key(x);
        comparator.setY(y);
        key = edges.floor(key);

        if(key != null)
            return key.edge;
        return null;
    }


    public SweepEdge removeEdge(SweepVertex v) {
        System.out.println("removeEdge for " + v);

        Key key = new Key(v.p.x);
        comparator.setY(v.p.y);

        NavigableSet<Key> lower = edges.headSet(key, true);
        Iterator<Key> it = lower.descendingIterator();

        // Get and remove first edge that is <= key
        assert it.hasNext();
        SweepEdge adjacentEdge = it.next().edge;
        assert adjacentEdge.end == v;
        it.remove();
        System.out.println("removeEdge: Removed " + adjacentEdge);

        // Handle lastMergeVertex of deleted edge
        if(adjacentEdge.lastMergeVertex != null)
            adjacentEdge.lastMergeVertex.connectMonotonePath(v);

        // Return second edge to the left if it exists
        if(!it.hasNext())
            return null;
        SweepEdge leftEdge = it.next().edge;
        return leftEdge;
    }


    /*private final ArrayList<SweepEdge> edges = new ArrayList<>(8);


    public void addEdge(SweepEdge edge) {
        int index = binarySearch(edge.start.p.x, edge.start.p.y);
        edges.add(index, edge);
    }


    public SweepEdge getEdge(float x, float y) {
        int index = binarySearch(x, y);
        index -= 1;

        if(index >= 0 && index < edges.size())
            return edges.get(index);
        return null;
    }*/


    /**
     * @return Insertion point for a new edge. (return-1) is associated SweepEdge for the point.
     */
    /*private int binarySearch(float x, float y) {
        int left  = 0;
        int right = edges.size() - 1;

        while(left <= right) {
            int center = (left + right) >>> 1;
            float dx = x - edges.get(center).getXAtY(y);

            if(dx >= 0)
                left = center + 1;
            else
                right = center - 1;
        }

        return left;
    }*/



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
