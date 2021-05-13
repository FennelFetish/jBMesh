package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.operator.FaceOps;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.DebugVisual;
import ch.alchemists.jbmesh.util.PlanarCoordinateSystem;
import ch.alchemists.jbmesh.util.Profiler;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class SweepTriangulation {
    private final BMesh bmesh;
    private final FaceOps faceOps;
    private final Vec3Property<Vertex> propPosition;

    private final ArrayList<SweepVertex> sweepVertices = new ArrayList<>();
    private final EdgeSet edges = new EdgeSet();

    public float yLimit = 0;


    public SweepTriangulation(BMesh bmesh) {
        this.bmesh = bmesh;
        this.faceOps = new FaceOps(bmesh);
        this.propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    private PlanarCoordinateSystem createCoordSystemTest(List<Vertex> vertices, Face face) {
        /*Vector3f p0 = propPosition.get(vertices.get(0));
        Vector3f p1 = propPosition.get(vertices.get(1));
        Vector3f n = faceOps.normal(face);

        PlanarCoordinateSystem coordSys = PlanarCoordinateSystem.withX(p0, p1, n);*/
        PlanarCoordinateSystem coordSys = PlanarCoordinateSystem.withX(Vector3f.UNIT_X, Vector3f.UNIT_Z);

        DebugVisual.setPointTransformation("SweepTriangulation", v -> {
            return coordSys.unproject(new Vector2f(v.x, v.y));
        });

        return coordSys;
    }

    private PlanarCoordinateSystem createCoordSystem(List<Vertex> vertices, Face face) {
        try(Profiler p = Profiler.start("createCoordSystem")) {
            Vector3f dirSum = new Vector3f();
            Vector3f dir = new Vector3f();

            for(Vertex v : vertices) {
                propPosition.get(v, dir);
                if(dir.dot(Vector3f.UNIT_X) < 0)
                    dir.negateLocal();

                //dir.x *= dir.x;
                //dir.y *= dir.y;
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
    }


    public void apply(Face face) {
        //System.out.println("SweepTriangulation.apply ----------------------------------------------------");
        //System.out.println("limit: " + yLimit);

        List<Vertex> vertices = face.getVertices();
        if(vertices.size() < 3)
            throw new IllegalArgumentException("Triangulation needs at least 3 vertices");

        PlanarCoordinateSystem coordSys = createCoordSystem(vertices, face);

        try {
            try(Profiler p = Profiler.start("createSweepVertices")) {
                createSweepVertices(vertices, coordSys);
            }

            //printVertices(coordSys);

            try(Profiler p = Profiler.start("process")) {
                process();
            }
        }
        finally {
            sweepVertices.clear();
            edges.clear();
        }
    }


    private void printVertices(PlanarCoordinateSystem coordSys) {
        System.out.println("Vertices:");
        for(int i=0; i<sweepVertices.size(); ++i) {
            System.out.println("  Vertex[" + i + "]: " + coordSys.unproject(sweepVertices.get(i).p) );
        }
    }


    private void createSweepVertices(List<Vertex> vertices, PlanarCoordinateSystem coordSys) {
        sweepVertices.ensureCapacity(vertices.size());
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

            sweepVertices.add(current);

            current.prev = prev;
            prev.next = current;

            prev = current;
        }

        first.prev = prev;
        prev.next = first;

        try(Profiler p = Profiler.start("sort")) {
            sweepVertices.sort(null);
        }
    }


    /*private SweepVertex createSweepVertex(Vertex vertex, int index, PlanarCoordinateSystem coordSys) {
        SweepVertex sweepVertex = new SweepVertex(index);
        coordSys.project(propPosition.get(vertex), sweepVertex.p);
        return sweepVertex;
    }*/


    private void process() {
        for(int i=0; i<sweepVertices.size(); ++i) {
            SweepVertex v = sweepVertices.get(i);
            /*if(v.p.y > yLimit)
                break;*/

            //System.out.println("===[ handleSweepVertex " + i + ": " + v.p + " ]===");
            handleSweepVertex(v);
            //edges.printEdges();
        }

        //edges.debug(yLimit);
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
            if(isLeftTurn(v) || yNext == y)
                handleContinuation(v);
            else if(yNext < y)
                handleMerge(v);
            else
                handleSplit(v);
        }
    }


    private void handleSweepVertexOld(SweepVertex v) {
        boolean prevUp = (v.p.y < v.prev.p.y);
        boolean nextUp = (v.p.y <= v.next.p.y);

        System.out.println("prevUp: " + prevUp + ", nextUp: " + nextUp);

        // One edge points upwards, one points downwards
        if(prevUp != nextUp) {
            System.out.println("  >> Continuation");
            handleContinuation(v);
            return;
        }

        // Both edges point upwards (+y)
        if(prevUp) {
            if(!isLeftTurn(v)) {
                System.out.println("  >> Split Vertex");
                handleSplit(v);
            }
            else {
                System.out.println("  >> Start Vertex");
                handleStart(v);
            }
        }
        // Both edges point downwards (-y)
        else {
            if(!isLeftTurn(v)) {
                System.out.println("  >> Merge Vertex");
                handleMerge(v);
            }
            else {
                System.out.println("  >> End Vertex");
                edges.removeEdge(v);
            }
        }
    }


    private static boolean isLeftTurn(SweepVertex v) {
        Vector2f v1 = v.p.subtract(v.prev.p);
        Vector2f v2 = v.next.p.subtract(v.prev.p);
        return v1.determinant(v2) >= 0;
    }


    private void handleStart(SweepVertex v) {
        //System.out.println("  >> Start Vertex");

        SweepEdge leftEdge = new SweepEdge(v, v.prev);
        leftEdge.lastVertex = v;
        edges.addEdge(leftEdge);

        //SweepEdge rightEdge = new SweepEdge(v, v.next);
        //edges.addEdge(rightEdge);
    }


    private void handleSplit(SweepVertex v) {
        //System.out.println("  >> Split Vertex");

        SweepEdge edge = edges.getEdge(v.p.x, v.p.y);
        assert edge != null;
        assert edge.lastVertex != null;

        edge.lastVertex.connectMonotonePath(v);
        edge.lastVertex = v;

        tryConnectLastMergeVertex(edge, v);

        //SweepEdge leftEdge = new SweepEdge(v, v.next);
        //leftEdge.lastVertex = v;
        //edges.addEdge(leftEdge);

        SweepEdge rightEdge = new SweepEdge(v, v.prev);
        rightEdge.lastVertex = v;
        edges.addEdge(rightEdge);
    }


    private void handleMerge(SweepVertex v) {
        //System.out.println("  >> Merge Vertex");

        SweepEdge edge = edges.removeEdge(v);
        edge.lastVertex = v;

        tryConnectLastMergeVertex(edge, v);
        edge.lastMergeVertex = v;
    }


    private void handleEnd(SweepVertex v) {
        //System.out.println("  >> End Vertex");
        edges.removeEdge(v);
    }


    private void handleContinuation(SweepVertex v) {
        //System.out.println("  >> Continuation Vertex");

        SweepEdge edge = edges.getEdge(v.p.x, v.p.y);
        edge.lastVertex = v;

        if(edge.end == v) {
            SweepVertex next = getContinuationVertex(edge);
            edge.reset(v, next);
        }

        tryConnectLastMergeVertex(edge, v);
    }

    private SweepVertex getContinuationVertex(SweepEdge edge) {
        if(edge.end.prev == edge.start)
            return edge.end.next;
        else {
            assert edge.end.next == edge.start;
            return edge.end.prev;
        }
    }


    private void tryConnectLastMergeVertex(SweepEdge edge, SweepVertex targetVertex) {
        if(edge.lastMergeVertex != null) {
            edge.lastMergeVertex.connectMonotonePath(targetVertex);
            edge.lastMergeVertex = null;
        }
    }
}
