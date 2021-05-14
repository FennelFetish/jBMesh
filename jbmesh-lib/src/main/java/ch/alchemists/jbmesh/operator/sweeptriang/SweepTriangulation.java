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
    public interface TriangleCallback {
        void handleTriangleIndices(int i1, int i2, int i3);
    }


    private final FaceOps faceOps;
    private final Vec3Property<Vertex> propPosition;

    private final TreeSet<SweepVertex> sweepVertices = new TreeSet<>();
    private final EdgeSet edges = new EdgeSet();
    private TriangleCallback cb;

    public float yLimit = 0;


    public SweepTriangulation(BMesh bmesh) {
        this.faceOps = new FaceOps(bmesh);
        this.propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    public void setTriangleCallback(TriangleCallback callback) {
        this.cb = callback;
    }


    private PlanarCoordinateSystem createCoordSystemTest(List<Vertex> vertices, Face face) {
        PlanarCoordinateSystem coordSys = PlanarCoordinateSystem.withX(Vector3f.UNIT_X, Vector3f.UNIT_Z);

        DebugVisual.setPointTransformation("SweepTriangulation", v -> {
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

        return coordSys;
    }


    public void apply(Face face) {
        //System.out.println("SweepTriangulation.apply ----------------------------------------------------");
        //System.out.println("limit: " + yLimit);

        List<Vertex> vertices = face.getVertices();
        if(vertices.size() < 3)
            throw new IllegalArgumentException("Triangulation needs at least 3 vertices");

        PlanarCoordinateSystem coordSys = createCoordSystemTest(vertices, face);

        cb = (i1, i2, i3) -> {
            Vector3f p1 = propPosition.get(vertices.get(i1));
            Vector3f p2 = propPosition.get(vertices.get(i2));
            Vector3f p3 = propPosition.get(vertices.get(i3));
            DebugVisual.get("SweepTriangles").addFace(p1, p2, p3);
            //System.out.println("Triangle: " + i1 + " " + i2 + " " + i3);
        };

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
            //       -> No, you would use own triangulation.
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

            //System.out.println("===[ handleSweepVertex " + (v.index+1) + ": " + v.p + " ]===");
            handleSweepVertex(v);
            //edges.printEdges();
        }

        edges.debug(yLimit);
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
        SweepEdge leftEdge = new SweepEdge(v, v.prev);
        leftEdge.monotoneSweep = new MonotoneSweep(v, cb);
        edges.addEdge(leftEdge);
    }


    private void handleSplit(SweepVertex v) {
        SweepEdge leftEdge  = edges.getEdge(v);
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
        SweepEdge removedEdge = edges.removeEdge(v);
        removedEdge.monotoneSweep.processEnd(v);

        if(removedEdge.lastMerge != null) {
            //drawMonotonePath(removedEdge.lastMerge.getLastVertex(), v);
            removedEdge.lastMerge.processEnd(v);
        }
    }


    private void handleContinuation(SweepVertex v) {
        SweepEdge edge = edges.getEdge(v);
        assert edge != null; // If this happens, some edges were crossing?

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
        //System.out.println("Connecting monotone path from " + src + " to " + dest);

        // Draw debug line for monotone paths
        Vector3f start = new Vector3f(src.p.x, src.p.y, 0);
        Vector3f end = new Vector3f(dest.p.x, dest.p.y, 0);
        DebugVisual.get("SweepTriangulation").addArrow(start, end);
    }
}
