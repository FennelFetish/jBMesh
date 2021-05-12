package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.util.DebugVisual;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

class SweepVertex implements Comparable<SweepVertex> {
    public final int index;
    public final Vector2f p = new Vector2f();

    public SweepVertex next, prev;
    public SweepVertex monotonePath;


    public SweepVertex(int index) {
        this.index = index;
    }


    public void connectMonotonePath(SweepVertex targetVertex) {
        System.out.println("Connecting monotone path from " + this + " to " + targetVertex);

        // Draw debug line for monotone paths
        Vector3f start = new Vector3f(p.x, p.y, 0);
        Vector3f end = new Vector3f(targetVertex.p.x, targetVertex.p.y, 0);
        DebugVisual.get("SweepTriangulation").addLine(start, end);

        monotonePath = targetVertex;
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

        return Float.compare(p.x, o.p.x);
        //return Integer.compare(index, o.index);
    }


    @Override
    public String toString() {
        return "SweepVertex{" + p + "}";
    }
}
