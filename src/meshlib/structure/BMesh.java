package meshlib.structure;

import com.jme3.math.Vector3f;
import java.util.List;

public class BMesh  {
    private final BMeshData<Vertex> vertexData;
    private final BMeshData<Edge> edgeData;
    private final BMeshData<Face> faceData;

    /*private final List<Vertex> vertices = new ArrayList<>();
    private final List<Edge> edges      = new ArrayList<>();
    private final List<Face> faces      = new ArrayList<>();

    private final List<Vertex> readonlyVertices = Collections.unmodifiableList(vertices);
    private final List<Edge> readonlyEdges      = Collections.unmodifiableList(edges);
    private final List<Face> readonlyFaces      = Collections.unmodifiableList(faces);*/


    public BMesh() {
        vertexData = new BMeshData<>(Vertex.ACCESSOR);
        edgeData   = new BMeshData<>(Edge.ACCESSOR);
        faceData   = new BMeshData<>(Face.ACCESSOR);
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


    // TODO: Make private so deduplication can work? Or leave deduplication up to the user?
    public Vertex createVertex() {
        return vertexData.add();

        /*int index = data.add();
        Vertex vert = new Vertex(index);
        vertices.add(vert);
        return vert;*/
    }

    public Vertex createVertex(float x, float y, float z) {
        Vertex vert = createVertex();
        vert.setLocation(x, y, z); // TODO: Deduplication?
        return vert;
    }

    public Vertex createVertex(Vector3f location) {
        return createVertex(location.x, location.y, location.z);
    }


    public Edge getOrCreateEdge(Vertex v0, Vertex v1) {
        assert v0 != v1;

        Edge edge = v0.getEdgeTo(v1);
        if(edge == null) {
            edge = edgeData.add();
            edge.vertex0 = v0;
            edge.vertex1 = v1;
            v0.addEdge(edge);
            v1.addEdge(edge);
        }

        /*if(edge == null) {
            edge = new Edge(v0, v1);
            v0.addEdge(edge);
            v1.addEdge(edge);
            edges.add(edge);
        }*/

        return edge;
    }


    public Face createFace(Vertex... faceVertices) {
        if(faceVertices.length < 3)
            throw new IllegalArgumentException("A face needs at least 3 vertices");

        Face face = faceData.add();
        //Face face = new Face();

        Loop[] loops = new Loop[faceVertices.length];
        for(int i=0; i<faceVertices.length; ++i) {
            loops[i] = new Loop(face);
        }
        
        face.loop = loops[0];

        for(int i=0; i<faceVertices.length; ++i) {
            int nextIndex = (i+1) % faceVertices.length;
            Edge edge = getOrCreateEdge(faceVertices[i], faceVertices[nextIndex]);
            edge.addLoop(loops[i]);

            loops[i].edge = edge;
            loops[i].vertex = faceVertices[i];
            loops[i].nextFaceLoop = loops[nextIndex];
        }

        return face;
    }


    public void compactData() {
        vertexData.compact();
        edgeData.compact();
        faceData.compact();
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
}
