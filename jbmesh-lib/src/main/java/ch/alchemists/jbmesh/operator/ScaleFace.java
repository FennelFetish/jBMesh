// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;

public class ScaleFace {
    private final FaceOps faceOps;
    private final Vec3Attribute<Vertex> positions;

    private float scale = 1.0f;

    // TODO: Define pivot point (alternatives to centroid)


    public ScaleFace(BMesh bmesh, float scale) {
        faceOps = new FaceOps(bmesh);
        positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
        this.scale = scale;
    }


    public void apply(Face face) {
        Vector3f centroid = faceOps.centroid(face);
        Vector3f p = new Vector3f();

        for(Vertex vertex : face.vertices()) {
            positions.get(vertex, p);
            p.subtractLocal(centroid);
            p.multLocal(scale);
            p.addLocal(centroid);
            positions.set(vertex, p);
        }
    }
}
