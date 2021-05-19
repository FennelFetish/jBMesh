package ch.alchemists.jbmesh.operator.bool;

import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.operator.meshgen.DistanceFunction;
import ch.alchemists.jbmesh.structure.*;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Create vertices inside intersecting faces when distance function makes a sharp corner? (Box)
public abstract class Cut {
    private static class FaceSplitInfo {
        public Loop start = null;
        public Loop end = null;

        public Edge startEdge = null;
        public Edge endEdge = null;
    }


    protected final BMesh bmesh;
    protected final Vec3Property<Vertex> propPosition;

    protected DistanceFunction dfunc;


    public Cut(BMesh bmesh, DistanceFunction dfunc) {
        this.bmesh = bmesh;
        this.dfunc = dfunc;
        propPosition = Vec3Property.get(Vertex.Position, bmesh.vertices());
    }


    protected void accumulateInside(Face face) {}
    protected void accumulateOutside(Face face) {}
    protected void accumulateIntersect(Face face) {}

    protected void accumulateCutVertex(Vertex vertex) {}
    protected void accumulateCutEdge(Edge edge) {}

    protected abstract void prepareCut();
    protected abstract void processCut();


    public void apply(List<Face> faces) {
        prepareCut();

        // Accumulate elements: Inside, Intersecting
        List<FaceSplitInfo> faceSplits = findEdgeCuts(faces);

        // Split all edges which are references by FaceSplitInfos, but each edge only once, remember resulting vertex
        // Split it so all Loops in FaceSplitInfos will be on the inside
        Map<Edge, Vertex> splitEdges = new HashMap<>();

        Vector3f p1 = new Vector3f();
        Vector3f p2 = new Vector3f();

        for(FaceSplitInfo splitInfo : faceSplits) {
            Vertex vStart = splitEdges.get(splitInfo.startEdge);
            if(vStart == null) {
                propPosition.get(splitInfo.start.vertex, p1); // Outside
                propPosition.get(splitInfo.start.nextFaceLoop.vertex, p2); // Inside

                vStart = bmesh.splitEdge(splitInfo.startEdge);
                moveToBorder(vStart, p2, p1);
                splitEdges.put(splitInfo.startEdge, vStart);

                accumulateCutVertex(vStart);
            }

            Vertex vEnd = splitEdges.get(splitInfo.endEdge);
            if(vEnd == null) {
                propPosition.get(splitInfo.end.vertex, p1); // Inside
                propPosition.get(splitInfo.end.nextFaceLoop.vertex, p2); // Outside

                vEnd = bmesh.splitEdge(splitInfo.endEdge);
                moveToBorder(vEnd, p1, p2);
                splitEdges.put(splitInfo.endEdge, vEnd);

                accumulateCutVertex(vEnd);
            }

            Face face = splitInfo.start.face;
            Edge edge = bmesh.splitFace(face, vStart, vEnd);
            accumulateCutEdge(edge);
            accumulateInside(face);

            Face outsideFace = edge.loop.nextEdgeLoop.face;
            if(outsideFace == face)
                outsideFace = edge.loop.face;
            accumulateOutside(outsideFace);
        }

        // TODO: Check for degenerate faces & edges

        processCut();
    }


    private List<FaceSplitInfo> findEdgeCuts(List<Face> faces) {
        List<FaceSplitInfo> faceSplits = new ArrayList<>();
        Vector3f p1 = new Vector3f();
        Vector3f p2 = new Vector3f();

        for(Face face : faces) {
            boolean hasInside = false;
            boolean hasOutside = false;

            FaceSplitInfo splitInfo = null;
            Loop lastEndLoop = null;
            for(Loop loop : face.loops()) {
                // TODO: Instead of doing intersection test for every loop, do it for each edge once -> save to HashMap?

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
                if(!hasOutside)
                    accumulateInside(face);
                else
                    accumulateIntersect(face);
            }
            else {
                accumulateOutside(face);
                // TODO: Check for intersection with (large) polygons/edges that have no vertices inside
                //       Can't represent holes in faces -> need to add 2 cuts to hole
            }
        }

        return faceSplits;
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

    // Does less but is not really faster since it's not the bottleneck
    /*private void moveToBorder(Vertex v, Vector3f inside, Vector3f outside) {
        float dIn  = dfunc.dist(inside);
        float dOut = dfunc.dist(outside);

        // Linear interpolation
        float t = dIn / (dIn - dOut);

        outside.subtractLocal(inside);
        outside.multLocal(t);
        outside.addLocal(inside);

        propPosition.set(v, outside);
    }*/



    // Move 2D
    private void moveOnPlane() {

    }
}
