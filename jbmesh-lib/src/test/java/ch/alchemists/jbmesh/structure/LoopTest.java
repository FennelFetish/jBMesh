// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

public class LoopTest {
    @Test
    public void testCtor() {
        Loop loop = new Loop();

        assertNull(loop.edge);
        assertNull(loop.face);
        assertNull(loop.vertex);
        assertNull(loop.nextFaceLoop);

        assertEquals(loop, loop.nextEdgeLoop);
    }
}
