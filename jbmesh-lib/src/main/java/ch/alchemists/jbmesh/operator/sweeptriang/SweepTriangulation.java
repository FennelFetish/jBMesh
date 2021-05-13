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
import java.util.List;
import java.util.TreeSet;

public class SweepTriangulation {
    private final BMesh bmesh;
    private final FaceOps faceOps;
    private final Vec3Property<Vertex> propPosition;

    private final TreeSet<SweepVertex> sweepVertices = new TreeSet<>();
    private final EdgeSet edges = new EdgeSet();

    public float yLimit = 0;


    public SweepTriangulation(BMesh bmesh) {
        this.bmesh = bmesh;
        this.faceOps = new FaceOps(bmesh);
        this.propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    private PlanarCoordinateSystem createCoordSystemTest(List<Vertex> vertices, Face face) {
        PlanarCoordinateSystem coordSys = PlanarCoordinateSystem.withX(Vector3f.UNIT_X, Vector3f.UNIT_Z);

        DebugVisual.setPointTransformation("SweepTriangulation", v -> {
            return coordSys.unproject(new Vector2f(v.x, v.y));
        });

        DebugVisual.setPointTransformation("SweepTriangles", v -> {
            return coordSys.unproject(new Vector2f(v.x, v.y));
        });

        return coordSys;
    }

    private PlanarCoordinateSystem createCoordSystem(List<Vertex> vertices, Face face) {
        Vector3f dirSum = new Vector3f();
        Vector3f dir = new Vector3f();

        for(Vertex v : vertices) {
            propPosition.get(v, dir);
            if(dir.dot(Vector3f.UNIT_X) < 0)
                dir.negateLocal();

            dirSum.addLocal(dir);
        }
        //dirSum.divideLocal(vertices.size());
        //System.out.println("dirSum: " + dirSum);

        Vector3f n = faceOps.normal(face);
        PlanarCoordinateSystem coordSys = PlanarCoordinateSystem.withY(Vector3f.ZERO, dirSum, n);

        DebugVisual.setPointTransformation("SweepTriangulation", v -> {
            return coordSys.unproject(new Vector2f(v.x, v.y));
        });

        DebugVisual.setPointTransformation("SweepTriangles", v -> {
            return coordSys.unproject(new Vector2f(v.x, v.y));
        });

        return coordSys;
    }


    public void apply(Face face) {
        System.out.println("SweepTriangulation.apply ----------------------------------------------------");
        System.out.println("limit: " + yLimit);

        List<Vertex> vertices = face.getVertices();
        if(vertices.size() < 3)
            throw new IllegalArgumentException("Triangulation needs at least 3 vertices");

        PlanarCoordinateSystem coordSys = createCoordSystemTest(vertices, face);

        try {
            createSweepVertices(vertices, coordSys);
            //printVertices(coordSys);
            process();
        }
        finally {
            sweepVertices.clear();
            edges.clear();
        }
    }


    private void printVertices(PlanarCoordinateSystem coordSys) {
        System.out.println("Vertices:");
        int i=0;
        for(SweepVertex v : sweepVertices) {
            System.out.println("  Vertex[" + i + "]: " + coordSys.unproject(v.p) );
            i++;
        }
    }


    private void createSweepVertices(List<Vertex> vertices, PlanarCoordinateSystem coordSys) {
        Vector3f pos = new Vector3f();

        SweepVertex first = new SweepVertex(0);
        propPosition.get(vertices.get(0), pos);
        coordSys.project(pos, first.p);
        sweepVertices.add(first);

        SweepVertex prev = first;

        for(int i=1; i<vertices.size(); ++i) {
            SweepVertex current = new SweepVertex(i);
            propPosition.get(vertices.get(i), pos);
            coordSys.project(pos, current.p);

            // TODO: Similiar vertices may be desired for carrying different attributes?
            if(current.p.isSimilar(prev.p, 0.0001f))
                continue;

            // TODO: Skip collinear spikes

            current.prev = prev;
            prev.next = current;
            prev = current;

            // Add after setting references because they are used for sorting bow-tie vertices
            sweepVertices.add(current);
        }

        first.prev = prev;
        prev.next = first;
    }


    private void process() {
        for(SweepVertex v : sweepVertices) {
            if(v.p.y > yLimit)
                break;

            System.out.println("===[ handleSweepVertex " + (v.index+1) + ": " + v.p + " ]===");
            handleSweepVertex(v);
            //edges.printEdges();
        }

        edges.debug(yLimit);

        /*for(SweepVertex v : monotoneStarts) {
            DebugVisual.get("SweepTriangulation").addPoint(new Vector3f(v.p.x, v.p.y, 0));
        }*/
    }


    private void handleSweepVertex(SweepVertex v) {
        float y = v.p.y;
        float yPrev = v.prev.p.y;
        float yNext = v.next.p.y;

        // v.prev is below
        if(yPrev < y) {
            if(yNext > y)
                handleContinuation(v);
            else if(isLeftTurn(v))
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
            else if(isLeftTurn(v))
                handleStart(v);
            else if(yNext == y)
                handleContinuation(v);
            else
                handleSplit(v);
        }
        // v.prev on same height
        else  {
            if(yNext == y || isLeftTurn(v))
                handleContinuation(v);
            else if(yNext < y)
                handleMerge(v);
            else
                handleSplit(v);
        }
    }


    private static boolean isLeftTurn(SweepVertex v) {
        Vector2f v1 = v.p.subtract(v.prev.p);
        Vector2f v2 = v.next.p.subtract(v.prev.p);
        return v1.determinant(v2) >= 0;
    }


    private void handleStart(SweepVertex v) {
        System.out.println("  >> Start Vertex");

        SweepEdge leftEdge = new SweepEdge(v, v.prev);
        leftEdge.monotoneSweep = new MonotoneSweep(v);
        leftEdge.lastVertex = v;
        edges.addEdge(leftEdge);
    }


    private void handleSplit(SweepVertex v) {
        System.out.println("  >> Split Vertex");

        SweepEdge leftEdge = edges.getEdge(v.p.x, v.p.y);
        assert leftEdge != null;
        assert leftEdge.lastVertex != null;

        SweepEdge rightEdge = new SweepEdge(v, v.prev);

        // Connection to left chain
        if(leftEdge.lastVertex == leftEdge.start) {
            leftEdge.lastVertex.connectMonotonePath(v);
            rightEdge.monotoneSweep = leftEdge.monotoneSweep;
            leftEdge.monotoneSweep = new MonotoneSweep(leftEdge.lastVertex);
        }
        // Connection to mergeVertex
        else if(leftEdge.lastMergeVertex != null) {
            leftEdge.lastMergeVertex.connectMonotonePath(v);
            leftEdge.lastMergeVertex = null;

            rightEdge.monotoneSweep = leftEdge.waitingMonotoneSweep;
            leftEdge.waitingMonotoneSweep = null;
        }
        // Connection to right chain
        else {
            leftEdge.lastVertex.connectMonotonePath(v);
            rightEdge.monotoneSweep = new MonotoneSweep(leftEdge.lastVertex);
        }

        leftEdge.monotoneSweep.processRight(v);
        leftEdge.lastVertex = v;

        rightEdge.monotoneSweep.processLeft(v);
        rightEdge.lastVertex = v;
        edges.addEdge(rightEdge);
    }


    private void handleMerge(SweepVertex v) {
        System.out.println("  >> Merge Vertex");

        // This will also connect lastMergeVertex of the removed edge to v
        SweepEdge edge = edges.removeEdge(v);
        edge.monotoneSweep.processRight(v);
        edge.lastVertex = v;
    }


    private void handleEnd(SweepVertex v) {
        System.out.println("  >> End Vertex");

        // This will also connect lastMergeVertex of the removed edge to v
        SweepEdge removedEdge = edges.removeEndEdge(v);
        removedEdge.monotoneSweep.processEnd(v);

        if(removedEdge.lastMergeVertex != null) {
            removedEdge.lastMergeVertex.connectMonotonePath(v);
            removedEdge.waitingMonotoneSweep.processEnd(v);
        }
    }


    private void handleContinuation(SweepVertex v) {
        System.out.println("  >> Continuation Vertex");

        SweepEdge edge = edges.getEdge(v.p.x, v.p.y);
        assert edge != null; // If this happens, some edges were crossing?
        edge.lastVertex = v;

        // Left edge continues
        if(edge.end == v) {
            SweepVertex next = getContinuationVertex(edge);
            edge.reset(v, next);

            if(edge.lastMergeVertex != null) {
                edge.lastMergeVertex.connectMonotonePath(v);
                edge.lastMergeVertex = null;

                edge.monotoneSweep.processEnd(v);
                edge.monotoneSweep = edge.waitingMonotoneSweep;
                edge.waitingMonotoneSweep = null;
            }

            edge.monotoneSweep.processLeft(v);
        }
        // Right edge continues
        else {
            if(edge.lastMergeVertex != null) {
                edge.lastMergeVertex.connectMonotonePath(v);
                edge.lastMergeVertex = null;

                edge.waitingMonotoneSweep.processEnd(v);
                edge.waitingMonotoneSweep = null;
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


    /*private void tryConnectLastMergeVertex(SweepEdge edge, SweepVertex targetVertex) {
        if(edge.lastMergeVertex != null) {
            edge.lastMergeVertex.connectMonotonePath(targetVertex);
            //edge.waitingMonotoneSweep
            edge.lastMergeVertex = null;
        }
    }*/
}
