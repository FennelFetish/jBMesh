package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.util.DebugVisual;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

class SweepVertex implements Comparable<SweepVertex> {
    public final Vector2f p = new Vector2f();
    public final int index;

    //public final boolean leftTurn; // Precalculate?

    public SweepVertex next, prev;
    public SweepVertex monotonePath;


    public SweepVertex(int index) {
        this.index = index;
    }


    public void connectMonotonePath(SweepVertex targetVertex) {
        //System.out.println("Connecting monotone path from " + this + " to " + targetVertex);

        // Draw debug line for monotone paths
        Vector3f start = new Vector3f(p.x, p.y, 0);
        Vector3f end = new Vector3f(targetVertex.p.x, targetVertex.p.y, 0);
        DebugVisual.get("SweepTriangulation").addArrow(start, end);

        //assert monotonePath == null;
        monotonePath = targetVertex;
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
