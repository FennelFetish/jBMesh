package ch.alchemists.jbmesh.operator.normalgen;

import ch.alchemists.jbmesh.data.property.BooleanAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.operator.FaceOps;
import ch.alchemists.jbmesh.structure.*;
import com.jme3.math.Vector3f;

public class AngleNormalCalculator implements NormalGenerator.NormalCalculator {
    private static final String ATTRIBUTE_EDGE_CREASE = "AngleNormalCalculator_EdgeCrease";

    protected FaceOps faceOps;

    protected Vec3Attribute<Vertex> positions;
    protected BooleanAttribute<Edge> edgeCrease;
    //protected final Map<Edge, Boolean> edgeCreases = new HashMap<>();

    protected final transient Vector3f tempV1 = new Vector3f();
    protected final transient Vector3f tempV2 = new Vector3f();


    public AngleNormalCalculator() {
        // TODO: Don't add attributes but use maps when recalculating normals for only a selection?

        /*
        With face normals:
        Norm Gen                                               4629.6860000         %     92.5937199  119.623899  50
        Norm Gen                                               4586.8600000         %     91.7371999  119.487299  50

        With edgeCrease attribute:
        Norm Gen                                               3569.6097000         %     71.3921940  107.608600  50
        Norm Gen                                               3640.2288999         %     72.8045779  108.164400  50
        Norm Gen                                               3605.3930000         %     72.1078600  108.670200  50

        With hash map for edge creases:
        Norm Gen                                               3904.8577999         %     78.0971560  135.635900  50
        Norm Gen                                               3951.4064000         %     79.0281280  136.145400  50
        Norm Gen                                               3940.2990000         %     78.8059800  133.910200  50
        */
    }


    @Override
    public void prepare(BMesh bmesh, float creaseAngle) {
        edgeCrease = BooleanAttribute.getOrCreate(ATTRIBUTE_EDGE_CREASE, bmesh.edges());
        positions = Vec3Attribute.get(Vertex.Position, bmesh.vertices());
        faceOps = new FaceOps(bmesh);

        for(Edge edge : bmesh.edges()) {
            if(edge.loop == null)
                continue;

            Face face1 = edge.loop.face;
            Face face2 = edge.loop.nextEdgeLoop.face;

            faceOps.normal(face1, tempV1);
            faceOps.normal(face2, tempV2);

            /*if(tempV1.x == 0 && tempV1.y == 0 && tempV1.z == 0) {
                attrEdgeCrease.set(edge, true);
                continue;
            }
            if(tempV2.x == 0 && tempV2.y == 0 && tempV2.z == 0) {
                attrEdgeCrease.set(edge, true);
                continue;
            }*/

            boolean crease = tempV1.angleBetween(tempV2) >= creaseAngle;
            edgeCrease.set(edge, crease);
        }
    }

    @Override
    public void cleanup(BMesh bmesh) {
        bmesh.edges().removeAttribute(edgeCrease);
        edgeCrease = null;
        positions = null;
        faceOps = null;
        //edgeCreases.clear();
    }


    @Override
    public void getWeightedNormal(Loop loop, Vector3f store) {
        // Use normal of triangle because faces are not always planar (?). Requires convex polygon.
        // -> TODO: Compare with face normal to make it work for concave (flip normal in this case)? -> Make separate NormalCalculator for that

        Vertex vertex = loop.vertex;
        Vertex vNext = loop.nextFaceLoop.vertex;
        Vertex vPrev = loop.prevFaceLoop.vertex;

        positions.get(vertex, tempV1);
        tempV2.set(tempV1);
        positions.subtractLocal(tempV1, vNext);
        positions.subtractLocal(tempV2, vPrev);

        store.set(tempV1).crossLocal(tempV2); // Don't normalize cross product so it includes triangle area

        // Degenerate faces? (happens for example after cutting)
        if(store.x == 0 && store.y == 0 && store.z == 0) {
            faceOps.normal(loop.face, store);
            return;
        }

        tempV1.normalizeLocal();
        tempV2.normalizeLocal();
        float angle = tempV1.angleBetween(tempV2);
        store.multLocal(angle);
    }


    @Override
    public boolean isCrease(Edge edge, Face face1, Face face2, float creaseAngle) {
        return edgeCrease.get(edge);

        /*Boolean crease = edgeCreases.get(edge);
        if(crease != null)
            return crease;

        faceOps.normal(face1, tempV1);
        faceOps.normal(face2, tempV2);
        boolean b = tempV1.angleBetween(tempV2) >= creaseAngle;
        edgeCreases.put(edge, b);
        return b;*/
    }
}
