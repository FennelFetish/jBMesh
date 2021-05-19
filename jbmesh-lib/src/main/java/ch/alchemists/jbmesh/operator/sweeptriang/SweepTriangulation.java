package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.DebugVisual;
import ch.alchemists.jbmesh.util.PlanarCoordinateSystem;
import com.jme3.math.Vector3f;
import java.util.List;
import java.util.TreeSet;

public class SweepTriangulation {
    public interface TriangleCallback {
        void handleTriangle(SweepVertex v1, SweepVertex v2, SweepVertex v3);
    }


    private final TreeSet<SweepVertex> sweepVertices = new TreeSet<>();
    private final EdgeSet edges = new EdgeSet();

    private final Preparation preparation = new Preparation(sweepVertices);
    private final Vec3Property<Vertex> propPosition; // TODO: Remove? Pass to addFace()?
    private TriangleCallback cb;


    public SweepTriangulation(BMesh bmesh) {
        this.propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    public void setTriangleCallback(TriangleCallback callback) {
        this.cb = callback;
    }

    public void setCoordinateSystem(PlanarCoordinateSystem coordSys) {
        preparation.setCoordinateSystem(coordSys);
    }


    public void addFace(Face face) {
        preparation.addFace(face.loops(), new LoopExtractor(propPosition));
    }

    public void addFaceWithPositions(List<Vector3f> face) {
        preparation.addFace(face, new Vector3fExtractor());
    }

    public void addFaceWithLoops(List<Loop> face) {
        preparation.addFace(face, new LoopExtractor(propPosition));
    }


    public void triangulate() {
        if(sweepVertices.size() < 3)
            throw new IllegalArgumentException("Triangulation needs at least 3 valid vertices");

        if(cb == null)
            throw new IllegalArgumentException("Missing TriangleCallback");

        try {
            for(SweepVertex v : sweepVertices)
                handleSweepVertex(v);
        }
        finally {
            sweepVertices.clear();
            edges.clear();
            preparation.reset();
        }
    }


    public void triangulateDebug(float yLimit) {
        //System.out.println("SweepTriangulation.triangulateDebug ----------------------------------------------------");

        if(sweepVertices.size() < 3)
            throw new IllegalArgumentException("Triangulation needs at least 3 valid vertices");

        if(cb == null)
            throw new IllegalArgumentException("Missing TriangleCallback");

        final float limit = yLimit + sweepVertices.first().p.y;

        try {
            for(SweepVertex v : sweepVertices) {
                if(v.p.y > limit)
                    break;

                //System.out.println("===[ handleSweepVertex " + (v.index+1) + ": " + v.p + " ]===");
                handleSweepVertex(v);
                //edges.printEdges(yLimit);
            }

            edges.drawSweepSegments(limit);
        }
        finally {
            sweepVertices.clear();
            edges.clear();
            preparation.reset();
        }
    }


    private void handleSweepVertex(SweepVertex v) {
        boolean prevUp = v.prev.isAbove(v);
        boolean nextUp = v.next.isAbove(v);

        if(prevUp != nextUp)
            handleContinuation(v);
        else if(v.leftTurn) {
            if(prevUp)
                handleStart(v);
            else
                handleEnd(v);
        } else {
            if(prevUp)
                handleSplit(v);
            else
                handleMerge(v);
        }
    }


    private void handleStart(SweepVertex v) {
        SweepEdge leftEdge = new SweepEdge(v, v.prev);
        leftEdge.monotoneSweep = new MonotoneSweep(v, cb);
        edges.addEdge(leftEdge);

        //leftEdge.rightEdge = new SweepEdge(v, v.next);
    }


    private void handleSplit(SweepVertex v) {
        SweepEdge leftEdge  = edges.getEdge(v);
        assert leftEdge != null : "Intersections?"; // If this happens, some edges were crossing?

        SweepEdge rightEdge = new SweepEdge(v, v.prev);

        SweepVertex lastVertex = leftEdge.monotoneSweep.getLastVertex();

        // Connection to left chain
        if(lastVertex == leftEdge.start) {
            //drawMonotonePath(lastVertex, v);
            rightEdge.monotoneSweep = leftEdge.monotoneSweep;
            leftEdge.monotoneSweep = new MonotoneSweep(lastVertex, cb);
        }
        // Connection to mergeVertex
        else if(leftEdge.lastMerge != null) {
            //drawMonotonePath(leftEdge.lastMerge.getLastVertex(), v);
            rightEdge.monotoneSweep = leftEdge.lastMerge;
            leftEdge.lastMerge = null;
        }
        // Connection to right chain
        else {
            //drawMonotonePath(lastVertex, v);
            rightEdge.monotoneSweep = new MonotoneSweep(lastVertex, cb);
        }

        //rightEdge.rightEdge = leftEdge.rightEdge;
        //leftEdge.rightEdge = new SweepEdge(v, v.next);

        leftEdge.monotoneSweep.processRight(v);
        rightEdge.monotoneSweep.processLeft(v);
        edges.addEdge(rightEdge);
    }


    private void handleMerge(SweepVertex v) {
        // Remove and handle edge to the right
        SweepEdge rightEdge = edges.removeEdge(v);
        if(rightEdge.lastMerge != null) {
            //drawMonotonePath(rightEdge.lastMerge.getLastVertex(), v);
            rightEdge.monotoneSweep.processEnd(v);
            rightEdge.monotoneSweep = rightEdge.lastMerge;
        }

        rightEdge.monotoneSweep.processLeft(v);

        SweepEdge leftEdge = edges.getEdge(v);
        if(leftEdge.lastMerge != null) {
            //drawMonotonePath(leftEdge.lastMerge.getLastVertex(), v);
            leftEdge.lastMerge.processEnd(v);
        }

        //leftEdge.rightEdge = rightEdge.rightEdge;

        leftEdge.monotoneSweep.processRight(v);
        leftEdge.lastMerge = rightEdge.monotoneSweep; // Left edge will remember this merge
    }


    private void handleEnd(SweepVertex v) {
        SweepEdge removedEdge = edges.removeEdge(v);
        removedEdge.monotoneSweep.processEnd(v);

        if(removedEdge.lastMerge != null) {
            //drawMonotonePath(removedEdge.lastMerge.getLastVertex(), v);
            removedEdge.lastMerge.processEnd(v);
        }
    }


    private void handleContinuation(SweepVertex v) {
        SweepEdge edge = edges.getEdge(v);
        assert edge != null : "Intersections?"; // If this happens, some edges were crossing?

        // Left edge continues
        if(edge.end == v) {
            SweepVertex next = getContinuationVertex(edge);
            edge.reset(v, next);

            if(edge.lastMerge != null) {
                //drawMonotonePath(edge.lastMerge.getLastVertex(), v);
                edge.monotoneSweep.processEnd(v);
                edge.monotoneSweep = edge.lastMerge;
                edge.lastMerge = null;
            }

            edge.monotoneSweep.processLeft(v);
        }
        // Right edge continues
        else {
            //assert edge.rightEdge.end == v;
            //SweepVertex next = getContinuationVertex(edge.rightEdge);
            //edge.rightEdge.reset(v, next);

            if(edge.lastMerge != null) {
                //drawMonotonePath(edge.lastMerge.getLastVertex(), v);
                edge.lastMerge.processEnd(v);
                edge.lastMerge = null;
            }

            edge.monotoneSweep.processRight(v);
        }
    }

    private SweepVertex getContinuationVertex(SweepEdge edge) {
        if(edge.end.prev == edge.start)
            return edge.end.next;
        else {
            assert edge.end.next == edge.start;
            return edge.end.prev;
        }
    }


    private void drawMonotonePath(SweepVertex src, SweepVertex dest) {
        //System.out.println("Connecting monotone path from " + src + " to " + dest);
        Vector3f start = new Vector3f(src.p.x, src.p.y, 0);
        Vector3f end = new Vector3f(dest.p.x, dest.p.y, 0);
        DebugVisual.get("SweepTriangulation").addArrow(start, end);
    }



    private static class LoopExtractor implements Preparation.Extractor<Loop> {
        private final Vec3Property<Vertex> propPosition;

        private LoopExtractor(Vec3Property<Vertex> propPosition) {
            this.propPosition = propPosition;
        }

        @Override
        public Vector3f position(Loop loop, Vector3f store) {
            return propPosition.get(loop.vertex, store);
        }

        @Override
        public Vertex vertex(Loop loop) {
            return loop.vertex;
        }
    };


    private static class Vector3fExtractor implements Preparation.Extractor<Vector3f> {
        @Override
        public Vector3f position(Vector3f v, Vector3f store) {
            return store.set(v);
        }

        @Override
        public Vertex vertex(Vector3f loop) {
            return null;
        }
    };
}
