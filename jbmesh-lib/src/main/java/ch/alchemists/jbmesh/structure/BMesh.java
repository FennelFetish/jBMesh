package ch.alchemists.jbmesh.structure;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BMesh {
    private final BMeshData<Vertex> vertexData;
    private final BMeshData<Edge> edgeData;
    private final BMeshData<Face> faceData;
    private final BMeshData<Loop> loopData;

    private final Vec3Attribute<Vertex> attrPosition = new Vec3Attribute<>(BMeshAttribute.Position);

    private final transient ArrayList<Loop> tempLoops = new ArrayList<>(4);


    public BMesh() {
        vertexData = new BMeshData<>(Vertex::new);
        edgeData   = new BMeshData<>(Edge::new);
        faceData   = new BMeshData<>(Face::new);
        loopData   = new BMeshData<>(Loop::new);

        vertexData.addAttribute(attrPosition);
    }


    public BMeshData<Vertex> vertices() {
        return vertexData;
    }

    public BMeshData<Edge> edges() {
        return edgeData;
    }

    public BMeshData<Face> faces() {
        return faceData;
    }

    public BMeshData<Loop> loops() {
        return loopData;
    }


    public void compactData() {
        vertexData.compactData();
        edgeData.compactData();
        faceData.compactData();
        loopData.compactData();

        //tempLoops.trimToSize();
    }


    public void clear() {
        vertexData.clear();
        edgeData.clear();
        faceData.clear();
        loopData.clear();

        tempLoops.trimToSize();
    }


    /**
     * Creates a new vertex.
     * @return A new vertex.
     */
    public Vertex createVertex() {
        return vertexData.create();
    }

    public Vertex createVertex(float x, float y, float z) {
        Vertex vert = createVertex();
        attrPosition.set(vert, x, y, z);
        return vert;
    }

    public Vertex createVertex(Vector3f location) {
        return createVertex(location.x, location.y, location.z);
    }

    
    /**
     * Removes the given vertex and all adjacent edges and faces from the structure.
     * @param vertex
     */
    public void removeVertex(Vertex vertex) {
        try {
            assert tempLoops.isEmpty();

            // Iterate disk cycle for vertex
            for(Edge edge : vertex.edges()) {
                // Iterate radial cycle for edge
                for(Loop radialLoop : edge.loops()) {
                    // Gather loops and destroy face
                    if(radialLoop.face.isAlive()) {
                        radialLoop.face.getLoops(tempLoops);
                        faceData.destroy(radialLoop.face);
                    }
                }

                edge.getOther(vertex).removeEdge(edge);
                edgeData.destroy(edge);
            }

            for(Loop loop : tempLoops) {
                if(loop.edge.isAlive())
                    loop.edge.removeLoop(loop);
                loopData.destroy(loop);
            }
        }
        finally {
            tempLoops.clear();
        }

        vertexData.destroy(vertex);
    }


    /**
     * Creates a new edge between the given vertices.
     * @param v0
     * @param v1
     * @return A new edge.
     */
    public Edge createEdge(Vertex v0, Vertex v1) {
        assert v0 != v1;

        Edge edge = edgeData.create();
        edge.vertex0 = v0;
        edge.vertex1 = v1;
        v0.addEdge(edge);
        v1.addEdge(edge);

        return edge;
    }


    /**
     * Removes the given edge and all adjacent faces from the structure.
     * @param edge
     */
    public void removeEdge(Edge edge) {
        try {
            // Gather all loops from adjacent faces
            assert tempLoops.isEmpty();
            for(Loop loop : edge.loops()) {
                loop.face.getLoops(tempLoops);
                faceData.destroy(loop.face);
            }

            for(Loop loop : tempLoops) {
                loop.edge.removeLoop(loop);
                loopData.destroy(loop);
            }
        }
        finally {
            tempLoops.clear();
        }

        edge.vertex0.removeEdge(edge);
        edge.vertex1.removeEdge(edge);
        edgeData.destroy(edge);
    }


    /**
     * Creates a new face between the given vertices. The order of the vertices define the winding order of the face.<br>
     * If edges between vertices already exist, they are used for the resulting face. Otherwise new edges are created.
     * @param faceVertices
     * @return A new Face.
     */
    public Face createFace(List<Vertex> faceVertices) {
        if(faceVertices.size() < 3)
            throw new IllegalArgumentException("A face needs at least 3 vertices");

        try {
            assert tempLoops.isEmpty();
            for(Vertex v : faceVertices) {
                Objects.requireNonNull(v);
                tempLoops.add(loopData.create());
            }

            Face face = faceData.create();
            face.loop = tempLoops.get(0);

            Loop prevLoop = tempLoops.get(tempLoops.size()-1);
            Vertex vCurrent = faceVertices.get(0);

            for(int i=0; i<faceVertices.size(); ++i) {
                int nextIndex = (i+1) % faceVertices.size();
                Vertex vNext = faceVertices.get(nextIndex);

                Edge edge = vCurrent.getEdgeTo(vNext);
                if(edge == null)
                    edge = createEdge(vCurrent, vNext);

                Loop loop = tempLoops.get(i);
                loop.face = face;
                loop.edge = edge;
                loop.vertex = vCurrent;
                loop.nextFaceLoop = tempLoops.get(nextIndex);
                loop.prevFaceLoop = prevLoop;
                edge.addLoop(loop);

                prevLoop = loop;
                vCurrent = vNext;
            }
            
            return face;
        }
        catch(Throwable t) {
            for(Loop loop : tempLoops)
                loopData.destroy(loop);
            throw t;
        }
        finally {
            tempLoops.clear();
        }
    }

    /**
     * See {@link #createFace(List<Vertex>)}.
     */
    public Face createFace(Vertex... faceVertices) {
        return createFace(Arrays.asList(faceVertices));
    }


    /**
     * Removes the given face from the structure.
     * @param face
     */
    public void removeFace(Face face) {
        try {
            assert tempLoops.isEmpty();
            face.getLoops(tempLoops);

            for(Loop loop : tempLoops) {
                loop.edge.removeLoop(loop);
                loopData.destroy(loop);
            }
        }
        finally {
            tempLoops.clear();
        }

        faceData.destroy(face);
    }


    /**
     * Splits the edge into two:
     * <ul>
     * <li>Creates a new Edge (from <i>vNew</i> to <i>v1</i>).</li>
     * <li>Reference <i>edge.vertex1</i> changes to <i>vNew</i>.</li>
     * <li>Updates disk cycle accordingly.</li>
     * <li>Adds one additional Loop to all adjacent Faces, increasing the number of sides,<br>
     *     and adds these Loops to the radial cycle of the new Edge.</li>
     * </ul>
     * <pre>
     *                   edge
     * Before: (v0)================(v1)
     * After:  (v0)=====(vNew)-----(v1)
     *             edge
     * </pre>
     *
     * @param edge
     * @return A new Vertex (<i>vNew</i>) with default attributes (undefined position).
     */
    public Vertex splitEdge(final Edge edge) {
        // Throws early if edge is null
        Vertex v0 = edge.vertex0;
        Vertex v1 = edge.vertex1;
        Vertex vNew = vertexData.create();

        Edge newEdge = edgeData.create();
        newEdge.vertex0 = vNew;
        newEdge.vertex1 = v1;

        v1.removeEdge(edge);
        v1.addEdge(newEdge);

        edge.vertex1 = vNew;
        vNew.addEdge(edge);
        vNew.addEdge(newEdge);

        for(Loop loop : edge.loops()) {
            Loop newLoop = loopData.create();
            newLoop.edge = newEdge;
            newLoop.face = loop.face;
            newEdge.addLoop(newLoop);

            // Link newLoop to next or previous loop, matching winding order.
            if(loop.vertex == v0) {
                // Insert 'newLoop' in front of 'loop'
                // (v0)--loop-->(vNew)--newLoop-->(v1)
                newLoop.faceSetBetween(loop, loop.nextFaceLoop);
                newLoop.vertex = vNew;
            } else {
                assert loop.vertex == v1;

                // Insert 'newLoop' at the back of 'loop'
                // (v1)--newLoop-->(vNew)--loop-->(v0)
                newLoop.faceSetBetween(loop.prevFaceLoop, loop);
                newLoop.vertex = loop.vertex;
                loop.vertex = vNew;
            }
        }

        return vNew;
    }


    /**
     * Removes edge and vertex.<br>
     * Vertex <i>v</i>must be adjacent to <i>edge</i> and exactly one other Edge.
     * <pre>
     *              edge
     * Before: (tv)======(v)-----(ov)
     * After:  (tv)--------------(ov)
     * </pre>
     * @param edge Will be removed.
     * @param vertex (<i>v</i>) Will be removed.
     * @return True on success.
     */
    public boolean joinEdge(final Edge edge, final Vertex vertex) {
        // Do this first so it will throw if edge is null or not adjacent
        Edge keepEdge = edge.getNextEdge(vertex);

        // No other edges
        if(keepEdge == edge)
            return false;

         // Check if there are >2 edges in disk cycle of vertex
        if(keepEdge.getNextEdge(vertex) != edge)
            return false;

        Vertex tv = edge.getOther(vertex);
        tv.removeEdge(edge);
        vertex.removeEdge(keepEdge);
        keepEdge.replace(vertex, tv);
        tv.addEdge(keepEdge);

        // Iterate Loops in radial cycle.
        // 'edge' and 'keepEdge' will have same number of loops and they will be connected
        // but the order in the radial cycle can be different (?).
        if(edge.loop != null) {
            Loop tl = edge.loop;
            Loop ol = keepEdge.loop;

            do {
                if(ol.vertex == vertex)
                    ol.vertex = tv;
                ol = ol.nextEdgeLoop;

                if(tl.face.loop == tl)
                    tl.face.loop = tl.nextFaceLoop;

                Loop loopRemove = tl;
                tl = tl.nextEdgeLoop;
                loopRemove.faceRemove();
                loopData.destroy(loopRemove);
            } while(tl != edge.loop);

            assert ol == keepEdge.loop;
        }

        edgeData.destroy(edge);
        vertexData.destroy(vertex);
        return true;
    }


    public Edge splitFace(Vertex vertex1, Vertex vertex2) {
        Face face = vertex1.getCommonFace(vertex2);
        if(face == null)
            throw new IllegalArgumentException("Vertices are not adjacent to a common face");

        return splitFace(face, vertex1, vertex2);
    }


    /**
     * Existing face is on right side, new face will be on left side of new edge (seen from vertex1 while looking at vertex2).
     * @param face
     * @param vertex1
     * @param vertex2
     * @return
     */
    public Edge splitFace(Face face, Vertex vertex1, Vertex vertex2) {
        assert vertex1 != vertex2;

        try {
            assert tempLoops.isEmpty();

            // Find v2
            Loop l2 = null;
            Loop loop = face.loop;
            do {
                if(loop.vertex == vertex2) {
                    l2 = loop;
                    break;
                }

                loop = loop.nextFaceLoop;
            } while(loop != face.loop);

            if(l2 == null)
                throw new IllegalArgumentException("Vertices are not adjacent to the given face");

            // Continue from v2 and find v1
            Loop l1 = null;
            do {
                if(loop.vertex == vertex1) {
                    l1 = loop;
                    break;
                }

                tempLoops.add(loop);
                loop = loop.nextFaceLoop;
            } while(loop != l2);

            if(l1 == null)
                throw new IllegalArgumentException("Vertices are not adjacent to the given face");

            Edge newEdge = createEdge(vertex1, vertex2);
            Loop l1Prev = l1.prevFaceLoop;
            Loop l2Prev = l2.prevFaceLoop;

            Loop l1New = loopData.create();
            l1New.face = face;
            l1New.edge = newEdge;
            l1New.vertex = vertex2;
            l1New.faceSetBetween(l2Prev, l1);

            Face newFace = faceData.create();
            for(Loop loopF2 : tempLoops)
                loopF2.face = newFace;

            Loop l2New = loopData.create();
            l2New.face = newFace;
            l2New.edge = newEdge;
            l2New.vertex = vertex1;
            l2New.faceSetBetween(l1Prev, l2);

            face.loop = l1New;
            newFace.loop = l2New;
            
            newEdge.addLoop(l1New);
            newEdge.addLoop(l2New);
            return newEdge;
        }
        finally {
            tempLoops.clear();
        }
    }


    /**
     * Removes face2. Faces must have exactly one common edge.
     * @param face1
     * @param face2
     */
    public void joinFace(Face face1, Face face2) {
        // TODO: Can have multiple common edges! -> This operator can't work safely in this case
        Edge commonEdge = face1.getAnyCommonEdge(face2);
        if(commonEdge == null)
            throw new IllegalArgumentException("Faces are not adjacent");

        joinFace(face1, face2, commonEdge);
    }

    public void joinFace(Face face1, Face face2, Edge commonEdge) {
        // Check if winding order for faces along commonEdge are different
        // -> Invert face2's winding order if they're the same?
        // Check if planar? -> No, up to the user
        // Remove loops along commonEdge
        // Connect loops of face1 to face2

        // TODO: Remove check, leave up to user
        /*if(face1.getCommonEdges(face2).size() != 1) {
            System.out.println("common edges: " + face1.getCommonEdges(face2).size());
            return;
        }*/

        assert face1.getCommonEdges(face2).size() == 1;
        assert face1 != face2;

        Loop l1 = null;
        Loop l2 = null;

        if(commonEdge.loop.nextEdgeLoop.nextEdgeLoop != commonEdge.loop)
            throw new IllegalArgumentException("Only the two given faces must be adjacent to the edge");

        // TODO: What if 'commonEdge' has >2 adjacent faces (T-structure)?
        if(commonEdge.loop.face == face1 && commonEdge.loop.nextEdgeLoop.face == face2) {
            l1 = commonEdge.loop;
            l2 = commonEdge.loop.nextEdgeLoop;
        }
        else if(commonEdge.loop.face == face2 && commonEdge.loop.nextEdgeLoop.face == face1) {
            l1 = commonEdge.loop.nextEdgeLoop;
            l2 = commonEdge.loop;
        }
        else
            throw new IllegalArgumentException("Faces are not adjacent to the given edge");

        // Check if loops point in the same direction (faces have opposing winding orders)
        if(l1.vertex == l2.vertex) {
            // TODO: throw? Overload with 'invert if needed' argument?
            System.out.println("invert");
            invertFace(face2);
        }

        for(Loop loop = l2.nextFaceLoop; loop != l2; loop = loop.nextFaceLoop) {
            assert loop.face == face2;
            loop.face = face1;
        }

        l1.nextFaceLoop.prevFaceLoop = l2.prevFaceLoop;
        l2.prevFaceLoop.nextFaceLoop = l1.nextFaceLoop;

        l1.prevFaceLoop.nextFaceLoop = l2.nextFaceLoop;
        l2.nextFaceLoop.prevFaceLoop = l1.prevFaceLoop;

        commonEdge.vertex0.removeEdge(commonEdge);
        commonEdge.vertex1.removeEdge(commonEdge);

        assert l1.nextFaceLoop != l2;
        face1.loop = l1.nextFaceLoop;

        loopData.destroy(l1);
        loopData.destroy(l2);
        edgeData.destroy(commonEdge);
        faceData.destroy(face2);
    }


    /**
     * Reverses the winding order of the given Face.
     * @param face
     */
    public void invertFace(Face face) {
        try {
            assert tempLoops.isEmpty();
            face.getLoops(tempLoops);

            Vertex firstVertex = tempLoops.get(0).vertex;
            Loop prev = tempLoops.get(tempLoops.size()-1);

            for(int i=0; i<tempLoops.size(); ++i) {
                int nextIndex = (i+1) % tempLoops.size();

                Loop current = tempLoops.get(i);
                current.vertex = current.nextFaceLoop.vertex;
                current.nextFaceLoop = prev;
                current.prevFaceLoop = tempLoops.get(nextIndex);
                prev = current;
            }

            prev.vertex = firstVertex;
            face.loop = prev; // Keep first vertex
        }
        finally {
            tempLoops.clear();
        }
    }
}
