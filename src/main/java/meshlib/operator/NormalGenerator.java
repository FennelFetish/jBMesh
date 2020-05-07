package meshlib.operator;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import meshlib.data.BMeshProperty;
import meshlib.data.property.BooleanProperty;
import meshlib.data.property.FloatProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Edge;
import meshlib.structure.Face;
import meshlib.structure.Loop;
import meshlib.structure.Vertex;

/**
 * Requires manifold.
 * @author rem
 */
public class NormalGenerator {
    private static final String PROPERTY_EDGE_CREASE = "NormalGenerator_EdgeCrease";

    private final BMesh bmesh;

    private NormalCalculator normalCalculator;
    private final FaceOps faceOps;

    private final Vec3Property<Loop> propLoopNormal;
    private final BooleanProperty<Edge> propEdgeCrease = null;

    private float creaseAngle = FastMath.DEG_TO_RAD * 60; // Minimum angle for hard edges

    // TODO: Tip of cone?


    public NormalGenerator(BMesh bmesh) {
        this.bmesh = bmesh;
        this.faceOps = new FaceOps(bmesh);

        normalCalculator = new AngleAreaNormalCalculator(bmesh, faceOps);

        Vec3Property<Loop> propLoopNormal = Vec3Property.get(BMeshProperty.Loop.NORMAL, bmesh.loops());
        if(propLoopNormal == null) {
            propLoopNormal = new Vec3Property<>(BMeshProperty.Loop.NORMAL);
            bmesh.loops().addProperty(propLoopNormal);
        }

        /*BooleanProperty<Edge> propCrease = BooleanProperty.get(PROPERTY_EDGE_CREASE, bmesh.edges());
        if(propCrease == null) {
            propCrease = new BooleanProperty<>(PROPERTY_EDGE_CREASE);
            bmesh.edges().addProperty(propCrease);
        }*/

        this.propLoopNormal = propLoopNormal;
        //this.propEdgeCrease = propCrease;
    }


    public void setCreaseAngle(float angleDeg) {
        this.creaseAngle = FastMath.DEG_TO_RAD * angleDeg;
    }


    // Preparing these values doesn't make it much faster, and somehow wrong?
    /*private void prepareCreaseFlags() {
        Vector3f norm1 = new Vector3f();
        Vector3f norm2 = new Vector3f();

        for(Edge edge : bmesh.edges()) {
            Face face1 = edge.loop.face;
            Face face2 = edge.loop.nextEdgeLoop.face;
            assert face1 != face2;

            propFaceNormal.get(face1, norm1);
            propFaceNormal.get(face2, norm2);

            boolean crease = norm1.angleBetween(norm2) > creaseAngle;
            propEdgeCrease.set(edge, crease);
        }
    }*/


    private static class NormalAccumulator {
        public final Vector3f normal = new Vector3f();
        public float magnitude = 0.0f;
        public Loop firstLoop;

        public NormalAccumulator(Loop loop) {
            firstLoop = loop;
        }
    }


    // TODO: Wrong smoothed normals
    public void apply() {
        normalCalculator.prepare();
        //prepareCreaseFlags();

        ArrayList<NormalAccumulator> accumulators = new ArrayList<>(6);

        for(Vertex vertex : bmesh.vertices()) {
            populateAccumulators(vertex, accumulators);

            int lastIndex = accumulators.size()-1;
            NormalAccumulator last = accumulators.get(lastIndex);
            if(lastIndex == 0) {
                // All smooth
                applyAccumulator(last, last.firstLoop);
            }
            else {
                // Check if last edge was smooth
                if(last.magnitude > 0) {
                    NormalAccumulator first = accumulators.get(0);
                    first.normal.addLocal(last.normal);
                    first.magnitude += last.magnitude;
                    first.firstLoop = last.firstLoop;
                }

                for(int i=0; i<lastIndex; ++i) {
                    applyAccumulator(accumulators.get(i), accumulators.get(i+1).firstLoop);
                }
            }
        }
    }


