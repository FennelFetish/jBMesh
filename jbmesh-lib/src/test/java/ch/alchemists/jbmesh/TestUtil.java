// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh;

import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUtil {
    public static Loop[] getLoops(Face face) {
        ArrayList<Loop> loops = new ArrayList<>(3);
        for(Loop loop : face.loops())
            loops.add(loop);
        return loops.toArray(new Loop[loops.size()]);
    }


    public static void assertFace(Face face, Vertex... vertices) {
        Loop[] loops = getLoops(face);
        assertEquals(vertices.length, loops.length);

        int prevIndex = loops.length-1;
        for(int i=0; i<loops.length; ++i) {
            int nextIndex = (i+1) % loops.length;
            Loop loop = loops[i];

            assertEquals(loops[nextIndex], loop.nextFaceLoop);
            assertEquals(loops[prevIndex], loop.prevFaceLoop);
            
            assertEquals(face, loop.face);
            assertEquals(vertices[i], loop.vertex);
            assertTrue(loop.edge.connects(vertices[i], vertices[nextIndex]));
            assertEquals(loop.edge, vertices[i].getEdgeTo(vertices[nextIndex]));
            assertEquals(loop.edge, vertices[nextIndex].getEdgeTo(vertices[i]));

            prevIndex = i;
        }
    }


    public static void assertFloat(float expected, float actual) {
        final float epsilon = 0.001f;
        assertEquals(expected, actual, epsilon);
    }
}
