package meshlib.structure;

import meshlib.data.Element;

// a) On each side of a BMFace  - iterate with 'nextFaceLoop'
// b) Runs along a BMEdge       - iterate with 'nextEdgeLoop'

// (stores per-face-vertex data, UV's, vertex-colors, etc)
public class Loop extends Element {
    // Never null
    public Face face;

    // Never null
    public Edge edge;

    // Reference is needed for properly defining winding order.
    // Can't rely on BMEdge's reference, since BMEdge has no specifc direction.
    // Never null
    public Vertex vertex; // source
    // Can also store in this loop whether the vertex was merged/remapped during conversion

    // Loop Cycle: Loop around face (iterate to list vertices of a face)
    // Never null
    public Loop nextFaceLoop; // Blender calls this next
    public Loop prevFaceLoop; // prev

    // Radial Cycle: Loop around edge (iterate to list faces on an edge)
    // Never null
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
