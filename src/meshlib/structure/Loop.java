package meshlib.structure;

// a) On each side of a BMFace  - iterate with 'nextFaceLoop'
// b) Runs along a BMEdge       - iterate with 'nextEdgeLoop'

// (stores per-face-vertex data, UV's, vertex-colors, etc)
public class Loop {
    private int index;

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


    private Loop() {}


    static final BMeshData.ElementAccessor<Loop> ACCESSOR = new BMeshData.ElementAccessor<Loop>() {
        @Override
        public Loop create() {
            return new Loop();
        }

        @Override
        public void release(Loop element) {
            element.index = -1;
            element.face = null;
            element.edge = null;
            element.vertex = null;
            element.nextFaceLoop = null;
            element.nextEdgeLoop = null;
        }

        @Override
        public int getIndex(Loop element) {
            return element.index;
        }

        @Override
        public void setIndex(Loop element, int index) {
            element.index = index;
        }
    };
}
