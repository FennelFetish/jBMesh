package meshlib.operator.normalgen;

import com.jme3.math.Vector3f;
import meshlib.data.BMeshProperty;
import meshlib.data.property.FloatProperty;
import meshlib.data.property.Vec3Property;
import meshlib.operator.FaceOps;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Loop;
import meshlib.structure.Vertex;

public class AngleNormalCalculator implements NormalGenerator.NormalCalculator {
    protected final BMesh bmesh;
    protected final FaceOps faceOps;

    protected final Vec3Property<Vertex> propPosition;
    protected final Vec3Property<Face> propFaceNormal;

    protected final transient Vector3f tempV1 = new Vector3f();
    protected final transient Vector3f tempV2 = new Vector3f();

    public AngleNormalCalculator(BMesh bmesh, FaceOps faceOps) {
        this.bmesh = bmesh;
        this.faceOps = faceOps;

        propPosition   = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
        propFaceNormal = Vec3Property.getOrCreate(BMeshProperty.Face.NORMAL, bmesh.faces());
        // Remove properties after apply?
    }

    @Override
    public void prepare() {
        for(Face face : bmesh.faces()) {
            faceOps.normal(face, tempV1);
            propFaceNormal.set(face, tempV1);
        }
    }

    @Override
    public Vector3f getNormal(Face face) {
        propFaceNormal.get(face, tempV1);
        return tempV1;
    }

    @Override
    public float calcNormalWeight(Loop loop) {
        // Angle of side
        Vertex vertex = loop.vertex;
        Vertex vNext = loop.nextFaceLoop.vertex;
        Vertex vPrev = loop.prevFaceLoop.vertex;

        propPosition.get(vertex, tempV1);
        tempV2.set(tempV1);
        propPosition.subtract(vNext, tempV1);
        propPosition.subtract(vPrev, tempV2);
        tempV1.normalizeLocal();
        tempV2.normalizeLocal();

        return tempV1.angleBetween(tempV2);
    }

    @Override
    public boolean isCrease(Face face1, Face face2, float creaseAngle) {
        propFaceNormal.get(face1, tempV1);
        propFaceNormal.get(face2, tempV2);
        return tempV1.angleBetween(tempV2) > creaseAngle;
    }
}
