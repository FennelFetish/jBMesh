package ch.alchemists.jbmesh.operator.sweeptriang;

class SweepEdge {
    public SweepVertex start;
    public SweepVertex end;

    public MonotoneSweep monotoneSweep;
    public MonotoneSweep lastMerge;

    private float xLeft;
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
            xLeft = end.p.x;
        }
        else {
            xChange = 0;
            xLeft = Math.min(end.p.x, start.p.x);
        }
    }


    public float getXAtY(float y) {
        float yDiff = y - end.p.y;
        float x = xLeft + yDiff*xChange;
        return x;
    }


    @Override
    public String toString() {
        int lastVertex = monotoneSweep.getLastVertex().index+1;
        int lastMergeVertex = (lastMerge != null) ? lastMerge.getLastVertex().index+1 : -1;
        return "SweepEdge{start: " + (start.index+1) + ", end: " + (end.index+1) + ", lastVertex: " + lastVertex + ", lastMerge: " + lastMergeVertex + "}";
    }
}
