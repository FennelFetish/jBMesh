package meshlib.operator;

import com.jme3.math.Vector3f;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Loop;
import meshlib.structure.Vertex;

// Naming... doesn't really match what this is doing: https://docs.blender.org/manual/en/latest/modeling/meshes/editing/duplicating/inset.html
public class Inset {
    private final FaceOps faceOps;
    private final ExtrudeFace extrusion;
    private final Vec3Property<Vertex> propPosition;

    private float thickness = 0.6f; // relative factor, TODO: absolute?
    private float depth = 1.0f;


    public Inset(BMesh bmesh, float thickness, float depth) {
        faceOps = new FaceOps(bmesh);
        extrusion = new ExtrudeFace(bmesh);
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
        
        this.thickness = thickness;
        this.depth = depth;
    }
    

    public void apply(Face face) {
        extrusion.apply(face);
        extrusion.copyVertexProperties();

        Vector3f p = new Vector3f();
        Vector3f centroid = faceOps.calcCentroid(face);
        Vector3f normal = faceOps.calcNormal(face).multLocal(-depth);

        for(Loop loop : face.loops()) {
            propPosition.get(loop.vertex, p);
            p.subtractLocal(centroid);
            p.multLocal(thickness);
            p.addLocal(centroid);
            propPosition.set(loop.vertex, p);
        }

        extrusion.apply(face);
        extrusion.copyVertexProperties();

        for(Loop loop : face.loops()) {
            propPosition.get(loop.vertex, p);
            p.addLocal(normal);
            propPosition.set(loop.vertex, p);
        }
    }
}
