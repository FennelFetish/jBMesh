// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.TestUtil;
import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ScaleFaceTest {
    private static final float SCALE = 2.0f;

    private BMesh bmesh;
    private Face face;
    private Vec3Attribute<Vertex> positions;


    @BeforeEach
    private void initFace() {
        bmesh = new BMesh();
        positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());

        Vertex[] vertices = new Vertex[] {
            bmesh.createVertex(1, 0, 5),
            bmesh.createVertex(1, 1, 5),
            bmesh.createVertex(0, 1, 5),
            bmesh.createVertex(0, 0, 5)
        };

        face = bmesh.createFace(vertices);
    }


    @Test
    public void testDefaultPivot() {
        testCentroidPivot();
    }


    @Test
    public void testCentroidPivot() {
        ScaleFace scaleFace = new ScaleFace(bmesh, SCALE);
        scaleFace.setPivotFunction(new ScaleFace.CentroidPivot(bmesh));
        scaleFace.apply(face);

        List<Vertex> vertices = face.getVertices();
        TestUtil.assertVec3Similar(1.5f, -0.5f, 5, positions.get(vertices.get(0)));
        TestUtil.assertVec3Similar(1.5f, 1.5f, 5, positions.get(vertices.get(1)));
        TestUtil.assertVec3Similar(-0.5f, 1.5f, 5, positions.get(vertices.get(2)));
        TestUtil.assertVec3Similar(-0.5f, -0.5f, 5, positions.get(vertices.get(3)));
    }


    @Test
    public void testFirstVertexPivot() {
        ScaleFace scaleFace = new ScaleFace(bmesh, SCALE);
        scaleFace.setPivotFunction(new ScaleFace.FirstVertexPivot(bmesh));
        scaleFace.apply(face);

        List<Vertex> vertices = face.getVertices();
        TestUtil.assertVec3Similar(1, 0, 5, positions.get(vertices.get(0)));
        TestUtil.assertVec3Similar(1, 2, 5, positions.get(vertices.get(1)));
        TestUtil.assertVec3Similar(-1, 2, 5, positions.get(vertices.get(2)));
        TestUtil.assertVec3Similar(-1, 0, 5, positions.get(vertices.get(3)));
    }


    @Test
    public void testPointPivot() {
        ScaleFace scaleFace = new ScaleFace(bmesh, SCALE);
        scaleFace.setPivotFunction(new ScaleFace.PointPivot(-1, -1, 0));
        scaleFace.apply(face);

        List<Vertex> vertices = face.getVertices();
        TestUtil.assertVec3Similar(3, 1, 10, positions.get(vertices.get(0)));
        TestUtil.assertVec3Similar(3, 3, 10, positions.get(vertices.get(1)));
        TestUtil.assertVec3Similar(1, 3, 10, positions.get(vertices.get(2)));
        TestUtil.assertVec3Similar(1, 1, 10, positions.get(vertices.get(3)));
    }


    @Test
    public void testCustomPivotAndScale() {
        Vector3f pivot = new Vector3f(10, 20, 30);

        ScaleFace scaleFace = new ScaleFace(bmesh, SCALE);
        scaleFace.setPivotFunction(f -> pivot);
        scaleFace.setScale(0);
        scaleFace.apply(face);

        assertEquals(0, scaleFace.getScale());

        for(Vertex v : face.getVertices())
            TestUtil.assertVec3Similar(pivot, positions.get(v));
    }


    @Test
    public void testNullPivotFunction() {
        assertThrows(NullPointerException.class, () -> {
            new ScaleFace(bmesh, SCALE, null);
        });

        assertThrows(NullPointerException.class, () -> {
            ScaleFace scaleFace = new ScaleFace(bmesh, SCALE);
            scaleFace.setPivotFunction(null);
        });
    }
}
