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

        System.out.println("Vertices:");
        for(int i=0; i<sweepVertices.size(); ++i) {
            System.out.println("  Vertex[" + i + "]: " + coordSys.unproject(sweepVertices.get(i).p) );
        }

        process();
        sweepVertices.clear();
    }


    private void createSweepVertices(List<Vertex> vertices, PlanarCoordinateSystem coordSys) {
        sweepVertices.ensureCapacity(vertices.size());

        SweepVertex first = createSweepVertex(vertices.get(0), 0, coordSys);
        SweepVertex prev = first;
        sweepVertices.add(first);

        for(int i=1; i<vertices.size(); ++i) {
            SweepVertex current = createSweepVertex(vertices.get(i), i, coordSys);
            if(current.p.isSimilar(prev.p, 0.0001f))
                continue;

            sweepVertices.add(current);

            current.prev = prev;
            prev.next = current;

            prev = current;
        }

        first.prev = prev;
        prev.next = first;

        sweepVertices.sort(null);
    }


    private SweepVertex createSweepVertex(Vertex vertex, int index, PlanarCoordinateSystem coordSys) {
        SweepVertex sweepVertex = new SweepVertex(index);
        coordSys.project(propPosition.get(vertex), sweepVertex.p);
        return sweepVertex;
    }


    private void process() {
        EdgeSet edges = new EdgeSet();

        for(int i=0; i<sweepVertices.size(); ++i) {
            SweepVertex v = sweepVertices.get(i);
            if(v.p.y > yLimit)
                break;

            System.out.println("))))))))))))))) handleSweepVertex " + i + ": " + v.p);
            handleSweepVertex(v, edges);
            edges.printEdges();
        }

        edges.debug(yLimit);
    }


    private void handleSweepVertex(SweepVertex v, EdgeSet edges) {
        boolean prevUp = (v.p.y < v.prev.p.y);
        boolean nextUp = (v.p.y <= v.next.p.y);

        System.out.println("prevUp: " + prevUp + ", nextUp: " + nextUp);

        // One edge points upwards, one points downwards
        if(prevUp != nextUp) {
            System.out.println("  >> Continuation");
            handleContinuation(v, edges);
            return;
        }

        final boolean inside = isInside(v);

        // Both edges point upwards (+y)
        if(prevUp) {
            if(inside) {
                System.out.println("  >> Split Vertex");
                handleSplit(v, edges);
            }
            else {
                System.out.println("  >> Start Vertex");
                handleStart(v, edges);
            }
        }
        // Both edges point downwards (-y)
        else {
            if(inside) {
                System.out.println("  >> Merge Vertex");
                handleMerge(v, edges);
            }
            else {
                System.out.println("  >> End Vertex");
                edges.removeEdge(v);
            }
        }
    }


    private boolean isInside(SweepVertex v) {
        // Check if polygon makes right turn at 'v'
        Vector2f v1 = v.p.subtract(v.prev.p);
        Vector2f v2 = v.next.p.subtract(v.prev.p);
        float det = v1.determinant(v2);
        return det <= 0;
    }


    private void handleStart(SweepVertex v, EdgeSet edges) {
        SweepEdge leftEdge = new SweepEdge(v, v.prev);
        leftEdge.lastVertex = v;
        edges.addEdge(leftEdge);

        //SweepEdge rightEdge = new SweepEdge(v, v.next);
        //edges.addEdge(rightEdge);
    }


    private void handleSplit(SweepVertex v, EdgeSet edges) {
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


    private void handleMerge(SweepVertex v, EdgeSet edges) {
        SweepEdge edge = edges.removeEdge(v);
        edge.lastVertex = v;

        tryConnectLastMergeVertex(edge, v);

        /*if(v.p.y == v.prev.p.y)
            edge.lastMergeVertex = v.prev;
        else*/
            edge.lastMergeVertex = v;
    }


    private void handleContinuation(SweepVertex v, EdgeSet edges) {
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
