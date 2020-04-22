package meshlib.structure;

import com.jme3.math.Vector3f;
import java.util.List;

public class BMesh  {
    private final BMeshData<Vertex> vertexData;
    private final BMeshData<Edge> edgeData;
    private final BMeshData<Face> faceData;
    private final BMeshData<Loop> loopData;

    private final BMeshData<Vertex>.Property propPosition;


    public BMesh() {
        vertexData = new BMeshData<>("Vertex", Vertex.ACCESSOR);
        edgeData   = new BMeshData<>("Edge", Edge.ACCESSOR);
        faceData   = new BMeshData<>("Face", Face.ACCESSOR);
        loopData   = new BMeshData<>("Loop", Loop.ACCESSOR);

        propPosition = vertexData.createProperty(BMeshProperties.Vertex.POSITION, BMeshData.PropertyType.Float, 3);
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


    // TODO: Make private so deduplication can work? Or leave deduplication up to the user?
    public Vertex createVertex() {
        return vertexData.add();
    }

    public Vertex createVertex(float x, float y, float z) {
        Vertex vert = createVertex();
        propPosition.setVec3(vert, x, y, z);
        //vert.setLocation(x, y, z); // TODO: Deduplication?
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

        return edge;
    }


    public Face createFace(Vertex... faceVertices) {
        if(faceVertices.length < 3)
            throw new IllegalArgumentException("A face needs at least 3 vertices");

        Face face = faceData.add();

        Loop[] loops = new Loop[faceVertices.length];
        for(int i=0; i<faceVertices.length; ++i) {
            loops[i] = loopData.add();
            loops[i].face = face;
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
        loopData.compact();
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
