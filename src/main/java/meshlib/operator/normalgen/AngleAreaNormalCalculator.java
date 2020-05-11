package meshlib.operator.normalgen;

import meshlib.data.property.FloatProperty;
import meshlib.operator.FaceOps;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Loop;

public class AngleAreaNormalCalculator extends AngleNormalCalculator {
    private static final String PROPERTY_FACE_AREA = "AngleAreaNormalCalculator_FaceArea";

    private final FloatProperty<Face> propFaceArea;

    public AngleAreaNormalCalculator(BMesh bmesh, FaceOps faceOps) {
        super(bmesh, faceOps);
        propFaceArea = FloatProperty.getOrCreate(PROPERTY_FACE_AREA, bmesh.faces());
    }


    @Override
    public void prepare() {
        for(Face face : bmesh.faces()) {
            faceOps.normal(face, tempV1);
            propFaceNormal.set(face, tempV1);

            float area = faceOps.area(face, tempV1);
            propFaceArea.set(face, area);
        }
    }


    @Override
    public float calcNormalWeight(Loop loop) {
        //float triangleArea = tempV1.cross(tempV2).length() * 0.5f;

        float weight = super.calcNormalWeight(loop);
        float area = propFaceArea.get(loop.face);
        return weight * area;
    }
}