    private void populateAccumulators(Vertex vertex, ArrayList<NormalAccumulator> accumulators) {
        // Find outgoing loop that uses this vertex
        Edge edge = vertex.edge;
        Loop startLoop = edge.loop;
        if(startLoop.vertex != vertex)
            startLoop = startLoop.nextFaceLoop;
        assert startLoop.vertex == vertex;

        NormalAccumulator acc = new NormalAccumulator(startLoop);
        accumulators.clear();
        accumulators.add(acc);

        Loop loop = startLoop;
        do {
            Face face1 = loop.face;
            float normalWeight = normalCalculator.calcNormalWeight(loop);
            Vector3f faceNormal = normalCalculator.getNormal(face1);
            faceNormal.multLocal(normalWeight);
            acc.normal.addLocal(faceNormal);
            acc.magnitude += normalWeight;

            // Hit a dead end, TODO: Make this work -> complete traversal in the other direction, bowties need to iterate disk cycle
            if(loop.nextEdgeLoop == loop) {
                System.out.println("dead end");
                break;
            }

            // Skip clockwise to next outgoing loop that uses vertex
            loop = loop.nextEdgeLoop;
            loop = loop.nextFaceLoop;
            assert loop.vertex == vertex;

            Face face2 = loop.face;
            if(normalCalculator.isCrease(face1, face2, creaseAngle)) {
                acc = new NormalAccumulator(loop);
                accumulators.add(acc);
            }
        } while(loop != startLoop);
    }


    private void applyAccumulator(NormalAccumulator acc, final Loop endLoop) {
        Loop loop = acc.firstLoop;
        //System.out.println("magnitude: " + acc.magnitude);
        acc.normal.divideLocal(acc.magnitude);
        //acc.normal.divideLocal(acc.magnitude).normalizeLocal();
        //acc.normal.normalizeLocal();

        do {
            propLoopNormal.set(loop, acc.normal);
            loop = loop.nextEdgeLoop;
            loop = loop.nextFaceLoop;
        } while(loop != endLoop);
    }


    public static interface NormalCalculator {
        void prepare();
        Vector3f getNormal(Face face);
        float calcNormalWeight(Loop loop);
        boolean isCrease(Face face1, Face face2, float creaseAngle);
    }


    public static class AngleAreaNormalCalculator implements NormalCalculator {
        private static final String PROPERTY_FACE_AREA = "NormalGenerator_FaceArea";

        private final BMesh bmesh;
        private final FaceOps faceOps;

        private final Vec3Property<Vertex> propPosition;
        private final Vec3Property<Face> propFaceNormal;
        private final FloatProperty<Face> propFaceArea;

        private final transient Vector3f tempV1 = new Vector3f();
        private final transient Vector3f tempV2 = new Vector3f();

        public AngleAreaNormalCalculator(BMesh bmesh, FaceOps faceOps) {
            this.bmesh = bmesh;
            this.faceOps = faceOps;

            propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());

            Vec3Property<Face> propFaceNormal = Vec3Property.get(BMeshProperty.Face.NORMAL, bmesh.faces());
            if(propFaceNormal == null) {
                propFaceNormal = new Vec3Property<>(BMeshProperty.Face.NORMAL);
                bmesh.faces().addProperty(propFaceNormal);
                // Delete after apply?
            }
            this.propFaceNormal = propFaceNormal;

            FloatProperty<Face> propFaceArea = FloatProperty.get(PROPERTY_FACE_AREA, bmesh.faces());
            if(propFaceArea == null) {
                propFaceArea = new FloatProperty<>(PROPERTY_FACE_AREA);
                bmesh.faces().addProperty(propFaceArea);
            }
            this.propFaceArea = propFaceArea;
        }

        @Override
        public void prepare() {
            for(Face face : bmesh.faces()) {
                faceOps.normal(face, tempV1);
                propFaceNormal.set(face, tempV1);

                float area = faceOps.area(face, tempV1);
                propFaceArea.set(face, area);
            }
        }

        @Override
        public Vector3f getNormal(Face face) {
            propFaceNormal.get(face, tempV1);
            return tempV1;
        }

        @Override
        public float calcNormalWeight(Loop loop) {
            // Angle of side
            Vertex vertex = loop.vertex;
            Vertex vNext = loop.nextFaceLoop.vertex;
            Vertex vPrev = loop.prevFaceLoop.vertex;
            propPosition.get(vertex, tempV1);
            tempV2.set(tempV1);
            propPosition.subtract(vNext, tempV1);
            propPosition.subtract(vPrev, tempV2);

            tempV1.normalizeLocal();
            tempV2.normalizeLocal();
            float angle = tempV1.angleBetween(tempV2);

            // Area of whole polygon
            float area = 1.0f;
            area = propFaceArea.get(loop.face);
            // TODO: Try triangle area of this polyong 'ear'?
            //area = tempNorm1.cross(tempNorm2).length() * 0.5f;

            return angle * area;
        }

        @Override
        public boolean isCrease(Face face1, Face face2, float creaseAngle) {
            propFaceNormal.get(face1, tempV1);
            propFaceNormal.get(face2, tempV2);
            return tempV1.angleBetween(tempV2) > creaseAngle;
        }
    }
}
