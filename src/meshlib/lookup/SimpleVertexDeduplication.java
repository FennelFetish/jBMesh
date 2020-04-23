package meshlib.lookup;

import com.jme3.math.Vector3f;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;

public class SimpleVertexDeduplication implements VertexDeduplication {
    private final Vec3Property<Vertex> propPosition;
    private float epsilonSquared;


    public SimpleVertexDeduplication(BMesh bmesh) {
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertexData());
        setRange(0.01f);
    }


    public void setRange(float epsilon) {
        epsilonSquared = epsilon * epsilon;
    }

    
    @Override
    public Vertex getOrCreateVertex(BMesh mesh, Vector3f location) {
        for(Vertex vert : mesh.vertices()) {
            if(propPosition.get(vert).distanceSquared(location) <= epsilonSquared)
                return vert;
        }

        Vertex vert = mesh.createVertex(location);
        return vert;
    }
}
