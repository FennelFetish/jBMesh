package meshlib.lookup;

import com.jme3.math.Vector3f;
import meshlib.structure.BMesh;
import meshlib.structure.BMeshData;
import meshlib.structure.BMeshProperty;
import meshlib.structure.Vertex;

public class SimpleVertexDeduplication implements VertexDeduplication {
    private final BMeshData<Vertex>.Property propPosition;
    private float epsilonSquared;


    public SimpleVertexDeduplication(BMesh bmesh) {
        propPosition = bmesh.vertexData().getProperty(BMeshProperty.Vertex.POSITION);
        setRange(0.01f);
    }


    public void setRange(float epsilon) {
        epsilonSquared = epsilon * epsilon;
    }

    
    @Override
    public Vertex getOrCreateVertex(BMesh mesh, Vector3f location) {
        Vector3f loc = new Vector3f();
        for(Vertex vert : mesh.vertices()) {
            propPosition.getVec3(vert, loc);
            if(location.distanceSquared(loc) <= epsilonSquared) {
                return vert;
            }
        }

        Vertex vert = mesh.createVertex(location);
        return vert;
    }
}
