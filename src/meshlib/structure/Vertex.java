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
                throw new IllegalArgumentException("Edge is not adjacent to Vertex");

            this.edge = edge;
            return;
        }

        // Do this first so it will throw if edge is null or not adjacent
        edge.setNextEdge(this, this.edge);

        // Find last edge of disk cycle at this vertex
        // TODO: Insert at beginning instead, because O(1)?
        //       -> Doesn't work without "prev" references because we have to iterate and find the previous node anyway.
        Edge lastEdge;
        Edge current = this.edge;
        do {
            if(current == edge)
                throw new IllegalArgumentException("Edge already exists in disk cycle for Vertex"); // Return false instead?

            lastEdge = current;
            current = current.getNextEdge(this);
        } while(current != this.edge);
        lastEdge.setNextEdge(this, edge);

        // TODO: Check/modify edge.v0NextEdge/v1NextEdge?
        // Why would parameter 'edge' already have a disk cycle at this vertex?
        // 'edge' must be a newly constructed object
    }


    public void removeEdge(Edge edge) {
        // Do this first so it will throw if edge is null or not adjacent
        final Edge nextEdge = edge.getNextEdge(this);

        Edge prevEdge = this.edge;
        Edge current = this.edge.getNextEdge(this);
        while(current != this.edge) {
            if(current == edge) {
                prevEdge.setNextEdge(this, nextEdge);
                edge.setNextEdge(this, edge);
                return;
            }

            prevEdge = current;
            current = current.getNextEdge(this);
        }

        if(this.edge != edge)
            throw new IllegalArgumentException("Edge does not exists in disk cycle for Vertex"); // Return false instead?

        // 'edge' was the only one here, prevEdge == current, loop didn't run
        this.edge = null;
        edge.setNextEdge(this, edge);
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
