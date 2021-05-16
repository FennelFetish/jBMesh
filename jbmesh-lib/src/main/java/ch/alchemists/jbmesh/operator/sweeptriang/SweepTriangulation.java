package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.DebugVisual;
import com.jme3.math.Vector3f;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;

public class SweepTriangulation {
    public interface TriangleCallback {
        void handleTriangle(SweepVertex v1, SweepVertex v2, SweepVertex v3);
    }


    private final TreeSet<SweepVertex> sweepVertices = new TreeSet<>();
    private final EdgeSet edges = new EdgeSet();

    private final Preparation preparation = new Preparation(sweepVertices);
    private final Vec3Property<Vertex> propPosition; // TODO: Remove? Pass to addFace()?
    private TriangleCallback cb;

    public float yLimit = Float.POSITIVE_INFINITY;


    public SweepTriangulation(BMesh bmesh) {
        this.propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    public void setTriangleCallback(TriangleCallback callback) {
        this.cb = callback;
    }


    public void addFace(Face face) {
        preparation.addFace(face.loops(), loop -> propPosition.get(loop.vertex), loop -> loop.vertex);
    }

    public void addFace(List<Vector3f> face) {
        preparation.addFace(face, Function.identity(), v -> null);
    }


    public void triangulate() {
        //System.out.println("SweepTriangulation.triangulate ----------------------------------------------------");
        //System.out.println("limit: " + yLimit);

        if(sweepVertices.size() < 3)
            throw new IllegalArgumentException("Triangulation needs at least 3 valid vertices");

        if(cb == null)
            throw new IllegalArgumentException("Missing TriangleCallback");

        try {
            for(SweepVertex v : sweepVertices) {
                if(v.p.y > yLimit)
                    break;

                //System.out.println("===[ handleSweepVertex " + (v.index+1) + ": " + v.p + " ]===");
                handleSweepVertex(v);
                //edges.printEdges(yLimit);
            }

            //edges.debug(yLimit);
        }
        finally {
            sweepVertices.clear();
            edges.clear();
            preparation.reset();
        }
    }


    private void handleSweepVertex(SweepVertex v) {
        float y = v.p.y;
        float yPrev = v.prev.p.y;
        float yNext = v.next.p.y;

        // v.prev is below
        if(yPrev < y) {
            if(yNext > y)
                handleContinuation(v);
            else if(v.leftTurn)
                handleEnd(v);
            else if(yNext == y)
                handleContinuation(v);
            else
                handleMerge(v);
        }
        // v.prev is above
        else if(yPrev > y) {
            if(yNext < y)
                handleContinuation(v);
            else if(v.leftTurn)
                handleStart(v);
            else if(yNext == y)
                handleContinuation(v);
            else
                handleSplit(v);
        }
        // v.prev on same height
        else  {
            if(yNext == y || v.leftTurn)
                handleContinuation(v);
            else if(yNext < y)
                handleMerge(v);
            else
                handleSplit(v);
        }
    }


    private void handleStart(SweepVertex v) {
        //System.out.println(" >> Start");

        SweepEdge leftEdge = new SweepEdge(v, v.prev);
        leftEdge.monotoneSweep = new MonotoneSweep(v, cb);
        edges.addEdge(leftEdge);
    }


    private void handleSplit(SweepVertex v) {
        //System.out.println(" >> Split");

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

        leftEdge.monotoneSweep.processRight(v);
        rightEdge.monotoneSweep.processLeft(v);
        edges.addEdge(rightEdge);
    }


    private void handleMerge(SweepVertex v) {
        //System.out.println(" >> Merge");

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

        leftEdge.monotoneSweep.processRight(v);
        leftEdge.lastMerge = rightEdge.monotoneSweep; // Left edge will remember this merge
    }


    private void handleEnd(SweepVertex v) {
        //System.out.println(" >> End");

        SweepEdge removedEdge = edges.removeEdge(v);
        removedEdge.monotoneSweep.processEnd(v);

        if(removedEdge.lastMerge != null) {
            //drawMonotonePath(removedEdge.lastMerge.getLastVertex(), v);
            removedEdge.lastMerge.processEnd(v);
        }
    }


    private void handleContinuation(SweepVertex v) {
        //System.out.println(" >> Continuation");

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
        System.out.println("Connecting monotone path from " + src + " to " + dest);
        Vector3f start = new Vector3f(src.p.x, src.p.y, 0);
        Vector3f end = new Vector3f(dest.p.x, dest.p.y, 0);
        DebugVisual.get("SweepTriangulation").addArrow(start, end);
    }
}
