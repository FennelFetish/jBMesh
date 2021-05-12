package ch.alchemists.jbmesh.operator.sweeptriang;

import com.jme3.math.Vector2f;

class SweepEdge {
    public final SweepVertex start;
    public final SweepVertex end;

    private final float xChange;

    public SweepInterval interval; // Inside polygon, TODO: Needed?


    public SweepEdge(SweepVertex start, SweepVertex end, SweepInterval interval) {
        this.start = start;
        this.end = end;
        this.interval = interval;

        Vector2f segDiff = end.p.subtract(start.p);
        if(Math.abs(segDiff.y) < 0.0001f)
            xChange = 0;
        else
            xChange = segDiff.x / -segDiff.y;
    }


    public float getXAtY(float y) {
        float yDiff = start.p.y - y;
        float x = start.p.x + yDiff*xChange;
        return x;
    }


    @Override
    public String toString() {
        return "SweepEdge{high: " + start.p + ", low: " + end.p + "}";
    }
}
