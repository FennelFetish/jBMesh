// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.TestUtil;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class FaceOpsTest {
    @Test
    public void testNormal() {
        BMesh bmesh = new BMesh();
        FaceOps faceOps = new FaceOps(bmesh);

        /**
         * Concave:
         *       v3
         *
         *       v1
         * v0          v2
         */
        Vertex v0 = bmesh.createVertex(0.0f, 0.0f, 0.0f);
        Vertex v1 = bmesh.createVertex(0.5f, 0.3f, 0.0f);
        Vertex v2 = bmesh.createVertex(1.0f, 0.0f, 0.0f);
        Vertex v3 = bmesh.createVertex(0.5f, 1.0f, 0.0f);

        Face faceConcave = bmesh.createFace(v1, v2, v3, v0);
        Vector3f normal = faceOps.normal(faceConcave);
        assertEquals(0.0f, normal.x);
        assertEquals(0.0f, normal.y);
        assertEquals(1.0f, normal.z);

        Face faceConvex = bmesh.createFace(v0, v2, v3);
        normal = faceOps.normalConvex(faceConvex);
        assertEquals(0.0f, normal.x);
        assertEquals(0.0f, normal.y);
        assertEquals(1.0f, normal.z);
    }


    @Test
    public void testArea() {
        BMesh bmesh = new BMesh();
        FaceOps faceOps = new FaceOps(bmesh);

        testArea(bmesh, faceOps, 0.5f,
            0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f);

        testArea(bmesh, faceOps, 1.0f,
            0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f);

        testArea(bmesh, faceOps, 0.5f,
            0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            0.5f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f);

        // TODO: more...
    }


    private void testArea(BMesh bmesh, FaceOps faceOps, float expectedArea, float... vertices) {
        Vertex[] vs = new Vertex[vertices.length / 3];
        assert vs.length * 3 == vertices.length;

        int v=0;
        for(int i=2; i<vertices.length; i += 3)
            vs[v++] = bmesh.createVertex(vertices[i-2], vertices[i-1], vertices[i]);

        Face face = bmesh.createFace(vs);
        TestUtil.assertFloat(expectedArea, faceOps.area(face));

        if(vs.length == 3)
            TestUtil.assertFloat(expectedArea, faceOps.areaTriangle(face));
    }
}
