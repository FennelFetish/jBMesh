package meshlib.operator;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
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
        public float edgeLengthChange = 0; // Outgoing edge, counterclock-wise
        public boolean reflex = false;

        public ProjectedVertex(Vertex vertex) {
            this.vertex = vertex;
        }
    }


    private FaceOps faceOps;
    private Vec3Property<Vertex> propPosition;

    private float distance = 1.0f; // Positive: Grow polygon, Negative: Shrink


    public PolygonOffset(BMesh bmesh) {
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

        calcBisectors(projectedVertices);
        float maxDistance = calcEdgeLengthChange(projectedVertices);

        // TODO: Test - this seems wrong. Maximum can be positive or negative. We're interested in the same sign as 'distance' (?).
        float dist = distance;
        if(maxDistance < 0) {
            dist = Math.max(maxDistance, distance);
        }

        System.out.println("offsetting by distance: " + dist);
        scale(projectedVertices, dist);
        unproject(coordSys, projectedVertices);
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
            last.edgeLengthChange    += current.bisector.dot(vLast);
            current.edgeLengthChange += current.bisector.dot(vNext);

            // Check for reflex vertices (concave corners)
            current.reflex = vLast.determinant(vNext) > 0.0f; // vLast.cross(vNext).z > 0.0f

            // Next iteration
            last = current;
            current = next;
        }
    }


    private float calcEdgeLengthChange(List<ProjectedVertex> projectedVertices) {
        ProjectedVertex current = projectedVertices.get(projectedVertices.size()-1);
        Vector2f edge = new Vector2f();

        float maxDistance = Float.NEGATIVE_INFINITY;

        for(int i=0; i<projectedVertices.size(); ++i) {
            ProjectedVertex next = projectedVertices.get(i);

            edge.set(next.p).subtractLocal(current.p);
            float edgeLength = edge.length();
            edge.normalizeLocal();

            //current.edgeLengthChange = current.bisector.dot(edge);
            //current.edgeLengthChange -= next.bisector.dot(edge);

            float dist = edgeLength / current.edgeLengthChange;
            if(dist > maxDistance)
                maxDistance = dist;

            System.out.println("EdgeLengthChange " + i + ": " + current.edgeLengthChange);
            System.out.println("dist: " + dist);

            current = next;
        }

        System.out.println("dist until first edge collapse event: " + maxDistance);
        return maxDistance;
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




    /*public void apply(Face face) {
        List<Vertex> vertices = face.getVertices();
        assert vertices.size() >= 3;

        // Calc bisectors
        Vector3f v0 = new Vector3f(); // last
        Vector3f v1 = new Vector3f(); // next

        Vector3f[] bisectors = new Vector3f[vertices.size()];
        for(int i=0; i< bisectors.length; ++i)
            bisectors[i] = new Vector3f();

        Vertex lastVertex = vertices.get(vertices.size()-1);
        Vertex currentVertex = vertices.get(0);

        for(int i=0; i<bisectors.length; ++i) {
            int nextIndex = (i+1) % vertices.size();
            Vertex nextVertex = vertices.get(nextIndex);

            propPosition.get(lastVertex, v0);
            propPosition.get(currentVertex, bisectors[i]);
            propPosition.get(nextVertex, v1);

            v0.subtractLocal(bisectors[i]).normalizeLocal();
            v1.subtractLocal(bisectors[i]).normalizeLocal();
            bisectors[i].set(v0).addLocal(v1).normalizeLocal();

            // TODO: Check for reflex vertices (concave corners)

            // Calc speed, use sine of half the angle
            float sin = v0.crossLocal(bisectors[i]).length();
            float speed = 1.0f / sin;
            bisectors[i].multLocal(speed);

            lastVertex = currentVertex;
            currentVertex = nextVertex;
        }


        debugApply(vertices, bisectors);
    }*/


    /*private void debugApply(List<Vertex> vertices, Vector3f[] bisectors) {
        Vector3f temp = new Vector3f();
        for(int i=0; i< bisectors.length; ++i) {
            bisectors[i].multLocal(distance);

            propPosition.get(vertices.get(i), temp);
            temp.subtractLocal(bisectors[i]);

            propPosition.set(vertices.get(i), temp);
        }
    }*/

    /*private List<Vector2f> projectOntoPlane(Face face) {
        List<Vertex> vertices = face.getVertices();
        assert vertices.size() >= 3;

        // Create coordinate system of plane
        Vector3f p0 = propPosition.get(vertices.get(0));
        Vector3f n = faceOps.normal(face);

        Vector3f x = propPosition.get(vertices.get(1));
        x.subtractLocal(p0).normalizeLocal();

        Vector3f y = n.cross(x).normalizeLocal();

        // Project vertices onto plane
        List<Vector2f> projected = new ArrayList<>(vertices.size());
        for(int i=0; i<vertices.size(); ++i) {
            Vector3f v = propPosition.get(vertices.get(i));
            v.subtractLocal(p0);

            projected.add(new Vector2f(v.dot(x), v.dot(y)));
        }

        return projected;
    }*/
}
