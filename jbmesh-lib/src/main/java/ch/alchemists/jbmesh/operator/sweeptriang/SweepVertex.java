package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector2f;

public class SweepVertex implements Comparable<SweepVertex> {
    // These fields provide information to caller via SweepTriangulation.TriangleCallback
    public final Vertex vertex;
    public final int index;
    public final int face;

    final Vector2f p = new Vector2f();
    boolean leftTurn = false;
    SweepVertex next, prev;


    SweepVertex(Vertex vertex, int index, int face) {
        this.vertex = vertex;
        this.index = index;
        this.face = face;
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

        // The Collection compares key with itself as a check
        if(this == o)
            return 0;

        // This sorts vertices at bow-tie positions.
        // Merges must come before splits.
        // Because collinear degeneracies are removed beforehand, this shouldn't recurse.
        return prev.compareTo(o.prev);
    }


    public boolean isAbove(SweepVertex o) {
        if(p.y > o.p.y)
            return true;
        if(p.y < o.p.y)
            return false;

        return p.x > o.p.x;
    }


    @Override
    public String toString() {
        return "SweepVertex{" + p + "}";
    }
}
