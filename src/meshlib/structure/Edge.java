package meshlib.structure;

// BMEdge has no specific direction

import meshlib.data.Element;

public class Edge extends Element {
    // Target vertex (at end).
    // Needed? Can we use BMLoop's reference instead?
    // -> No, we need both vertices since an edge can exist without faces (no loop) and without adjacent edges (wireframe, single line, no nextEdge)
    public Vertex vertex0;
    public Vertex vertex1;

    // Disk cycle at start vertex.
    //Needed? Can we go through BMLoop instead? -> No, wireframe doesn't have loops
    // Never NULL
    private Edge v0NextEdge = this;

    // Disk cycle at end vertex
    // Never NULL
    private Edge v1NextEdge = this;

    // Can be null
    public Loop loop;


    Edge() {}


    @Override
    protected void releaseElement() {
        vertex0 = null;
        vertex1 = null;
        v0NextEdge = null;
        v1NextEdge = null;
        loop = null;
    }
    

    public void addLoop(Loop loop) {
        if(this.loop == null) {
            this.loop = loop;
            return;
        }

        // Insert loop at end of linked list
        Loop lastLoop = this.loop;
        while(lastLoop.nextEdgeLoop != this.loop)
            lastLoop = lastLoop.nextEdgeLoop;

        loop.nextEdgeLoop = this.loop;
        lastLoop.nextEdgeLoop = loop;
    }


    public void setNextEdge(Vertex contactPoint, Edge edge) {
        if(contactPoint == vertex0)
            v0NextEdge = edge;
        else if(contactPoint == vertex1)
            v1NextEdge = edge;
        else
            throw new IllegalArgumentException("Edge is not adjacent to contact point");
    }


    // Iterate disk cycle
    public Edge getNextEdge(Vertex contactPoint) {
        if(contactPoint == vertex0)
            return v0NextEdge;
        else if(contactPoint == vertex1)
            return v1NextEdge;

        throw new IllegalArgumentException("Edge is not adjacent to contact point");
    }


    public boolean connects(Vertex v0, Vertex v1) {
        return (vertex0 == v0 && vertex1 == v1)
            || (vertex0 == v1 && vertex1 == v0);
    }


    public boolean isAdjacentTo(Vertex v) {
        return vertex0 == v || vertex1 == v;
    }
}
