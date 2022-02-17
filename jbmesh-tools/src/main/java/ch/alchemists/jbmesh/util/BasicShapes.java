// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.util;

import ch.alchemists.jbmesh.conversion.LineExport;
import ch.alchemists.jbmesh.conversion.TriangleExport;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;

// TODO: Move to jbmesh-lib
public abstract class BasicShapes {
    // Circles

    public static Face createDisc(BMesh bmesh, PlanarCoordinateSystem coordSys, int numSamples, float radius) {
        float angleBetweenSamples = FastMath.TWO_PI / numSamples;
        Vector2f p = new Vector2f();
        Vector3f temp = new Vector3f();

        Vertex[] vertices = new Vertex[numSamples];
        for(int i=0; i<numSamples; ++i) {
            float angle = i * angleBetweenSamples;
            p.x = FastMath.cos(angle) * radius;
            p.y = FastMath.sin(angle) * radius;
            coordSys.unproject(p, temp);
            vertices[i] = bmesh.createVertex(temp);
        }

        return bmesh.createFace(vertices);
    }


    public static Face createDiscXY(BMesh bmesh, Vector3f center, int numSamples, float radius) {
        float angleBetweenSamples = FastMath.TWO_PI / numSamples;

        Vertex[] vertices = new Vertex[numSamples];
        for(int i=0; i<numSamples; ++i) {
            float angle = i * angleBetweenSamples;
            float x = FastMath.cos(angle) * radius;
            float y = FastMath.sin(angle) * radius;
            vertices[i] = bmesh.createVertex(center.x + x, center.y + y, center.z);
        }

        return bmesh.createFace(vertices);
    }


    // Quads

    public static Face createQuad(BMesh bmesh, PlanarCoordinateSystem coordSys, float width, float height) {
        Vertex v0 = bmesh.createVertex( coordSys.unproject(0, 0) );
        Vertex v1 = bmesh.createVertex( coordSys.unproject(width, 0) );
        Vertex v2 = bmesh.createVertex( coordSys.unproject(width, height) );
        Vertex v3 = bmesh.createVertex( coordSys.unproject(0, height) );
        return bmesh.createFace(v0, v1, v2, v3);
    }

    public static Face createSquare(BMesh bmesh, PlanarCoordinateSystem coordSys, float sideLength) {
        return createQuad(bmesh, coordSys, sideLength, sideLength);
    }


    // JME Mesh

    public static Mesh disc(int numSamples, float radius) {
        BMesh bmesh = new BMesh();
        createDiscXY(bmesh, Vector3f.ZERO, numSamples, radius);
        return TriangleExport.apply(bmesh);
    }


    public static Mesh circle(int numSamples, float radius) {
        BMesh bmesh = new BMesh();
        createDiscXY(bmesh, Vector3f.ZERO, numSamples, radius);
        return LineExport.apply(bmesh);
    }
}
