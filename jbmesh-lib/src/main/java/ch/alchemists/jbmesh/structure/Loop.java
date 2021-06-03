// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.structure;

import ch.alchemists.jbmesh.data.Element;

public class Loop extends Element {
    // Never null on a valid object
    public Face face;

    // Never null on a valid object
    public Edge edge;

    // Reference is needed for properly defining winding order.
    // Can't rely on BMEdge's reference, since BMEdge has no specifc direction.
    // Never null on a valid object
    public Vertex vertex; // source
    // Can also store in this loop whether the vertex was merged/remapped during conversion

    // Loop Cycle: Loop around face (iterate to list vertices of a face)
    // Never null on a valid object
    public Loop nextFaceLoop; // Blender calls this next
    public Loop prevFaceLoop; // prev

    // Radial Cycle: Loop around edge (iterate to list faces on an edge)
    // Never null on a valid object
    public Loop nextEdgeLoop = this; // Blender calls this radialNext
    public Loop prevEdgeLoop = this; // radialPrev


    Loop() {}


    @Override
    protected void releaseElement() {
        face = null;
        edge = null;
        vertex = null;
        nextFaceLoop = null;
        prevFaceLoop = null;
        nextEdgeLoop = this;
        prevEdgeLoop = this;
    }


    public void faceSetBetween(final Loop prev, final Loop next) {
        //assert prev.nextFaceLoop == next;
        //assert next.prevFaceLoop == prev;

        prevFaceLoop = prev;
        nextFaceLoop = next;
        prev.nextFaceLoop = this;
        next.prevFaceLoop = this;
    }

    public void faceRemove() {
        nextFaceLoop.prevFaceLoop = prevFaceLoop;
        prevFaceLoop.nextFaceLoop = nextFaceLoop;
        prevFaceLoop = this;
        nextFaceLoop = this;
    }


    public void radialSetBetween(final Loop prev, final Loop next) {
        assert prev.nextEdgeLoop == next;
        assert next.prevEdgeLoop == prev;

        prevEdgeLoop = prev;
        nextEdgeLoop = next;
        prev.nextEdgeLoop = this;
        next.prevEdgeLoop = this;
    }

    public void radialRemove() {
        nextEdgeLoop.prevEdgeLoop = prevEdgeLoop;
        prevEdgeLoop.nextEdgeLoop = nextEdgeLoop;
        prevEdgeLoop = this;
        nextEdgeLoop = this;
    }
}
