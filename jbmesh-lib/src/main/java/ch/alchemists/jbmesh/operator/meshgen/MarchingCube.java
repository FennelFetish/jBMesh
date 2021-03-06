// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.operator.meshgen;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.lookup.VertexDeduplication;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;

public class MarchingCube {
    private final float cellSize;
    private final BMesh bmesh;
    private final VertexDeduplication dedup;
    private final Vec3Attribute<Loop> loopNormals;

    // Per vertex
    private final Vector3f[] corners = new Vector3f[8];
    private final float dist[] = new float[8];

    // Per edge
    private final Vector3f[] intersectionPoints = new Vector3f[12];
    private final Vector3f[] normals = new Vector3f[12];


    public MarchingCube(BMesh bmesh, VertexDeduplication dedup, float cellSize, boolean setNormals) {
        this.cellSize = cellSize;
        this.bmesh = bmesh;
        this.dedup = dedup;
        loopNormals = setNormals ? Vec3Attribute.getOrCreate(BMeshAttribute.Normal, bmesh.loops()) : null;

        for(int i=0; i<corners.length; ++i)
            corners[i] = new Vector3f();

        for(int i=0; i<intersectionPoints.length; ++i) {
            intersectionPoints[i] = new Vector3f();
            normals[i] = new Vector3f();
        }
    }


    public void setPosition(Vector3f position) {
        for(int i=0; i<corners.length; ++i)
            corners[i].set(position);

        // Bottom plane
        corners[1].x += cellSize;
        corners[2].x += cellSize; corners[2].z += cellSize;
        corners[3].z += cellSize;

        // Top plane
        corners[4].y += cellSize;
        corners[5].x += cellSize; corners[5].y += cellSize;
        corners[6].x += cellSize; corners[6].y += cellSize; corners[6].z += cellSize;
        corners[7].y += cellSize; corners[7].z += cellSize;
    }


    public int process(DistanceFunction dfunc) {
        // Check which corners of the cube are inside (bits set = inside = negative distance)
        short cornerMask = 0;
        for(int i=0; i<corners.length; ++i) {
            dist[i] = dfunc.dist(corners[i]);
            if(dist[i] <= 0.0f)
                cornerMask |= (1 << i);
        }

        int intersectingEdges = Tables.MC_edgeTable[cornerMask];
        if(intersectingEdges == 0)
            return 0;

        int walkDirections = 0;

        // Calculate intersection points between edges and distance function
        for(int i=0; i<12; ++i, intersectingEdges >>>= 1) {
            if((intersectingEdges & 1) == 0)
                continue;

            walkDirections |= Tables.EDGE_WALK_DIRECTIONS[i];

            int idxV0 = Tables.edgeVertexTable[i][0];
            int idxV1 = Tables.edgeVertexTable[i][1];

            // Linear interpolation between corner distances
            float t = dist[idxV0] / (dist[idxV0] - dist[idxV1]);

            Vector3f p = intersectionPoints[i];
            p.set(corners[idxV1]);
            p.subtractLocal(corners[idxV0]);
            p.multLocal(t);
            p.addLocal(corners[idxV0]);

            if(loopNormals != null)
                dfunc.normal(p, normals[i]);
        }

        // Create triangles
        byte[] triangles = Tables.MC_triTable[cornerMask];
        for(int i=0; triangles[i] >= 0; ) {
            int idx0 = triangles[i++];
            int idx1 = triangles[i++];
            int idx2 = triangles[i++];

            Vertex v0 = dedup.getOrCreateVertex(intersectionPoints[idx0]);
            Vertex v1 = dedup.getOrCreateVertex(intersectionPoints[idx1]);
            Vertex v2 = dedup.getOrCreateVertex(intersectionPoints[idx2]);

            if(v0 != v1 && v1 != v2 && v0 != v2) {
                Face face = bmesh.createFace(v0, v1, v2);

                if(loopNormals != null) {
                    loopNormals.set(face.loop, normals[idx0]);
                    loopNormals.set(face.loop.nextFaceLoop, normals[idx1]);
                    loopNormals.set(face.loop.prevFaceLoop, normals[idx2]);
                }
            }
        }

        return walkDirections;
    }


    // It is said that this is a more accurate interpolation
    // http://paulbourke.net/geometry/polygonise/interp.c
    /*mpVector LinearInterp(mp4Vector p1, mp4Vector p2, float value) {
        if (p2 < p1) {
            mp4Vector temp;
            temp = p1;
            p1 = p2;
            p2 = temp;
        }

        mpVector p;
        if(fabs(p1.val - p2.val) > 0.00001)
            p = (mpVector)p1 + ((mpVector)p2 - (mpVector)p1)/(p2.val - p1.val)*(value - p1.val);
        else
            p = (mpVector)p1;
        return p;
    }

    bool operator<(const mp4Vector &right) const {
        if (x < right.x)
            return true;
        else if (x > right.x)
            return false;

        if (y < right.y)
            return true;
        else if (y > right.y)
            return false;

        if (z < right.z)
            return true;
        else if (z > right.z)
            return false;

        return false;
    }*/
}
