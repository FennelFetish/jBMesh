package meshlib.operator;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Edge;
import meshlib.structure.Face;
import meshlib.structure.Vertex;

// Winding order defines what is inside (left) and what is outside (right)
// Straight Skeletons of Simple Polygons https://www.youtube.com/watch?v=sKhnRBAO9sw.
public class PolygonOffset {
    private class CoordinateSystem {
        public final Vector3f n = new Vector3f();
        public final Vector3f p = new Vector3f();
        public final Vector3f x = new Vector3f();
        public final Vector3f y = new Vector3f();

        public CoordinateSystem(List<Vertex> vertices, Face face) {
            faceOps.normal(face, n);
            propPosition.get(vertices.get(0), p);

            propPosition.get(vertices.get(1), x);
            x.subtractLocal(p).normalizeLocal();

            y.set(n).crossLocal(x).normalizeLocal();
        }
    }


    private static class ProjectedVertex {
        public final Vertex vertex;
        public final Vector2f p = new Vector2f();
        public final Vector2f bisector = new Vector2f(); // Length determines speed
        public float edgeLengthChange = 0; // Change amount when shrinking. Outgoing edge, counterclock-wise
        public boolean reflex = false;

        public ProjectedVertex(Vertex vertex) {
            this.vertex = vertex;
        }
    }


    private static final float EPSILON = 0.001f;
    private static final float EPSILON_SQUARED = EPSILON * EPSILON;

    private final BMesh bmesh;
    private FaceOps faceOps;
    private Vec3Property<Vertex> propPosition;

    private float distance = 0.0f; // Positive: Grow polygon, Negative: Shrink


