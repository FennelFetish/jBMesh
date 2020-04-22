package meshlib.lookup;

import com.jme3.math.Vector3f;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;

public class SimpleVertexDeduplication implements VertexDeduplication {
    private float epsilonSquared;


    public SimpleVertexDeduplication() {
        setRange(0.01f);
    }


    public void setRange(float epsilon) {
        epsilonSquared = epsilon * epsilon;
    }

    
    @Override
    public Vertex getOrCreateVertex(BMesh mesh, Vector3f location) {
        Vector3f loc = new Vector3f();
        for(Vertex vert : mesh.vertices()) {
            vert.getLocation(loc);
            if(location.distanceSquared(loc) <= epsilonSquared) {
                return vert;
            }
        }

        Vertex vert = mesh.createVertex(location);
        return vert;
    }
}
