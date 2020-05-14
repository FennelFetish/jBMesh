package meshlib.operator.bool;

import com.jme3.math.Vector3f;
import java.util.*;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.operator.EdgeOps;
import meshlib.structure.*;

public class Subtract {
    public static interface DistanceFunc {
        float dist(Vector3f p);
    }


    private static class FaceSplitInfo {
        public Loop start = null;
        public Loop end = null;

        public Edge startEdge = null;
        public Edge endEdge = null;
    }


    private final BMesh bmesh;
    private final Vec3Property<Vertex> propPosition;
    //private final Vector3f temp = new Vector3f();

    private DistanceFunc dfunc;

    private int edgeSubdivisions = 2; // No -> Make a max deviation parameter


    public Subtract(BMesh bmesh, DistanceFunc dfunc) {
        this.bmesh = bmesh;
        this.dfunc = dfunc;
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    public void apply(List<Face> faces) {
        // Accumulate elements: Inside, Intersecting
        List<FaceSplitInfo> faceSplits = new ArrayList<>();
        List<Face> insideFaces = new ArrayList<>();

        Vector3f p1 = new Vector3f();
        Vector3f p2 = new Vector3f();

        for(Face face : faces) {
            boolean hasInside = false;
            boolean hasOutside = false;

            FaceSplitInfo splitInfo = null;
            Loop lastEndLoop = null;
            for(Loop loop : face.loops()) {
                // Check for intersection of edge with the border of distance function
                propPosition.get(loop.vertex, p1);
                float dist1 = dfunc.dist(p1);

                propPosition.get(loop.nextFaceLoop.vertex, p2);
                float dist2 = dfunc.dist(p2);

                // Loop pointing inwards
                if(dist1 > 0 && dist2 <= 0) {
                    hasInside = true;
                    hasOutside = true;

                    assert splitInfo == null;
                    splitInfo = new FaceSplitInfo();
                    splitInfo.start = loop;
                    splitInfo.startEdge = loop.edge;
                    faceSplits.add(splitInfo);
                }
                // Loop pointing outwards
                else if(dist1 <= 0 && dist2 > 0) {
                    hasInside = true;
                    hasOutside = true;

                    if(splitInfo != null) {
                        splitInfo.end = loop;
                        splitInfo.endEdge = loop.edge;
                        splitInfo = null;
                    }
                    else {
                        assert lastEndLoop == null;
                        lastEndLoop = loop; // Put into last FaceSplitInfo
                    }
                }
                // No intersection, both points on the same side, check if both are inside
                else if(dist1 <= 0)
                    hasInside = true;
                else
                    hasOutside = true; // Both outside
            }

            if(lastEndLoop != null) {
                assert splitInfo != null;
                assert splitInfo.end == null;
                splitInfo.end = lastEndLoop;
                splitInfo.endEdge = lastEndLoop.edge;
            }

            if(hasInside) {
                if(!hasOutside) {
                    insideFaces.add(face);
                    //face.loops().forEach(l -> insideVertices.add(l.vertex));
                }
            }
            else {
                // TODO: Check for intersection with (large) polygons/edges that have no vertices inside
            }
        }

        // Split all edges which are references by FaceSplitInfos, but each edge only once, remember resulting vertex
        // Split it so all Loops in FaceSplitInfos will be on the inside
        Map<Edge, Vertex> splitEdges = new HashMap<>();
        Set<Vertex> splitResultVertices = new HashSet<>();

        for(FaceSplitInfo splitInfo : faceSplits) {
            Vertex vStart = splitEdges.get(splitInfo.startEdge);
            if(vStart == null) {
                propPosition.get(splitInfo.start.vertex, p1); // Outside
                propPosition.get(splitInfo.start.nextFaceLoop.vertex, p2); // Inside

                vStart = bmesh.splitEdge(splitInfo.startEdge);
                moveToBorder(vStart, p2, p1);
                splitEdges.put(splitInfo.startEdge, vStart);

                splitResultVertices.add(vStart);
            }

            Vertex vEnd = splitEdges.get(splitInfo.endEdge);
            if(vEnd == null) {
                propPosition.get(splitInfo.end.vertex, p1); // Inside
                propPosition.get(splitInfo.end.nextFaceLoop.vertex, p2); // Outside

                vEnd = bmesh.splitEdge(splitInfo.endEdge);
                moveToBorder(vEnd, p1, p2);
                splitEdges.put(splitInfo.endEdge, vEnd);

                splitResultVertices.add(vEnd);
            }

            Face face = splitInfo.start.face;
            Edge edge = bmesh.splitFace(face, vStart, vEnd);

            insideFaces.add(face);

            // Subdivide each new edge and approximate distance function, planar to face -> calc 2D normal on distance field?

        }


        // Process inside, TODO: Return to user
        Set<Vertex> insideVertices = new HashSet<>();
        for(Face face : insideFaces) {
            face.loops().forEach(l -> insideVertices.add(l.vertex));
        }

        for(Vertex v : insideVertices) {
            if(!splitResultVertices.contains(v))
                bmesh.removeVertex(v);
        }
    }


    // Move 1D
    private void moveToBorder(Vertex v, Vector3f inside, Vector3f outside) {
        final float epsilon = 0.0001f; // 0.001
        final int maxSteps = 200; // 12
        final float stepShrink = 0.5f; // 0.5 0.66f
        final float minStepSize = 0.15f; // 0.1f

        outside.subtractLocal(inside).normalizeLocal().negateLocal(); // Points from out to in, inwards

        float dist = dfunc.dist(inside); // On the inside: dist() <= 0
        float stepSize = 0.5f;

        for(int i=0; Math.abs(dist) > epsilon && i<maxSteps; ++i) {
            float t = dist * stepSize;
            stepSize = Math.max(stepSize * stepShrink, minStepSize);

            inside.addLocal(outside.x * t, outside.y * t, outside.z * t);
            dist = dfunc.dist(inside);
        }

        propPosition.set(v, inside);
    }



    // Move 2D
    private void moveOnPlane() {

    }



    public static class Plane implements DistanceFunc {
        private final Vector3f p = new Vector3f();
        private final Vector3f n = new Vector3f();
        private final Vector3f proj = new Vector3f();

        public Plane(Vector3f p, Vector3f n) {
            this.p.set(p);
            this.n.set(n).normalizeLocal();
        }

        @Override
        public float dist(Vector3f p) {
            proj.set(p).subtractLocal(this.p);
            return -proj.dot(n); // "Inside" is in direction of normal
        }
    }


    public static class Sphere implements DistanceFunc {
        private final Vector3f center = new Vector3f();
        private final float radius;
        private final Vector3f pClone = new Vector3f();

        public Sphere(Vector3f center, float radius) {
            this.center.set(center);
            this.radius = radius;
        }

        @Override
        public float dist(Vector3f p) {
            //p = pClone.set(p).multLocal(1, 7.0f, 1);
            return center.distance(p) - radius;
        }
    }
}
