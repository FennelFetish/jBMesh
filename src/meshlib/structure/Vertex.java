package meshlib.structure;

import meshlib.data.Element;

public class Vertex extends Element {
    // Can be NULL
    public Edge edge;


    Vertex() {}


    @Override
    public void releaseElement() {
        edge = null;
    }


    /**
     * Inserts Edge at end of disk cycle at this Vertex;
     * @param edge A newly created Edge which is adjacent to this Vertex.
     */
    public void addEdge(Edge edge) {
        if(this.edge == null) {
            if(!edge.isAdjacentTo(this))
                throw new IllegalArgumentException("Edge is not adjacent to vertex");

            this.edge = edge;
            return;
        }

        // Find last edge of disk cycle at this vertex
        // TODO: Insert at beginning instead, because O(1)?
        //       -> Doesn't work without "prev" references because we have to iterate and find the previous node anyway.
        Edge lastEdge;
        Edge nextEdge = this.edge;
        do {
            if(nextEdge == edge)
                throw new IllegalArgumentException("Edge already exists in disk cycle for vertex"); // Return false instead?

            lastEdge = nextEdge;
            nextEdge = lastEdge.getNextEdge(this);
        } while(nextEdge != this.edge);

        edge.setNextEdge(this, this.edge);
        lastEdge.setNextEdge(this, edge);

        // TODO: Check/modify edge.v0NextEdge/v1NextEdge?
        // Why would parameter 'edge' already have a disk cycle at this vertex?
        // 'edge' must be a newly constructed object
    }


    public Edge getEdgeTo(Vertex other) {
        if(edge == null)
            return null;

        Edge currentEdge = this.edge;
        do {
            if(currentEdge.connects(this, other))
                return currentEdge;
            currentEdge = currentEdge.getNextEdge(this);
        } while(currentEdge != this.edge);

        return null;
    }
}
