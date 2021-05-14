package ch.alchemists.jbmesh.operator.sweeptriang;

class SweepEdge {
    public SweepVertex start;
    public SweepVertex end;

    public MonotoneSweep monotoneSweep;
    public MonotoneSweep lastMerge;

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
        SweepVertex lastVertex = monotoneSweep.getLastVertex();
        SweepVertex lastMergeVertex = (lastMerge != null) ? lastMerge.getLastVertex() : null;
        return "SweepEdge{start: " + start.p + ", end: " + end.p + ", lastVertex: " + lastVertex + ", lastMerge: " + lastMergeVertex + "}";
    }
}
