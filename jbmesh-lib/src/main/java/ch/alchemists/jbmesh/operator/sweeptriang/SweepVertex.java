package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector2f;

class SweepVertex implements Comparable<SweepVertex> {
    public final int index;
    public final Vector2f p = new Vector2f();

    public SweepVertex next, prev;
    public SweepVertex monotonePath;


    public SweepVertex(int index) {
        this.index = index;
    }

    @Override
    public int compareTo(SweepVertex o) {
        int yCompare = Float.compare(p.y, o.p.y);
        if(yCompare != 0)
            return yCompare;

        // If vertices are located at same Y coordinates, sort them by winding order.
        // This is required in SweepTriangulation.handleSweepVertex() which depends on next/prev references
        // to determine vertex type.
        if(o == prev)
            return 1;
        if(o == next)
            return -1;

        //return Float.compare(p.x, o.p.x);
        return Integer.compare(index, o.index);
    }
}
