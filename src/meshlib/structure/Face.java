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


    public int getVertexCount(Face face) {
        int count = 0;
        Loop current = loop;
        do {
            current = current.nextFaceLoop;
            count++;
        } while(current != loop);
        return count;
    }


    // TODO: Can have multiple common edges!
    public Edge getAnyCommonEdge(Face face) {
        for(Loop l1 : loops()) {
            for(Loop l2 : face.loops()) {
                if(l1.edge == l2.edge)
                    return loop.edge;
            }
        }

        return null;
    }


    public Iterable<Loop> loops() {
        return () -> new FaceLoopIterator(loop);
    }


    private static class FaceLoopIterator implements Iterator<Loop> {
        private final Loop startLoop;
        private Loop currentLoop;
        private boolean first = true; // Get rid of this flag? (emulate do-while), also doesn't work with prevFaceLoops

        public FaceLoopIterator(Loop loop) {
            startLoop = loop;
            currentLoop = loop;
        }

        @Override
        public boolean hasNext() {
            return currentLoop != startLoop || first;
        }

        @Override
        public Loop next() {
            first = false;
            Loop loop = currentLoop;
            currentLoop = currentLoop.nextFaceLoop;
            return loop;
        }
    }
}
