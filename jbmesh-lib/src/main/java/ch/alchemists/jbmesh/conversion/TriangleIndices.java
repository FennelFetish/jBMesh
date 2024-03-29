// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.data.property.ObjectAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.operator.sweeptriang.SweepTriangulation;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import java.util.ArrayList;
import java.util.logging.Logger;

public class TriangleIndices {
    private static class Triangle extends Element {
        public Loop l1, l2, l3;

        @Override
        protected void releaseElement() {}
    }


    private static final Logger LOG = Logger.getLogger(TriangleIndices.class.getName());

    private final BMesh bmesh;
    private final SweepTriangulation triangulation;
    private final ObjectAttribute<Loop, Vertex> attrLoopVertex;

    private final BMeshData<Triangle> triangleData;
    private final Indices<Triangle> indices;


    public TriangleIndices(BMesh bmesh, ObjectAttribute<Loop, Vertex> attrLoopVertex) {
        this.bmesh = bmesh;
        triangulation = new SweepTriangulation();
        this.attrLoopVertex = attrLoopVertex;

        triangleData = new BMeshData<>(Triangle::new);
        indices = new Indices<>(triangleData, 3);
    }


    /**
     * Updates face triangulation. This needs to be called when the face topology changes.
     * TODO: Call only for dirty faces?
     */
    public void triangulateFaces() {
        Vec3Attribute<Vertex> attrPosition = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());

        triangleData.clear();
        triangleData.ensureCapacity(bmesh.faces().size());

        ArrayList<Loop> loops = new ArrayList<>(6);
        triangulation.setTriangleCallback((v1, v2, v3) -> {
            addTriangle(loops, v1.index, v2.index, v3.index);
        });

        for(Face face : bmesh.faces()) {
            loops.clear();
            face.getLoops(loops);
            final int numVertices = loops.size();

            if(numVertices == 3)
                addTriangle(loops,0, 1, 2);
            else if(numVertices == 4)
                triangulateQuad(attrPosition, loops);
            else if(numVertices > 4)
                triangulatePolygon(attrPosition, loops);
            else
                LOG.warning("Couldn't triangulate face with " + numVertices + " vertices.");

            // TODO: Ear clipping for faces with 5-10 vertices?
        }
    }


    private void addTriangle(ArrayList<Loop> loops, int i1, int i2, int i3) {
        Triangle tri = triangleData.create();
        tri.l1 = loops.get(i1);
        tri.l2 = loops.get(i2);
        tri.l3 = loops.get(i3);
    }


    /**
     * Triangulates a quadrilateral with a split along the shorter diagonal.
     * If a vertex is reflex and the quad forms an arrowhead, this reflex vertex will be part of the chosen diagonal.
     */
    private void triangulateQuad(Vec3Attribute<Vertex> attrPosition, ArrayList<Loop> loops) {
        Vector3f p0 = attrPosition.get(loops.get(0).vertex);
        Vector3f p1 = attrPosition.get(loops.get(1).vertex);
        Vector3f p2 = attrPosition.get(loops.get(2).vertex);
        Vector3f p3 = attrPosition.get(loops.get(3).vertex);

        // Test 1 & 3 against diagonal 0->2
        Vector3f diagonal = p2.subtract(p0);

        Vector3f v = p1.subtract(p0);
        Vector3f cross = diagonal.cross(v); // cross = 0->2 x 0->1

        v.set(p3).subtractLocal(p0);
        v.crossLocal(diagonal); // v = 0->3 x 0->2

        // If 1 & 3 are on different sides of 0->2, diagonal is valid
        float length0_2 = Float.POSITIVE_INFINITY;
        if(cross.dot(v) > 0)
            length0_2 = diagonal.lengthSquared();

        // Test 0 & 2 against diagonal 1->3
        diagonal.set(p3).subtractLocal(p1);

        v.set(p0).subtractLocal(p1);
        cross.set(diagonal).crossLocal(v); // cross = 1->3 x 1->0

        v.set(p2).subtractLocal(p1);
        v.crossLocal(diagonal); // v = 1->2 x 1->3

        // If 0 & 2 are on different sides of 1->3, diagonal is valid
        float length1_3 = Float.POSITIVE_INFINITY;
        if(cross.dot(v) > 0)
            length1_3 = diagonal.lengthSquared();

        // Choose shorter diagonal
        // TODO: Use homogenous direction if lengths are almost equal
        if(length0_2 <= length1_3) {
            addTriangle(loops, 0, 1, 2);
            addTriangle(loops, 0, 2, 3);
        }
        else {
            addTriangle(loops, 0, 1, 3);
            addTriangle(loops, 1, 2, 3);
        }
    }


    private void triangulatePolygon(Vec3Attribute<Vertex> attrPosition, ArrayList<Loop> loops) {
        try {
            triangulation.addFaceWithLoops(attrPosition, loops);
            triangulation.triangulate();
        }
        catch(Throwable t) {
            LOG.warning("Couldn't triangulate face with " + loops.size() + " vertices.");
        }
    }

    
    /**
     * Updates index buffer with existing triangulation and Loop->Vertex mapping.
     * This needs to be called when Loop->Vertex mapping (duplication) is changed, e.g. after NormalGenerator.
     * TODO: Maintain Triangle dirty state so only indices of changed faces are updated?
     */
    public void applyIndexBuffer(Mesh mesh) {
        int maxVertexIndex = bmesh.vertices().totalSize()-1;
        indices.prepare(maxVertexIndex);

        indices.updateIndices((Triangle tri, int[] indices) -> {
            indices[0] = attrLoopVertex.get(tri.l1).getIndex();
            indices[1] = attrLoopVertex.get(tri.l2).getIndex();
            indices[2] = attrLoopVertex.get(tri.l3).getIndex();
        });

        indices.applyIndexBuffer(mesh);
    }
}
