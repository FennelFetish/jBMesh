package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.util.DebugVisual;
import com.jme3.math.Vector3f;
import java.util.Comparator;
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
            return (edge != null) ? edge.getXAtY(y) : x;
        }
    }


    private static final class KeyComparator implements Comparator<Key> {
        private float y = 0;

        public void setY(float y) {
            this.y = y;
        }

        @Override
        public int compare(Key k1, Key k2) {
            // Compare x with tolerance
            float dx = k1.getX(y) - k2.getX(y);
            if(dx > 0.0001f)
                return 1;
            if(dx < -0.0001f)
                return -1;

            // Allow duplicate keys for bow-tie vertices at same position.
            // Keys for getEdge() and removeEdge() must be >= than requested result.
            if(k1.edge == null)
                return 1;
            if(k1.edge == k2.edge)
                return 0;
            if(k2.edge == null)
                return -1;

            // Starts must come before splits
            return k1.edge.start.compareTo(k2.edge.start);
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


    public SweepEdge getEdge(SweepVertex v) {
        Key key = new Key(v.p.x);
        comparator.setY(v.p.y);
        key = edges.floor(key);
        return (key == null) ? null : key.edge;
    }


    public SweepEdge removeEdge(SweepVertex v) {
        Key key = new Key(v.p.x);
        comparator.setY(v.p.y);

        // Get and remove first edge that is <= key
        Key adjacent = edges.floor(key);
        assert adjacent.edge.end == v;

        boolean removeSuccess = edges.remove(adjacent);
        assert removeSuccess;

        return adjacent.edge;
    }


    public void drawSweepSegments(float y) {
        DebugVisual dbg = DebugVisual.get("SweepTriangulation");

        int i = 1;
        for(Key k : edges) {
            float x1 = k.edge.getXAtY(y);

            float x2 = x1 + 0.2f;
            if(k.edge.rightEdge != null)
                x2 = k.edge.rightEdge.getXAtY(y);

            Vector3f p1 = new Vector3f(x1, y, 0);
            Vector3f p2 = new Vector3f(x2, y, 0);
            dbg.addText(p1, "" + i);
            dbg.addLine(p1, p2);
            i++;
        }
    }

    public void printEdges(float y) {
        System.out.println("--- Edges:");
        for(Key k : edges) {
            System.out.println("  - " + k.edge + " (x=" + k.edge.getXAtY(y) + ")");
        }
    }
}
