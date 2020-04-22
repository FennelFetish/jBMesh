package meshlib.structure;

// BMEdge has no specific direction
public class Edge {
    private int index;

    // Target vertex (at end).
    // Needed? Can we use BMLoop's reference instead?
    // -> No, we need both vertices since an edge can exist without faces (no loop) and without adjacent edges (wireframe, single line, no nextEdge)
    public Vertex vertex0; // "Start" vertex
    public Vertex vertex1; // "End" vertex
    // Let's always normalize these. v0.index < v1.index

    // Disk cycle at start vertex.
    //Needed? Can we go through BMLoop instead? -> No, wireframe doesn't have loops
    // Never NULL
    private Edge v0NextEdge = this;

    // Disk cycle at end vertex
    // Never NULL
    private Edge v1NextEdge = this;

    // Can be null
    public Loop loop;

    private Edge() {}


    /*Edge(Vertex v0, Vertex v1) {
        vertex0 = v0;
        vertex1 = v1;

        assert v0.getIndex() != v1.getIndex();
        
        if(v0.getIndex() <= v1.getIndex()) {
            vertex0 = v0;
            vertex1 = v1;
        } else {
            vertex0 = v1;
            vertex1 = v0;
        }
    }*/


    void addLoop(Loop loop) {
        if(this.loop == null) {
            this.loop = loop;
            return;
        }

        // Insert loop at end of linked list
        Loop lastLoop = this.loop;
        while(lastLoop.nextEdgeLoop != this.loop)
            lastLoop = lastLoop.nextEdgeLoop;

        /*do {
            lastLoop = lastLoop.nextEdgeLoop;
        } while(lastLoop.nextEdgeLoop != this.loop);*/

        loop.nextEdgeLoop = this.loop;
        lastLoop.nextEdgeLoop = loop;
    }


    // void addEdge(Edge edge, Vertex vertex /* disk-cycle of vertex */ ) {
    /*void addEdge(Edge edge) {
        if(vertex0 == edge.vertex0) {
            this.addEdgeV0(edge);
            edge.addEdgeV0(this);
            edge.v0NextEdge = this;
        }
        else if(vertex0 == edge.vertex1) {
            this.addEdgeV0(edge);
            edge.addEdgeV1(this);
            edge.v1NextEdge = this;
        }
        else if(vertex1 == edge.vertex0) {
            this.addEdgeV1(edge);
            edge.addEdgeV0(this);
            edge.v0NextEdge = this;
        }
        else if(vertex1 == edge.vertex1) {
            this.addEdgeV1(edge);
            edge.addEdgeV1(this);
            edge.v1NextEdge = this;
        }
        else {
            throw new RuntimeException("Edges not adjacent");
        }
    }*/


    /*private void addEdgeV0(Edge edge) {
        if(v0NextEdge == this) {
            v0NextEdge = edge;
            // TODO: close disk cycle
            return;
        }

        Edge lastEdge = v0NextEdge;
        Edge nextEdge;
        while(lastEdge != this)
            nextEdge = lastEdge.getNextEdge(vertex0);

        Edge lastEdge = this;
        do {
            lastEdge = lastEdge.v0NextEdge;
        } while(lastEdge.v0NextEdge != this);

        lastEdge.v0NextEdge = edge;

        // edge.v ... =
        // ----> which edge?? How would you traverse this if they don't use the same reference name?
        // "However for any given vertex it may be represented by the v1 pointer in some edges in its disk cycle and the v2 pointer for others."
        // --> normally you'd traverse using the loops. We only need the disk cycle when traversing wireframes.
    }*/

    /*private void addEdgeV1(Edge edge) {
        if(v1NextEdge == null) {
            v1NextEdge = edge;
            return;
        }

        Edge lastEdge = this;
        do {
            lastEdge = lastEdge.v1NextEdge;
        } while(lastEdge.v1NextEdge != this);

        lastEdge.v1NextEdge = edge;
    }*/


    void setNextEdge(Vertex contactPoint, Edge edge) {
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
        return (vertex0 == v0 && vertex1 == v1) || (vertex0 == v1 && vertex1 == v0);
    }


    static final BMeshData.ElementAccessor<Edge> ACCESSOR = new BMeshData.ElementAccessor<Edge>() {
        @Override
        public Edge create() {
            return new Edge();
        }

        @Override
        public void release(Edge element) {
            element.index = -1;
            element.vertex0 = null;
            element.vertex1 = null;
            element.v0NextEdge = null;
            element.v1NextEdge = null;
            element.loop = null;
        }

        @Override
        public int getIndex(Edge element) {
            return element.index;
        }

        @Override
        public void setIndex(Edge element, int index) {
            element.index = index;
        }
    };
}
