package ch.alchemists.jbmesh.operator.sweeptriang;

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

        float dy = end.p.y - start.p.y;
        assert dy >= 0;

        if(dy >= 0.0001f) {
            float dx = end.p.x - start.p.x;
            xChange = dx / dy;
        }
        else
            xChange = 0;
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
