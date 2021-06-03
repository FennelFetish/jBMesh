// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.operator.normalgen;

import ch.alchemists.jbmesh.data.property.FloatAttribute;
import ch.alchemists.jbmesh.operator.FaceOps;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import com.jme3.math.Vector3f;

@Deprecated
public class AngleAreaNormalCalculator extends AngleNormalCalculator {
    private static final String ATTRIBUTE_FACE_AREA = "AngleAreaNormalCalculator_FaceArea";

    private FloatAttribute<Face> attrFaceArea;


    /*public AngleAreaNormalCalculator() {
        super();
    }*/


    @Override
    public void prepare(BMesh bmesh, float creaseAngle) {
        attrFaceArea = FloatAttribute.getOrCreate(ATTRIBUTE_FACE_AREA, bmesh.faces());
        FaceOps faceOps = new FaceOps(bmesh);

        for(Face face : bmesh.faces()) {
            faceOps.normal(face, tempV1);
            //attrFaceNormal.set(face, tempV1);

            float area = faceOps.area(face, tempV1);
            attrFaceArea.set(face, area);
        }
    }

    @Override
    public void cleanup(BMesh bmesh) {
        bmesh.faces().removeAttribute(attrFaceArea);
        attrFaceArea = null;
    }


    @Override
    public void getWeightedNormal(Loop loop, Vector3f store) {
        /*float weight = super.getWeightedNormal(loop, store);
        float area = attrFaceArea.get(loop.face); // TODO: Use existing cross product of triangles in AngleNormalCalculator instead?
        return weight * area;*/

        store.zero();
    }
}
