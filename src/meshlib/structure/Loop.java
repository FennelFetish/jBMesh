package meshlib.structure;

import meshlib.data.Element;

// a) On each side of a BMFace  - iterate with 'nextFaceLoop'
// b) Runs along a BMEdge       - iterate with 'nextEdgeLoop'

// (stores per-face-vertex data, UV's, vertex-colors, etc)
public class Loop extends Element {
    public Face face;

    public Edge edge;

    // Reference is needed for properly defining winding order.
    // Can't rely on BMEdge's reference, since BMEdge has no specifc direction.
    public Vertex vertex; // source
    // Can also store in this loop whether the vertex was merged/remapped during conversion

    // Loop Cycle: Loop around face (iterate to list vertices of a face)
    public Loop nextFaceLoop;
    // prev?

    // Radial Cycle: Loop around edge (iterate to list faces on an edge)
    // Never null
    public Loop nextEdgeLoop = this;


    Loop() {}


    @Override
    protected void releaseElement() {
        face = null;
        edge = null;
        vertex = null;
        nextFaceLoop = null;
        nextEdgeLoop = null;
    }
}