    public PolygonOffset(BMesh bmesh) {
        this.bmesh = bmesh;
        faceOps = new FaceOps(bmesh);
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    /**
     * @param distance Absolute distance in units by which the edges should be moved.<br>
     *                 Positive: Grow face. Negative: Shrink face.
     */
    public void setDistance(float distance) {
        this.distance = distance;
    }


    public void apply(Face face) {
        List<Vertex> vertices = face.getVertices();
        assert vertices.size() >= 3;

        CoordinateSystem coordSys = new CoordinateSystem(vertices, face);
        List<ProjectedVertex> projectedVertices = project(vertices, coordSys);

        while(Math.abs(distance) > EPSILON)
            loop(face, projectedVertices);

        unproject(coordSys, projectedVertices);
    }


    private void loop(Face face, List<ProjectedVertex> projectedVertices) {
        System.out.println("--- loop ---");
        calcBisectors(projectedVertices);
        float edgeCollapse = getFirstEdgeCollapse(projectedVertices);

        float dist;
        if(distance < 0)
            dist = Math.max(-edgeCollapse, distance);
        else
            dist = Math.min(edgeCollapse, distance);

        System.out.println("offsetting by distance: " + dist);
        scale(projectedVertices, dist);
        distance -= dist;
        System.out.println("Distance remaining: " + distance);

        collapseEdges(face, projectedVertices);
        // TODO: Remove collinear edges -> it's the same check as checking for reflex intersections?
    }


    private List<ProjectedVertex> project(List<Vertex> vertices, CoordinateSystem coordSys) {
        List<ProjectedVertex> results = new ArrayList<>(vertices.size());
        Vector3f vertexPos = new Vector3f();

        for(Vertex vertex : vertices) {
            ProjectedVertex projected = new ProjectedVertex(vertex);

            propPosition.get(projected.vertex, vertexPos);
            vertexPos.subtractLocal(coordSys.p);

            projected.p.x = vertexPos.dot(coordSys.x);
            projected.p.y = vertexPos.dot(coordSys.y);
            results.add(projected);
        }

        return results;
    }


    private void calcBisectors(List<ProjectedVertex> projectedVertices) {
        final int numVertices = projectedVertices.size();

        ProjectedVertex last = projectedVertices.get(numVertices-1);
        ProjectedVertex current = projectedVertices.get(0);

        Vector2f vLast = new Vector2f();
        Vector2f vNext = new Vector2f();

        for(int i=0; i<numVertices; ++i) {
            int nextIndex = (i+1) % numVertices;
            ProjectedVertex next = projectedVertices.get(nextIndex);

            vLast.set(last.p).subtractLocal(current.p).normalizeLocal();
            vNext.set(next.p).subtractLocal(current.p).normalizeLocal();

            current.bisector.set(vLast).addLocal(vNext).normalizeLocal();

            // Calc vertex speed, use sine of half the angle
            float sin = vLast.determinant(current.bisector); // cross(vLast, current.bisector).length()
            float speed = 1.0f / sin;
            current.bisector.multLocal(speed);

            // Calc edge length change (same for both adjacent edges)
            float edgeChange = current.bisector.dot(vLast);
            last.edgeLengthChange    += edgeChange;
            current.edgeLengthChange += edgeChange;
            //last.edgeLengthChange    += current.bisector.dot(vLast);
            //current.edgeLengthChange += current.bisector.dot(vNext);

            // Check for reflex vertices (concave corners)
            //current.reflex = vLast.determinant(vNext) > 0.0f; // cross(vLast, vNext).z > 0.0f
            current.reflex = edgeChange > 0.0f;

            // Next iteration
            last = current;
            current = next;
        }
    }


    /**
     * @param projectedVertices
     * @return Distance until edge collapse. Always positive.
     */
    private float getFirstEdgeCollapse(List<ProjectedVertex> projectedVertices) {
        ProjectedVertex current = projectedVertices.get(projectedVertices.size()-1);
        Vector2f edge = new Vector2f();
        float minDistance = Float.POSITIVE_INFINITY;

        for(int i=0; i<projectedVertices.size(); ++i) {
            ProjectedVertex next = projectedVertices.get(i);

            // Look for shrinking edges.
            // 'edgeLengthChange' is the change amount when shrinking.
            // When 'distance' is positive (growing polygon), a positive 'edgeLengthChange' marks a shrinking edge.
            if(sameSign(current.edgeLengthChange, distance)) {
                edge.set(next.p).subtractLocal(current.p);

                float dist = edge.length() / Math.abs(current.edgeLengthChange);
                if(dist < minDistance)
                    minDistance = dist;
            }

            current = next;
        }

        System.out.println("dist until first edge collapse event: " + minDistance);
        return minDistance;
    }


    private void collapseEdges(Face face, List<ProjectedVertex> projectedVertices) {
        CollapseEdge collapseEdge = new CollapseEdge(bmesh);

        // Search for edges with length 0
        ProjectedVertex last = projectedVertices.get(projectedVertices.size()-1);
        for(int i=0; i<projectedVertices.size(); ++i) {
            ProjectedVertex current = projectedVertices.get(i);

            if(current.p.distanceSquared(last.p) >= EPSILON_SQUARED) {
                last = current;
                continue;
            }

            Edge edge = current.vertex.getEdgeTo(last.vertex);
            if(edge.vertex0 == current.vertex) {
                projectedVertices.remove(last);
                last = current;
            }
            else {
                assert edge.vertex0 == last.vertex;
                projectedVertices.remove(current);
            }

            collapseEdge.apply(edge);
            System.out.println("edge collapsed");
        }

        // Delete face if num vertices < 3
        if(projectedVertices.size() < 3) {
            System.out.println("face collapsed to vertex");
            //bmesh.removeFace(face); // TODO: Should collapse face into 1 single vertex
        }

        //System.out.println("Face vertices: " + face.getVertices().size());
    }


    private boolean sameSign(float a, float b) {
        return (a >= 0) ^ (b < 0);
    }


    private void scale(List<ProjectedVertex> projectedVertices, float dist) {
        Vector2f dir = new Vector2f();

        for(ProjectedVertex proj : projectedVertices) {
            dir.set(proj.bisector).multLocal(dist);
            proj.p.addLocal(dir);
        }
    }


    private void unproject(CoordinateSystem coordSys, List<ProjectedVertex> projectedVertices) {
        Vector3f px = new Vector3f();
        Vector3f py = new Vector3f();
        Vector3f result = new Vector3f();

        for(ProjectedVertex proj : projectedVertices) {
            px.set(coordSys.x).multLocal(proj.p.x);
            py.set(coordSys.y).multLocal(proj.p.y);

            result.set(coordSys.p).addLocal(px).addLocal(py);
            propPosition.set(proj.vertex, result);
        }
    }
}
