// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.lookup;

import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;

public interface VertexDeduplication {
    void addExisting(Vertex vertex);
    void clear();

    Vertex getVertex(Vector3f location);
    Vertex getOrCreateVertex(Vector3f location);
}
