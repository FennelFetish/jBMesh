package meshlib.structure;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
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
        return vertexData.add();
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

        Edge edge = edgeData.add();
        edge.vertex0 = v0;
        edge.vertex1 = v1;
        v0.addEdge(edge);
        v1.addEdge(edge);

        return edge;
    }


    public Face createFace(Vertex... faceVertices) {
        if(faceVertices.length < 3)
            throw new IllegalArgumentException("A face needs at least 3 vertices");

        tempLoops.ensureCapacity(faceVertices.length);
        for(int i=0; i<faceVertices.length; ++i) {
            tempLoops.add(loopData.add());
        }

        Face face = faceData.add();
        face.loop = tempLoops.get(0);

        for(int i=0; i<faceVertices.length; ++i) {
            int nextIndex = (i+1) % faceVertices.length;
            
            Edge edge = faceVertices[i].getEdgeTo(faceVertices[nextIndex]);
            if(edge == null)
                edge = createEdge(faceVertices[i], faceVertices[nextIndex]);

            Loop loop = tempLoops.get(i);
            edge.addLoop(loop);

            loop.face = face;
            loop.edge = edge;
            loop.vertex = faceVertices[i];
            loop.nextFaceLoop = tempLoops.get(nextIndex);
        }

        tempLoops.clear();
        return face;
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
}
