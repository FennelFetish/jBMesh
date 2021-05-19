package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;

public class ScaleFace {
    private final FaceOps faceOps;
    private final Vec3Property<Vertex> propPosition;

    private float scale = 1.0f;

    // TODO: Define pivot point (alternatives to centroid)


    public ScaleFace(BMesh bmesh, float scale) {
        faceOps = new FaceOps(bmesh);
        propPosition = Vec3Property.get(Vertex.Position, bmesh.vertices());
        this.scale = scale;
    }


    public void apply(Face face) {
        Vector3f centroid = faceOps.centroid(face);
        Vector3f p = new Vector3f();

        for(Vertex vertex : face.vertices()) {
            propPosition.get(vertex, p);
            p.subtractLocal(centroid);
            p.multLocal(scale);
            p.addLocal(centroid);
            propPosition.set(vertex, p);
        }
    }
}
