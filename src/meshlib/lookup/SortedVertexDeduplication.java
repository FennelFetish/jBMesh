package meshlib.lookup;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;

// TODO: Use insertion sort or TreeSet instead
public class SortedVertexDeduplication {
    private class Entry {
        public final int index;
        public final Vector3f location = new Vector3f();
        public Vertex vertex = null;

        private Entry(int index, Vector3f location) {
            this.index = index;
            this.location.set(location);
        }
    }


    private final BMesh bmesh;

    private final Vector3f min = new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    private final Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

    private final List<Entry> entries = new ArrayList<>();
    private final Map<Integer, Entry> mapping = new HashMap<>();
    private int numLocations = 0;

    private float epsilon;
    private float epsilonSquared;


    public SortedVertexDeduplication(BMesh bmesh) {
        this(bmesh, 0.01f);
    }

    public SortedVertexDeduplication(BMesh bmesh, float range) {
        this.bmesh = bmesh;
        setRange(range);
    }


    public void setRange(float epsilon) {
        epsilonSquared = epsilon * epsilon;
    }


    public void add(int index, Vector3f location) {
        min.minLocal(location);
        max.maxLocal(location);

        Entry entry = new Entry(index, location);
        entries.add(entry);
    }


    public Vertex getVertex(int originalIndex) {
        return mapping.get(originalIndex).vertex;
    }


    public int map() {
        mapping.clear();
        numLocations = 0;

        Vector3f diff = max.subtract(min);
        if(diff.x >= diff.y) {
            if(diff.x >= diff.z)
                map(new AxisX());
            else if(diff.y >= diff.z)
                map(new AxisY());
            else
                map(new AxisZ());
        }
        else if(diff.y >= diff.z)
            map(new AxisY());
        else
            map(new AxisZ());

        return numLocations;
    }


    private void map(Axis axis) {
        entries.sort(axis);

        for(int i=0; i<entries.size(); ++i) {
            Entry e1 = entries.get(i);
            if(e1.vertex != null)
                continue;

            e1.vertex = bmesh.createVertex(e1.location);
            mapping.put(e1.index, e1);
            numLocations++;

            float p1 = axis.component(e1);

            // Backwards
            float range = p1-epsilon;
            for(int k=i-1; k>=0; --k) {
                Entry e2 = entries.get(k);
                float p2 = axis.component(e2);
                if(p2 < range)
                    break;

                tryAdd(e1, e2);
            }

            // Forward
            range = p1+epsilon;
            for(int k=i+1; k<entries.size(); ++k) {
                Entry e2 = entries.get(k);
                float p2 = axis.component(e2);
                if(p2 > range)
                    break;

                tryAdd(e1, e2);
            }
        }
    }


    private void tryAdd(Entry e, Entry candidate) {
        if(candidate.vertex != null)
            return;

        float d = e.location.distanceSquared(candidate.location);
        if(d <= epsilonSquared) {
            candidate.vertex = e.vertex;
            mapping.put(candidate.index, e);
        }
    }


    private static abstract class Axis implements Comparator<Entry> {
        public abstract float component(Entry e);
    }

    private static class AxisX extends Axis {
        @Override
        public int compare(Entry e1, Entry e2) {
            return Float.compare(e1.location.x, e2.location.x);
        }

        @Override
        public float component(Entry e) {
            return e.location.x;
        }
    }

    private static class AxisY extends Axis {
        @Override
        public int compare(Entry e1, Entry e2) {
            return Float.compare(e1.location.y, e2.location.y);
        }

        @Override
        public float component(Entry e) {
            return e.location.y;
        }
    }

    private static class AxisZ extends Axis {
        @Override
        public int compare(Entry e1, Entry e2) {
            return Float.compare(e1.location.z, e2.location.z);
        }

        @Override
        public float component(Entry e) {
            return e.location.z;
        }
    }
}
