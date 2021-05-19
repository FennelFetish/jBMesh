package ch.alchemists.jbmesh.structure;

import ch.alchemists.jbmesh.data.Element;
import java.util.Iterator;

public class Vertex extends Element {
    // Property names
    public static final String Position = "VertexPosition";
    public static final String Normal   = "VertexNormal";
    public static final String Color    = "VertexColor";


    // Can be null
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
        // TODO: Does this make modifications more difficult?
        if(edge.getNextEdge(this) != edge)
            throw new IllegalArgumentException("Edge already associated with a disk cycle for this Vertex");
        assert edge.getPrevEdge(this) == edge;

        // Insert edge at end of disk cycle
        if(this.edge == null)
            this.edge = edge;
        else
            edge.diskSetBetween(this, this.edge.getPrevEdge(this), this.edge);
    }
    

    public void removeEdge(Edge edge) {
        // Do this first so it will throw if edge is null or not adjacent
        Edge next = edge.getNextEdge(this);

        if(this.edge == edge) {
            if(next == edge) {
                // Edge was the only one in disk cycle
                assert edge.getPrevEdge(this) == edge;
                this.edge = null;
            } else {
                edge.diskRemove(this);
                this.edge = next;
            }

            return;
        }

        // Check for null so it will throw IllegalArgumentException and not NPE, regardless of this object's state
        if(this.edge != null) {
            // Check if 'edge' exists in disk cycle
            // TODO: Start from 'edge' and check if 'this.edge' is reachable? -> Less iterations?
            //       Or remove this check?
            Edge current = this.edge.getNextEdge(this);
            while(current != this.edge) {
                if(current == edge) {
                    edge.diskRemove(this);
                    return;
                }

                current = current.getNextEdge(this);
            }
        }

        throw new IllegalArgumentException("Edge does not exist in disk cycle for Vertex");
    }


    // TODO: Allow multiple Edges between two vertices?
    public Edge getEdgeTo(Vertex other) {
        if(edge == null)
            return null;

        Edge current = this.edge;
        do {
            if(current.connects(this, other))
                return current;
            current = current.getNextEdge(this);
        } while(current != this.edge);

        return null;
    }


    public Face getCommonFace(Vertex other) {
        for(Edge edgeThis : edges()) {
            for(Loop loopThis : edgeThis.loops()) {
                for(Edge edgeOther : other.edges()) {
                    for(Loop loopOther : edgeOther.loops()) {
                        if(loopThis.face == loopOther.face)
                            return loopThis.face;
                    }
                }
            }
        }

        return null;
    }


    public Iterable<Edge> edges() {
        return VertexEdgeIterator::new;
    }

    private class VertexEdgeIterator implements Iterator<Edge> {
        private Edge current;
        private boolean first;

        public VertexEdgeIterator() {
            current = Vertex.this.edge;
            first = (current != null);
        }

        @Override
        public boolean hasNext() {
            return (current != Vertex.this.edge) || first;
        }

        @Override
        public Edge next() {
            first = false;
            Edge edge = current;
            current = current.getNextEdge(Vertex.this);
            return edge;
        }
    }



    public Iterable<Face> faces() {
        return VertexFaceIterator::new;
    }

    private static class VertexFaceIterator implements Iterator<Face> {
        @Override
        public boolean hasNext() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }


        @Override
        public Face next() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
