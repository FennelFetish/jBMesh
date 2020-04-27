package meshlib.structure;

import java.util.Iterator;
import java.util.Objects;
import meshlib.data.Element;

/**
 * Has no specific direction.
 */
public class Edge extends Element {
    // Target vertex (at end).
    // Needed? Can we use BMLoop's reference instead?
    // -> No, we need both vertices since an edge can exist without faces (no loop) and without adjacent edges (wireframe, single line, no nextEdge)
    public Vertex vertex0;
    public Vertex vertex1;
    // Make those private? Add setter that checks for null?

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
    

    /**
     * Insert Loop at end of radial cycle of this edge.
     * @param loop A newly created Loop which is adjacent to this Edge.
     */
    public void addLoop(Loop loop) {
        assert loop.edge == this;

        if(this.loop == null) {
            this.loop = loop;
            return;
        }

        // Do this first so it will throw if loop is null
        loop.nextEdgeLoop = this.loop;

        // Insert loop at end of linked list
        Loop lastLoop = this.loop;
        while(lastLoop.nextEdgeLoop != this.loop)
            lastLoop = lastLoop.nextEdgeLoop;
        lastLoop.nextEdgeLoop = loop;
    }


    public void removeLoop(Loop loop) {
        // Throw NPE if loop is null
        if(loop.edge != this) {
            throw new IllegalArgumentException("Loop is not adjacent to Edge");
        }

        if(this.loop == loop) {
            if(loop.nextEdgeLoop == loop) {
                // Loop was the only one here
                this.loop = null;
            } else {
                Loop prev = loop.getPrevEdgeLoop();
                prev.nextEdgeLoop = loop.nextEdgeLoop;
                this.loop = loop.nextEdgeLoop;
                loop.nextEdgeLoop = loop;
            }

            return;
        }
        
        if(this.loop != null) {
            Loop prev = this.loop;
            Loop current = prev.nextEdgeLoop;
            while(current != this.loop) {
                if(current == loop) {
                    prev.nextEdgeLoop = current.nextEdgeLoop;
                    loop.nextEdgeLoop = loop;
                    return;
                }

                prev = current;
                current = current.nextEdgeLoop;
            }
        }

        throw new IllegalArgumentException("Loop does not exist in radial cycle of Edge");
    }


    public void setNextEdge(Vertex contactPoint, Edge edge) {
        Objects.requireNonNull(edge);

        if(contactPoint == vertex0)
            v0NextEdge = edge;
        else if(contactPoint == vertex1)
            v1NextEdge = edge;
        else
            throw new IllegalArgumentException("Edge is not adjacent to Vertex");
    }


    // Iterate disk cycle
    // TODO: find(Edge): Use iterators that also allow insertion/removal at position (with prev reference) -> better than a prev-reference because it also checks if edge exists in cycle
    //                   But it introduces object allocation

    public Edge getNextEdge(Vertex contactPoint) {
        if(contactPoint == vertex0)
            return v0NextEdge;
        else if(contactPoint == vertex1)
            return v1NextEdge;

        throw new IllegalArgumentException("Edge is not adjacent to Vertex");
    }


    public Edge getPrevEdge(Vertex contactPoint) {
        Edge prev = this;
        Edge current = getNextEdge(contactPoint);
        while(current != this) {
            prev = current;
            current = current.getNextEdge(contactPoint);
        }

        return prev;
    }


    public boolean connects(Vertex v0, Vertex v1) {
        if(v0 == null || v1 == null)
            return false;
        
        return (vertex0 == v0 && vertex1 == v1)
            || (vertex0 == v1 && vertex1 == v0);
    }


    /**
     * Checks whether this Edge is connected to Vertex <i>v</i>.
     * @param v Vertex
     * @return True if this Edge is adjacent to the supplied Vertex. False otherwise, or if the supplied Vertex is <i>null</i>.
     */
    public boolean isAdjacentTo(Vertex v) {
        return (v != null) && (vertex0 == v || vertex1 == v);
    }


    // TODO: It's possible that both vertices are the same - equals() ?
    public Vertex getCommonVertex(Edge other) {
        if(vertex0 == other.vertex0 || vertex0 == other.vertex1)
            return vertex0;
        else if(vertex1 == other.vertex0 || vertex1 == other.vertex1)
            return vertex1;

        return null;
    }


    public Iterable<Loop> loops() {
        return () -> new EdgeLoopIterator(loop);
    }


    private static class EdgeLoopIterator implements Iterator<Loop> {
        private final Loop startLoop;
        private Loop currentLoop;
        private boolean first;

        public EdgeLoopIterator(Loop loop) {
            startLoop = loop;
            currentLoop = loop;
            first = (loop != null);
        }

        @Override
        public boolean hasNext() {
            return currentLoop != startLoop || first;
        }

        @Override
        public Loop next() {
            first = false;
            Loop loop = currentLoop;
            currentLoop = currentLoop.nextEdgeLoop;
            return loop;
        }
    }
}
