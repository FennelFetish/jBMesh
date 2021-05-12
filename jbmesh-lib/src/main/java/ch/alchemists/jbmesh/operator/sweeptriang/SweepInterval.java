package ch.alchemists.jbmesh.operator.sweeptriang;

class SweepInterval {
    public SweepEdge leftEdge;
    public SweepEdge rightEdge;

    public SweepVertex lastVertex;
    public SweepVertex lastMergeVertex;

    //public LinkedList<SweepVertex> prevMergeVertices;

    public float xLeft  = 0;
    public float xRight = 0;


    public void updateX(float y) {
        xLeft  = leftEdge.getXAtY(y);
        xRight = rightEdge.getXAtY(y);
    }


    /*public void addMergeVertex(SweepVertex mergeVertex) {
        if(prevMergeVertices == null)
            prevMergeVertices = new LinkedList<>();

        prevMergeVertices.add(mergeVertex);
    }*/
}
