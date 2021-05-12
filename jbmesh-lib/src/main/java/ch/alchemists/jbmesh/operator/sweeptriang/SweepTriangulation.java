package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.operator.FaceOps;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.DebugVisual;
import ch.alchemists.jbmesh.util.PlanarCoordinateSystem;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class SweepTriangulation {
    private final BMesh bmesh;
    private final FaceOps faceOps;
    private final Vec3Property<Vertex> propPosition;

    private final ArrayList<SweepVertex> sweepVertices = new ArrayList<>();

    public float yLimit = 0;


    public SweepTriangulation(BMesh bmesh) {
        this.bmesh = bmesh;
        this.faceOps = new FaceOps(bmesh);
        this.propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    public void apply(Face face) {
        System.out.println("SweepTriangulation.apply ----------------------------------------------------");
        System.out.println("limit: " + yLimit);
        DebugVisual.clear("SweepTriangulation");

        List<Vertex> vertices = face.getVertices();
        if(vertices.size() < 3) {
            System.out.println("SweepTriangulation needs at least 3 vertices");
            return;
        }

        Vector3f p0 = propPosition.get(vertices.get(0));
        Vector3f p1 = propPosition.get(vertices.get(1));
        Vector3f n = faceOps.normal(face);
        PlanarCoordinateSystem coordSys = new PlanarCoordinateSystem(p0, p1, n);

        DebugVisual.setPointTransformation("SweepTriangulation", v -> {
            return coordSys.unproject(new Vector2f(v.x, v.y));
        });

        assert sweepVertices.isEmpty();
        createSweepVertices(vertices, coordSys);

        /*System.out.println("Vertices:");
        for(int i=0; i<sweepVertices.length; ++i) {
            Vector2f p = new Vector2f(sweepVertices[i].p.x, sweepVertices[i].p.y);
            System.out.println("  Vertex[" + i + "]: " + coordSys.unproject(p) );
        }*/

        process();
        sweepVertices.clear();
    }


    private void createSweepVertices(List<Vertex> vertices, PlanarCoordinateSystem coordSys) {
        sweepVertices.ensureCapacity(vertices.size());

        SweepVertex first = addSweepVertex(vertices.get(0), 0, coordSys);
        SweepVertex prev = first;

        for(int i=1; i<vertices.size(); ++i) {
            SweepVertex current = addSweepVertex(vertices.get(i), i, coordSys);

            current.prev = prev;
            prev.next = current;

            prev = current;
        }

        first.prev = prev;
        prev.next = first;

        sweepVertices.sort(null);
    }


    private SweepVertex addSweepVertex(Vertex vertex, int index, PlanarCoordinateSystem coordSys) {
        SweepVertex sweepVertex = new SweepVertex(index);
        coordSys.project(propPosition.get(vertex), sweepVertex.p);
        sweepVertices.add(sweepVertex);
        return sweepVertex;
    }


    private void process() {
        IntervalSet intervals = new IntervalSet();

        for(int i=0; i<sweepVertices.size(); ++i) {
            SweepVertex v = sweepVertices.get(i);
            if(v.p.y > yLimit)
                break;

            System.out.println("handleSweepVertex " + i + ": " + v.p);
            handleSweepVertex(v, intervals);
        }

        intervals.debug(yLimit);
    }


    private void handleSweepVertex(SweepVertex v, IntervalSet intervals) {
        boolean prevUp = (v.p.y < v.prev.p.y);
        boolean nextUp = (v.p.y <= v.next.p.y);

        System.out.println("prevUp: " + prevUp + ", nextUp: " + nextUp);

        // Normal vertex, vertical continuation of edge
        if(prevUp != nextUp) {
            System.out.println("  >> Continuation");
            handleContinuation(v, intervals);
        }
        // Both edges point upwards (+y)
        else if(prevUp) {
            SweepInterval interval = intervals.getInterval(v);

            // Start vertex
            if(interval == null) {
                System.out.println("  >> Start Vertex");
                handleStart(v, intervals);
            }
            // Split vertex
            else {
                System.out.println("  >> Split Vertex");
                handleSplit(v, interval, intervals);
            }
        }
        // Both edges point downwards (-y)
        else {
            assert !prevUp;
            handleMerge(v, intervals);
        }
    }


    private void handleStart(SweepVertex v, IntervalSet intervals) {
        SweepInterval interval = intervals.addInterval(v);
        interval.leftEdge  = new SweepEdge(v, v.prev, interval);
        interval.rightEdge = new SweepEdge(v, v.next, interval);
        interval.lastVertex = v;

        // TODO: Connect to last merge vertex?
    }


    private void handleSplit(SweepVertex v, SweepInterval leftInterval, IntervalSet intervals) {
        // Draw debug line for monotone paths
        Vector3f start = new Vector3f(v.p.x, v.p.y, 0);
        Vector3f end   = new Vector3f(leftInterval.lastVertex.p.x, leftInterval.lastVertex.p.y, 0);
        DebugVisual.get("SweepTriangulation").addLine(start, end);

        // Connect v to interval.lastVertex for y-monotone chain
        leftInterval.lastVertex.monotonePath = v;

        if(leftInterval.lastMergeVertex != null)
            connectToLastMerge(v, leftInterval);


        SweepInterval rightInterval = intervals.addInterval(v);
        rightInterval.leftEdge = new SweepEdge(v, v.prev, rightInterval);
        rightInterval.rightEdge = leftInterval.rightEdge;
        rightInterval.rightEdge.interval = rightInterval;
        rightInterval.lastVertex = v;

        leftInterval.rightEdge = new SweepEdge(v, v.next, leftInterval);
        leftInterval.lastVertex = v;
    }


    private void handleMerge(SweepVertex v, IntervalSet intervals) {
        SweepInterval leftInterval  = intervals.getWhereRightEndpoint(v);
        SweepInterval rightInterval = intervals.getWhereLeftEndpoint(v);

        if(leftInterval.lastMergeVertex != null)
            connectToLastMerge(v, leftInterval);

        // Merge vertex
        if(leftInterval != rightInterval) {
            System.out.println("  >> Merge Vertex");

            //System.out.println("    leftEdge: " + leftInterval.leftEdge);
            //System.out.println("    rightEdge: " + rightInterval.rightEdge);

            // Combine intervals
            leftInterval.rightEdge = rightInterval.rightEdge;
            leftInterval.rightEdge.interval = leftInterval;
            leftInterval.lastVertex = v;

            if(rightInterval.lastMergeVertex != null)
                connectToLastMerge(v, rightInterval);

            if(v.p.y == v.prev.p.y)
                leftInterval.lastMergeVertex = v.prev;
            else
                leftInterval.lastMergeVertex = v;

            /*leftInterval.addMergeVertex(v);
            if(rightInterval.prevMergeVertices != null)
                leftInterval.prevMergeVertices.addAll(rightInterval.prevMergeVertices);*/
        }
        // End vertex, leftInterval == rightInterval
        else {
            // Nothing special, just remove interval (happens below)
            System.out.println("  >> End Vertex");
        }

        intervals.removeInterval(rightInterval);
    }


    private void handleContinuation(SweepVertex v, IntervalSet intervals) {
        SweepInterval interval = intervals.getIntervalOther(v);
        assert interval != null;
        interval.lastVertex = v;

        if(v == interval.leftEdge.end) {
            SweepVertex next = getNextVertex(interval.leftEdge);
            interval.leftEdge = new SweepEdge(v, next, interval);
        }
        else {
            assert v == interval.rightEdge.end;

            SweepVertex next = getNextVertex(interval.rightEdge);
            interval.rightEdge = new SweepEdge(v, next, interval);
        }

        if(interval.lastMergeVertex != null)
            connectToLastMerge(v, interval);
    }

    private SweepVertex getNextVertex(SweepEdge edge) {
        if(edge.end.prev == edge.start)
            return edge.end.next;
        else {
            assert edge.end.next == edge.start;
            return edge.end.prev;
        }
    }


    private void connectToLastMerge(SweepVertex v, SweepInterval interval) {
        // Draw debug line for monotone paths
        Vector3f start = new Vector3f(v.p.x, v.p.y, 0);
        Vector3f end = new Vector3f(interval.lastMergeVertex.p.x, interval.lastMergeVertex.p.y, 0);
        DebugVisual.get("SweepTriangulation").addLine(start, end);

        interval.lastMergeVertex.monotonePath = v;
        interval.lastMergeVertex = null;

        /*for(SweepVertex mergeVertex : interval.prevMergeVertices) {
            // Draw debug line for monotone paths
            Vector3f start = new Vector3f(v.p.x, v.p.y, 0);
            Vector3f end = new Vector3f(mergeVertex.p.x, mergeVertex.p.y, 0);
            DebugVisual.get("SweepTriangulation").addLine(start, end);
        }

        interval.prevMergeVertices = null;*/
    }
}
