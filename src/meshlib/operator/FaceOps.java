package meshlib.operator;

import com.jme3.math.Vector3f;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Vertex;

public class FaceOps {
    private final BMesh bmesh;
    private final Vec3Property<Vertex> propPosition;


    public FaceOps(BMesh bmesh) {
        this.bmesh = bmesh;
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertexData());
    }


    public int calcVertexCount(Face face) {
        int count = 0;
        for(Vertex v : face.vertices())
            count++;
        return count;
    }


    public Vector3f calcNormal(Face face) {
        Vector3f v0 = propPosition.get(face.loop.vertex);
        Vector3f v1 = propPosition.get(face.loop.nextFaceLoop.vertex);
        Vector3f v2 = propPosition.get(face.loop.nextFaceLoop.nextFaceLoop.vertex);

        v0.subtractLocal(v1);
        v2.subtractLocal(v1);
        return v2.crossLocal(v0).normalizeLocal();
    }


    public Vector3f calcCentroid(Face face) {
        int numVertices = 0;
        Vector3f centroid = new Vector3f();
        Vector3f p = new Vector3f();

        for(Vertex v : face.vertices()) {
            propPosition.get(v, p);
            centroid.addLocal(p);
            numVertices++;
        }

        return centroid.divideLocal(numVertices);
    }
}
