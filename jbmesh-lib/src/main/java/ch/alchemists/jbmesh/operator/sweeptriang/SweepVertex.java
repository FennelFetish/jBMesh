package ch.alchemists.jbmesh.operator.sweeptriang;

import com.jme3.math.Vector2f;

class SweepVertex implements Comparable<SweepVertex> {
    public final Vector2f p = new Vector2f();
    public final int index;

    public SweepVertex next, prev;


    public SweepVertex(int index) {
        this.index = index;
    }


    @Override
    public int compareTo(SweepVertex o) {
        if(p.y > o.p.y)
            return 1;
        if(p.y < o.p.y)
            return -1;

        if(p.x > o.p.x)
            return 1;
        if(p.x < o.p.x)
            return -1;

        // Here, vertices are either at the same position
        // or at least one position is NaN.

        // This sorts vertices at bow-tie positions.
        // Merges must come before splits.
        if(prev != null && o.prev != null)
            return prev.compareTo(o.prev);

        // Sort by index during construction where 'prev' references are still missing.
        return (index > o.index) ? 1 : -1;
    }


    @Override
    public String toString() {
        return "SweepVertex{" + p + "}";
    }
}
