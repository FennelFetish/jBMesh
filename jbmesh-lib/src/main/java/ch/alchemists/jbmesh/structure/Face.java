package ch.alchemists.jbmesh.structure;

import ch.alchemists.jbmesh.data.Element;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Face extends Element {
    // Property names
    public static final String Normal = "FaceNormal";


    // Never null on a valid object
    public Loop loop;


    Face() {}


    @Override
    protected void releaseElement() {
        loop = null;
    }


    public Edge getAnyCommonEdge(Face face) {
        for(Loop l1 : loops()) {
            for(Loop l2 : face.loops()) {
                if(l1.edge == l2.edge)
                    return loop.edge;
            }
        }

        return null;
    }

    public List<Edge> getCommonEdges(Face face) {
        List<Edge> edges = new ArrayList<>(4);
        for(Loop l1 : loops()) {
            for(Loop l2 : face.loops()) {
                if(l1.edge == l2.edge)
                    edges.add(loop.edge);
            }
        }

        return edges;
    }

    public int countCommonEdges(Face face) {
        int commonEdges = 0;
        for(Loop l1 : loops()) {
            for(Loop l2 : face.loops()) {
                if(l1.edge == l2.edge)
                    commonEdges++;
            }
        }

        return commonEdges;
    }


    public int countVertices(Face face) {
        int count = 0;
        Loop current = loop;
        do {
            current = current.nextFaceLoop;
            count++;
        } while(current != loop);
        return count;
    }


    public List<Vertex> getVertices() {
        return getVertices(new ArrayList<>(4));
    }

    public List<Vertex> getVertices(List<Vertex> list) {
        Loop current = loop;
        do {
            list.add(current.vertex);
            current = current.nextFaceLoop;
        } while(current != loop);

        return list;
    }

    public Iterable<Vertex> vertices() {
        return () -> new FaceVertexIterator(loop);
    }


    public List<Loop> getLoops() {
        return getLoops(new ArrayList<>(4));
    }

    public List<Loop> getLoops(List<Loop> list) {
        Loop current = loop;
        do {
            list.add(current);
            current = current.nextFaceLoop;
        } while(current != loop);

        return list;
    }

    public Iterable<Loop> loops() { // TODO: Replace with vertices() where possible
        return () -> new FaceLoopIterator(loop);
    }


    private static class FaceLoopIterator implements Iterator<Loop> {
        private final Loop startLoop;
        private Loop currentLoop;
        private boolean first = true;

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


    private static class FaceVertexIterator implements Iterator<Vertex> {
        private final FaceLoopIterator it;

        public FaceVertexIterator(Loop loop) {
            it = new FaceLoopIterator(loop);
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Vertex next() {
            return it.next().vertex;
        }
    }
}
