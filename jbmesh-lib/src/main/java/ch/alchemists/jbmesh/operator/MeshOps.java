// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Edge;
import ch.alchemists.jbmesh.structure.Face;

public class MeshOps {
    public static void invert(BMesh bmesh) {
        for(Face f : bmesh.faces())
            bmesh.invertFace(f);
    }


    public static void mergePlanarFaces(BMesh bmesh) {
        FaceOps faceOps = new FaceOps(bmesh);
        for(Edge e : bmesh.edges().getAll()) {
            Face f1 = e.loop.face;
            Face f2 = e.loop.nextEdgeLoop.face;

            if(f1 != f2 && faceOps.coplanar(f1, f2) && f1.countCommonEdges(f2) == 1)
                bmesh.joinFace(f1, f2, e);
        }
    }
}
