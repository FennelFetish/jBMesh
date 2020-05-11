package meshlib;

import com.jme3.math.Vector3f;
import com.jme3.scene.shape.Sphere;
import java.util.List;
import meshlib.conversion.Import;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;

public class TestMesh {
    public static BMesh testSphere() {
        Sphere sphere = new Sphere(16, 16, 2.0f);
        BMesh bmesh = Import.convertExactMapped(sphere);

        Vec3Property<Vertex> propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
        List<Vertex> vertices = bmesh.vertices().getAll();
        for(Vertex v : vertices) {
            Vector3f pos = propPosition.get(v);
            if(pos.z < 0 || pos.z > 0.7f || pos.x < 0.9f || pos.y < -0.2f)
                bmesh.removeVertex(v);
            else
                propPosition.execute(v, (p) -> {
                    //p.x = 1.0f;
                });
        }

        return bmesh;
    }
}
