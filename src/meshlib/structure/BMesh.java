package meshlib.structure;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Objects;
import meshlib.data.BMeshData;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;

public class BMesh {
    private final BMeshData<Vertex> vertexData;
    private final BMeshData<Edge> edgeData;
    private final BMeshData<Face> faceData;
    private final BMeshData<Loop> loopData;

    private final Vec3Property<Vertex> propPosition = new Vec3Property<>(BMeshProperty.Vertex.POSITION);

    private final transient ArrayList<Loop> tempLoops = new ArrayList<>(4);


    public BMesh() {
        vertexData = new BMeshData<>(() -> new Vertex());
        edgeData   = new BMeshData<>(() -> new Edge());
        faceData   = new BMeshData<>(() -> new Face());
        loopData   = new BMeshData<>(() -> new Loop());

        vertexData.addProperty(propPosition);
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


    // Euler operations:
    // Split Edge, create Vertex    Vertex splitEdge(Edge)
    // Join Edge, remove Vertex     Edge(void?) joinEdge(Edge 1, Edge 2)
    // Split Face, create Edge      Edge splitFace(Vertex 1, Vertex 2, Face?)
    // Join Face, remove Edge       Face(void?) joinFace(Face 1, Face 2)
    // Invert face                  void invert(Face)

    // extrudeVertex        -> new edge
    // extrudeEdgeQuad      -> new face
    // extrudeEdgeTriangle  -> new triangle-face from edge with 1 additional vertex
    // extrudeFace          -> new volume


    public Vertex createVertex() {
        return vertexData.create();
    }

    public Vertex createVertex(float x, float y, float z) {
        Vertex vert = createVertex();
        propPosition.set(vert, x, y, z);
        return vert;
    }

    public Vertex createVertex(Vector3f location) {
        return createVertex(location.x, location.y, location.z);
    }


    public Edge createEdge(Vertex v0, Vertex v1) {
        assert v0 != v1;

        Edge edge = edgeData.create();
        edge.vertex0 = v0;
        edge.vertex1 = v1;
        v0.addEdge(edge);
        v1.addEdge(edge);

        return edge;
    }


    public void removeEdge(Edge edge) {
        // Remove adjacent faces?
        // Remove adjacent vertices if they're not connected to anything else?

        edgeData.destroy(edge);
    }


    public Face createFace(Vertex... faceVertices) {
        if(faceVertices.length < 3)
            throw new IllegalArgumentException("A face needs at least 3 vertices");

        try {
            assert tempLoops.isEmpty();
            for(int i=0; i<faceVertices.length; ++i) {
                Objects.requireNonNull(faceVertices[i]);
                tempLoops.add(loopData.create());
            }

            Face face = faceData.create();
            face.loop = tempLoops.get(0);

            Loop prevLoop = tempLoops.get(tempLoops.size()-1);
            for(int i=0; i<faceVertices.length; ++i) {
                int nextIndex = (i+1) % faceVertices.length;

                Edge edge = faceVertices[i].getEdgeTo(faceVertices[nextIndex]);
                if(edge == null)
                    edge = createEdge(faceVertices[i], faceVertices[nextIndex]);

                Loop loop = tempLoops.get(i);
                loop.face = face;
                loop.edge = edge;
                loop.vertex = faceVertices[i];
                loop.nextFaceLoop = tempLoops.get(nextIndex);
                loop.prevFaceLoop = prevLoop;
                edge.addLoop(loop);

                prevLoop = loop;
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


    public void removeFace(Face face) {
        // Disconnect loops from Edges
        // Leave Edges
        // Release elements (Loops)

        
    }


    /**
     * Splits the Edge into two:
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
     * @return A new Vertex (<i>vNew</i>) with default properties (no specific position).
     */
    public Vertex splitEdge(Edge edge) {
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
     * Removes edge and vertex. Vertex must be adjacent to <i>edge</i> one other Edge.
     * @param edge Will be removed.
     * @param vertex Will be removed.
     */
    public boolean joinEdge(Edge edge, Vertex vertex) {
        // Do this first so it will throw if edge is null or not adjacent
        Edge keepEdge = edge.getNextEdge(vertex);
        if(keepEdge.getNextEdge(vertex) != edge)
            return false;

        return true;
    }


    
    public Edge splitFace(Vertex vertex1, Vertex vertex2) {
        return null;
    }


    /**
     * Removes face2.
     * @param face1
     * @param face2
     */
    public void joinFace(Face face1, Face face2) {
        // TODO: Can have multiple common edges!
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


    }


    public void invertFace(Face face) {
        try {
            assert tempLoops.isEmpty();
            for(Loop loop : face.loops())
                tempLoops.add(loop);

            Loop prev = tempLoops.get(tempLoops.size()-1);
            for(int i=0; i<tempLoops.size(); ++i) {
                int nextIndex = (i+1) % tempLoops.size();

                Loop current = tempLoops.get(i);
                current.nextFaceLoop = prev;
                current.prevFaceLoop = tempLoops.get(nextIndex);
                prev = current;
            }
        }
        finally {
            tempLoops.clear();
        }
    }
}
