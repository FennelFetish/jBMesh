// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.structure;

import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.util.LoopMapIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * An Edge connects two Vertex elements in the BMesh data structure.
 * It has no specific direction.<br><br>
 * Adjacent Faces are connected through the <i>Radial Cycle</i> formed by Loops.
 * They follow no particular order.<br><br>
 * In a manifold mesh, an Edge has two adjacent Faces.
 * The BMesh structure however also allows
 * <ul>
 * <li><code>0</code> (line mesh),</li>
 * <li><code>1</code> (mesh border) or</li>
 * <li><code>&gt;2</code> (T-structure)</li>
 * </ul>
 * adjacent Faces to form non-manifold meshes.
 */
public class Edge extends Element {
    // Can't use Loop's reference: We need both vertices since an edge can exist
    // without faces (no loop) and without adjacent edges (wireframe, single line, no nextEdge)
    // TODO: Make those private? Add setter that checks for null?
    /**
     * The first adjacent Vertex of this Edge.<br>
     * Edges are generally undirected.<br>
     * Never <code>null</code> on a valid object.
     */
    public Vertex vertex0; // Blender calls this 'v1'

    /**
     * The second adjacent Vertex of this Edge.<br>
     * Edges are generally undirected.<br>
     * Never <code>null</code> on a valid object.
     */
    public Vertex vertex1; // Blender calls this 'v2'


    // Disk cycle at first vertex.
    // Needed? Can we go through BMLoop instead? -> No, wireframe doesn't have loops
    // Never null on a valid object
    private Edge v0NextEdge = this; // Blender calls this v0DiskNext (v1_disk_link.next)
    private Edge v0PrevEdge = this; // v0DiskPrev

    // Disk cycle at second vertex
    // Never null on a valid object
    private Edge v1NextEdge = this; // v1DiskNext
    private Edge v1PrevEdge = this; // v1DiskPrev


    /**
     * Any Loop along this Edge.<br>
     * Can be <code>null</code> when there are no adjacent Faces (e.g. line meshes).
     */
    public Loop loop;


    Edge() {}


    @Override
    protected void releaseElement() {
        vertex0 = null;
        vertex1 = null;
        v0NextEdge = this;
        v0PrevEdge = this;
        v1NextEdge = this;
        v1PrevEdge = this;
        loop = null;
    }
    

    /**
     * Insert Loop at end of radial cycle of this edge.
     * @param loop A newly created Loop which is adjacent to this Edge.
     */
    public void addLoop(Loop loop) {
        assert loop.edge == this;
        Objects.requireNonNull(loop);

        // TODO: Is this check needed?
        // TODO: Also see if the 'exists already in cycle' check is needed in Vertex.addEdge. -> Could make manipulations more difficult.
        // This throws in BMesh.createFace() because the references are set before. Also at other places.
        /*if(loop.nextEdgeLoop != loop)
            throw new IllegalArgumentException("Loop already associated with a radial cycle"); // TODO: Add to unit test
        assert loop.prevEdgeLoop == loop;*/

        if(this.loop == null) {
            this.loop = loop;
            return;
        }

        // Insert loop at end of linked list
        loop.radialSetBetween(this.loop.prevEdgeLoop, this.loop);
    }


