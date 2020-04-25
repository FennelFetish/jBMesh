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
     * Inserts Edge at end of disk cycle at this Vertex.
     * @param edge A newly created Edge which is adjacent to this Vertex.
     */
    public void addEdge(Edge edge) {
        // Edge cannot already belong to a disk cycle
        // Will throw if edge is null or not adjacent
        if(edge.getNextEdge(this) != edge)
            throw new IllegalArgumentException("Edge already associated with a disk cycle for this Vertex");

        if(this.edge == null) {
            this.edge = edge;
            return;
        }

        // Find last edge of disk cycle at this vertex
        // TODO: Insert at beginning instead, because O(1)?
        //       -> Doesn't work without "prev" references because we have to iterate and find the previous node anyway.
        Edge prevEdge = this.edge.getPrevEdge(this);
        prevEdge.setNextEdge(this, edge);
        edge.setNextEdge(this, this.edge);
    }
    

    public void removeEdge(Edge edge) {
        // Do this first so it will throw if edge is null or not adjacent
        Edge nextEdge = edge.getNextEdge(this);

        if(this.edge == edge) {
            if(nextEdge == edge) {
                // Edge was the only one in disk cycle
                edge.setNextEdge(this, edge);
                this.edge = null;
            }
            else {
                Edge prev = edge.getPrevEdge(this); // Could continue search from 'nextEdge'
                prev.setNextEdge(this, nextEdge);
                edge.setNextEdge(this, edge);
                this.edge = nextEdge;
            }

            return;
        }

        // Check for null so it will throw IllegalArgumentException and not NPE, regardless of this object's state
        if(this.edge != null) {
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
        }

        throw new IllegalArgumentException("Edge does not exists in disk cycle for Vertex");
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
