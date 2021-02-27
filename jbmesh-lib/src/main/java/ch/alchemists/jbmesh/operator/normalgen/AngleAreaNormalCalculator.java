package ch.alchemists.jbmesh.operator.normalgen;

import com.jme3.math.Vector3f;
import ch.alchemists.jbmesh.data.property.FloatProperty;
import ch.alchemists.jbmesh.operator.FaceOps;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;

@Deprecated
public class AngleAreaNormalCalculator extends AngleNormalCalculator {
    private static final String PROPERTY_FACE_AREA = "AngleAreaNormalCalculator_FaceArea";

    private FloatProperty<Face> propFaceArea;


    /*public AngleAreaNormalCalculator() {
        super();
    }*/


    @Override
    public void prepare(BMesh bmesh, float creaseAngle) {
        propFaceArea = FloatProperty.getOrCreate(PROPERTY_FACE_AREA, bmesh.faces());
        FaceOps faceOps = new FaceOps(bmesh);

        for(Face face : bmesh.faces()) {
            faceOps.normal(face, tempV1);
            //propFaceNormal.set(face, tempV1);

            float area = faceOps.area(face, tempV1);
            propFaceArea.set(face, area);
        }
    }

    public void cleanup(BMesh bmesh) {
        bmesh.faces().removeProperty(propFaceArea);
        propFaceArea = null;
    }


    @Override
    public void getWeightedNormal(Loop loop, Vector3f store) {
        /*float weight = super.getWeightedNormal(loop, store);
        float area = propFaceArea.get(loop.face); // TODO: Use existing cross product of triangles in AngleNormalCalculator instead?
        return weight * area;*/

        store.zero();
    }
}
