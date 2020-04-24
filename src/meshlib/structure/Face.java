package meshlib.structure;

import java.util.Iterator;
import meshlib.data.Element;

public class Face extends Element {
    // Never NULL
    public Loop loop;


    Face() {}


    @Override
    protected void releaseElement() {
        loop = null;
    }


    public Iterable<Vertex> vertices() {
        return () -> new FaceVertexIterator(loop);
    }


    private static class FaceVertexIterator implements Iterator<Vertex> {
        private final Loop startLoop;
        private Loop currentLoop;
        private boolean first = true; // Get rid of this flag? (emulate do-while)

        public FaceVertexIterator(Loop loop) {
            startLoop = loop;
            this.currentLoop = loop;
        }

        @Override
        public boolean hasNext() {
            return currentLoop != startLoop || first;
        }

        @Override
        public Vertex next() {
            first = false;
            Vertex vertex = currentLoop.vertex;
            currentLoop = currentLoop.nextFaceLoop;
            return vertex;
        }
    }
}
