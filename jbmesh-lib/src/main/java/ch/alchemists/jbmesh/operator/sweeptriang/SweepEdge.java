package ch.alchemists.jbmesh.operator.sweeptriang;

import com.jme3.math.Vector2f;

class SweepEdge {
    public SweepVertex start;
    public SweepVertex end;

    public SweepVertex lastVertex;
    public SweepVertex lastMergeVertex;

    private float xChange;


    public SweepEdge(SweepVertex start, SweepVertex end) {
        reset(start, end);
    }


    public void reset(SweepVertex start, SweepVertex end) {
        this.start = start;
        this.end = end;

        Vector2f segDiff = end.p.subtract(start.p);
        if(Math.abs(segDiff.y) < 0.0001f)
            xChange = 0;
        else
            xChange = segDiff.x / segDiff.y;
    }


    public float getXAtY(float y) {
        float yDiff = y - end.p.y;
        float x = end.p.x + yDiff*xChange;
        return x;
    }


    @Override
    public String toString() {
        return "SweepEdge{high: " + start.p + ", low: " + end.p + ", lastVertex: " + lastVertex + ", lastMerge: " + lastMergeVertex + "}";
    }
}
