package meshlib.lookup;

import com.jme3.math.Vector3f;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;

public class SimpleVertexDeduplication implements VertexDeduplication {
    private final Vec3Property<Vertex> propPosition;
    private float epsilonSquared;

    private final transient Vector3f tempPosition = new Vector3f();


    public SimpleVertexDeduplication(BMesh bmesh, float range) {
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
        setRange(range);
    }

    public SimpleVertexDeduplication(BMesh bmesh) {
        this(bmesh, 0.01f);
    }


    public void setRange(float epsilon) {
        epsilonSquared = epsilon * epsilon;
    }

    
    @Override
    public Vertex getOrCreateVertex(BMesh bmesh, Vector3f location) {
        for(Vertex vert : bmesh.vertices()) {
            propPosition.get(vert, tempPosition);
            if(tempPosition.distanceSquared(location) <= epsilonSquared)
                return vert;
        }

        return bmesh.createVertex(location);
    }
}
