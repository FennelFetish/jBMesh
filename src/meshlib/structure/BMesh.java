package meshlib.structure;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
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


    public List<Vertex> vertices() {
        return vertexData.elements();
    }

    public List<Edge> edges() {
        return edgeData.elements();
    }

    public List<Face> faces() {
        return faceData.elements();
    }


    public BMeshData<Vertex> vertexData() {
        return vertexData;
    }

    public BMeshData<Edge> edgeData() {
        return edgeData;
    }

    public BMeshData<Face> faceData() {
        return faceData;
    }

    public BMeshData<Loop> loopData() {
        return loopData;
    }


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


    public Face createFace(Vertex... faceVertices) {
        if(faceVertices.length < 3)
            throw new IllegalArgumentException("A face needs at least 3 vertices");

        try {
            tempLoops.ensureCapacity(faceVertices.length);
            for(int i=0; i<faceVertices.length; ++i) {
                Objects.requireNonNull(faceVertices[i]);
                tempLoops.add(loopData.create());
            }

            Face face = faceData.create();
            face.loop = tempLoops.get(0);

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
                edge.addLoop(loop);
            }
            
            return face;
        }
        finally {
            tempLoops.clear();
        }
    }


    public void compactData() {
        vertexData.compact();
        edgeData.compact();
        faceData.compact();
        loopData.compact();

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
        Vertex vertex = vertexData.create();

        Edge newEdge = edgeData.create();
        newEdge.vertex0 = vertex;
        newEdge.vertex1 = edge.vertex1;

        edge.vertex1.removeEdge(edge);
        edge.vertex1.addEdge(newEdge);
        edge.vertex1 = vertex;

        vertex.addEdge(edge);
        vertex.addEdge(newEdge);

        for(Loop loop : edge.loops()) {
            Loop newLoop = loopData.create();
            newLoop.edge = newEdge;
            newLoop.face = loop.face;
            newEdge.addLoop(newLoop);

            // Link newLoop to next or previous loop, matching winding order.
            if(loop.vertex == edge.vertex0) {
                newLoop.vertex = vertex;
                newLoop.nextFaceLoop = loop.nextFaceLoop;
                loop.nextFaceLoop = newLoop;
            }
            else {
                assert loop.vertex == newEdge.vertex1;

                Loop prevLoop = loop.getPrevFaceLoop();
                prevLoop.nextFaceLoop = newLoop;
                newLoop.nextFaceLoop = loop;
                newLoop.vertex = loop.vertex;
                loop.vertex = vertex;
            }
        }

        return vertex;
    }
}