    public void removeLoop(Loop loop) {
        // Throw NPE if loop is null
        if(loop.edge != this)
            throw new IllegalArgumentException("Loop is not adjacent to Edge");

        if(this.loop == loop) {
            if(loop.nextEdgeLoop == loop) {
                // Loop was the only one here
                this.loop = null;
            } else {
                this.loop = loop.nextEdgeLoop;
                loop.radialRemove();
            }

            return;
        }

        // Check for null so it will throw IllegalArgumentException and not NPE, regardless of this object's state
        if(this.loop != null) {
            // Check if 'loop' exists in radial cycle
            Loop current = this.loop.nextEdgeLoop;
            while(current != this.loop) {
                if(current == loop) {
                    loop.radialRemove();
                    return;
                }

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

    public void setPrevEdge(Vertex contactPoint, Edge edge) {
        Objects.requireNonNull(edge);

        if(contactPoint == vertex0)
            v0PrevEdge = edge;
        else if(contactPoint == vertex1)
            v1PrevEdge = edge;
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
        if(contactPoint == vertex0)
            return v0PrevEdge;
        else if(contactPoint == vertex1)
            return v1PrevEdge;

        throw new IllegalArgumentException("Edge is not adjacent to Vertex");
    }


    /**
     * Updates the links in the disk cycle of <i>contactPoint</i> so that the following order is created:<br>
     * <pre>
     * Before: prev -&gt; next
     * After:  prev -&gt; this -&gt; next
     * </pre>
     * @param contactPoint
     * @param prev
     * @param next
     */
    void diskSetBetween(Vertex contactPoint, Edge prev, Edge next) {
        assert prev.getNextEdge(contactPoint) == next;
        assert next.getPrevEdge(contactPoint) == prev;

        if(contactPoint == vertex0) {
            v0NextEdge = next;
            v0PrevEdge = prev;
            prev.setNextEdge(contactPoint, this);
            next.setPrevEdge(contactPoint, this);
        }
        else if(contactPoint == vertex1) {
            v1NextEdge = next;
            v1PrevEdge = prev;
            prev.setNextEdge(contactPoint, this);
            next.setPrevEdge(contactPoint, this);
        }
        else
            throw new IllegalArgumentException("Edge is not adjacent to Vertex");
    }


    /**
     * Removes this Edge from the disk cycle. Links the previous and the next element to each other.
     * <pre>
     * Before: prev -&gt; this -&gt; next
     * After:  prev -&gt; next
     * </pre>
     * @param contactPoint Adjacent Vertex.
     */
    void diskRemove(Vertex contactPoint) {
        if(contactPoint == vertex0) {
            v0NextEdge.setPrevEdge(contactPoint, v0PrevEdge);
            v0PrevEdge.setNextEdge(contactPoint, v0NextEdge);
            v0NextEdge = this;
            v0PrevEdge = this;
        }
        else if(contactPoint == vertex1) {
            v1NextEdge.setPrevEdge(contactPoint, v1PrevEdge);
            v1PrevEdge.setNextEdge(contactPoint, v1NextEdge);
            v1NextEdge = this;
            v1PrevEdge = this;
        }
        else
            throw new IllegalArgumentException("Edge is not adjacent to Vertex");
    }


    public boolean connects(Vertex v0, Vertex v1) {
        if(v0 == null || v1 == null)
            return false;
        
        return (vertex0 == v0 && vertex1 == v1)
            || (vertex0 == v1 && vertex1 == v0);
    }


    /**
     * Checks whether this Edge is connected to Vertex <i>v</i>.
     * @param vertex Vertex
     * @return True if this Edge is adjacent to the supplied Vertex. False otherwise, or if the supplied Vertex is <i>null</i>.
     */
    public boolean isAdjacentTo(Vertex vertex) {
        return (vertex != null) && (vertex0 == vertex || vertex1 == vertex);
    }

    public boolean isAdjacentTo(Face face) {
        for(Loop loop : loops()) {
            if(loop.face == face)
                return true;
        }

        return false;
    }

    public boolean isAdjacentTo(Edge edge) {
        if(vertex0 == edge.vertex0 || vertex0 == edge.vertex1)
            return true;
        else if(vertex1 == edge.vertex0 || vertex1 == edge.vertex1)
            return true;

        return false;
    }


    // TODO: It's possible that both vertices are the same - equals() ?
    public Vertex getCommonVertex(Edge other) {
        if(vertex0 == other.vertex0 || vertex0 == other.vertex1)
            return vertex0;
        else if(vertex1 == other.vertex0 || vertex1 == other.vertex1)
            return vertex1;

        return null;
    }

    
    public Vertex getOther(Vertex vertex) {
        if(vertex == vertex0)
            return vertex1;
        else if(vertex == vertex1)
            return vertex0;
        else
            throw new IllegalArgumentException("Edge is not adjacent to Vertex");
    }

    public void setOther(Vertex contactPoint, Vertex vertex) {
        if(contactPoint == vertex0)
            vertex1 = vertex;
        else if(contactPoint == vertex1)
            vertex0 = vertex;
        else
            throw new IllegalArgumentException("Edge is not adjacent to Vertex");
    }

    public void replace(Vertex oldVertex, Vertex newVertex) {
        if(oldVertex == vertex0)
            vertex0 = newVertex;
        else if(oldVertex == vertex1)
            vertex1 = newVertex;
        else
            throw new IllegalArgumentException("Edge is not adjacent to Vertex");
    }


    public ArrayList<Face> getFaces() {
        return getFaces(new ArrayList<>(2));
    }

    public <C extends Collection<Face>> C getFaces(C collection) {
        for(Loop loop : loops())
            collection.add(loop.face);
        return collection;
    }

    public Iterable<Face> faces() {
        return () -> new LoopMapIterator<>(new EdgeLoopIterator(loop), loop -> loop.face);
    }


    public ArrayList<Loop> getLoops() {
        return getLoops(new ArrayList<>(2));
    }

    public <C extends Collection<Loop>> C getLoops(C collection) {
        for(Loop loop : loops())
            collection.add(loop);
        return collection;
    }

    public Iterable<Loop> loops() {
        return () -> new EdgeLoopIterator(loop);
    }


    private static final class EdgeLoopIterator implements Iterator<Loop> {
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
