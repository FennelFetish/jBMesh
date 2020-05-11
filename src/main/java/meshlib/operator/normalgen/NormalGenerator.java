package meshlib.operator.normalgen;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.operator.FaceOps;
import meshlib.structure.*;

/**
 * Requires manifold.
 * @author rem
 */
public class NormalGenerator {
    public static interface NormalCalculator {
        void prepare();
        Vector3f getNormal(Face face);
        float calcNormalWeight(Loop loop);
        boolean isCrease(Face face1, Face face2, float creaseAngle);
    }


    private final BMesh bmesh;
    private NormalCalculator normalCalculator;

    private final Vec3Property<Loop> propLoopNormal;

    private float creaseAngle = FastMath.DEG_TO_RAD * 60; // Minimum angle for hard edges

    // TODO: Tip of cone? -> Ignore: Jaimie's tail has a cone and it should be smooth


    public NormalGenerator(BMesh bmesh) {
        this(bmesh, new AngleNormalCalculator(bmesh, new FaceOps(bmesh)));
    }

    public NormalGenerator(BMesh bmesh, NormalCalculator normalCalculator) {
        this.bmesh = bmesh;
        this.normalCalculator = normalCalculator;

        propLoopNormal = Vec3Property.getOrCreate(BMeshProperty.Loop.NORMAL, bmesh.loops());
    }


    public void setCreaseAngle(float angleDeg) {
        this.creaseAngle = FastMath.DEG_TO_RAD * angleDeg;
    }


    public void apply() {
        normalCalculator.prepare();
        NormalAccumulator.Pool accumulators = new NormalAccumulator.Pool();

        for(Vertex vertex : bmesh.vertices()) {
            accumulators.clear();
            populateAccumulators(vertex, accumulators);

            int lastIndex = accumulators.size()-1;
            NormalAccumulator last = accumulators.get(lastIndex);
            if(lastIndex == 0) {
                // All smooth
                applyAccumulator(last, last.firstLoop);
            }
            else {
                // Check if last edge was smooth. If it was, combine the last accumulator with the first one.
                if(last.magnitude > 0) {
                    NormalAccumulator first = accumulators.get(0);
                    first.normal.addLocal(last.normal);
                    first.magnitude += last.magnitude;
                    first.firstLoop = last.firstLoop;
                }

                // Don't process last accumulator. It only serves as a sentinel, so the second last accumulator knows where to stop.
                for(int i=0; i<lastIndex; ++i) {
                    applyAccumulator(accumulators.get(i), accumulators.get(i + 1).firstLoop);
                }
            }
        }
    }


    private void populateAccumulators(Vertex vertex, NormalAccumulator.Pool accumulators) {
        // Find outgoing loop that uses this vertex
        Edge edge = vertex.edge;
        Loop startLoop = edge.loop;
        if(startLoop.vertex != vertex)
            startLoop = startLoop.nextFaceLoop;

        NormalAccumulator acc = accumulators.pushBack(startLoop);
        Loop loop = startLoop;

        // Iterate faces/edges of this Vertex, but without the disk cycle formed by Edges.
        // This gives proper clockwise order, but can only work for manifolds.
        do {
            // Requires manifold (<=2 Loops per radial cycle of Edge)
            if(loop.nextEdgeLoop.nextEdgeLoop != loop)
                break;
            assert loop.vertex == vertex;

            addToAccumulator(acc, loop);

            // If the vertex lies on the border of a hole (equivalent to the border of a surface),
            // we may have to complete traversal in the other direction.
            // TODO: Make this work with bowtie structures?
            if(loop.nextEdgeLoop == loop) {
                otherDirection(vertex, accumulators);
                break;
            }

            // Skip clockwise to next outgoing loop that uses vertex
            Face face1 = loop.face;
            loop = loop.nextEdgeLoop.nextFaceLoop;

            Face face2 = loop.face;
            if(normalCalculator.isCrease(face1, face2, creaseAngle))
                acc = accumulators.pushBack(loop);
        } while(loop != startLoop);
    }


    private void otherDirection(Vertex vertex, NormalAccumulator.Pool accumulators) {
        // Continue from starting point of first accumulator
        NormalAccumulator acc = accumulators.get(0);
        Loop loop = acc.firstLoop;

        while(loop.prevFaceLoop.nextEdgeLoop != loop.prevFaceLoop) {
            // Skip counter-clockwise to previous outgoing loop that uses vertex
            Face face1 = loop.face;
            loop = loop.prevFaceLoop.nextEdgeLoop;

            // Requires manifold (<=2 Loops per radial cycle of Edge)
            if(loop.nextEdgeLoop.nextEdgeLoop != loop)
                break;
            assert loop.vertex == vertex;

            Face face2 = loop.face;
            if(normalCalculator.isCrease(face1, face2, creaseAngle))
                acc = accumulators.pushFront(loop);
            else
                acc.firstLoop = loop;

            addToAccumulator(acc, loop);
        };
    }


    private void addToAccumulator(NormalAccumulator acc, Loop loop) {
        float normalWeight = normalCalculator.calcNormalWeight(loop);
        Vector3f faceNormal = normalCalculator.getNormal(loop.face);

        faceNormal.multLocal(normalWeight);
        acc.normal.addLocal(faceNormal);
        acc.magnitude += normalWeight;
    }


    private void applyAccumulator(NormalAccumulator acc, final Loop endLoop) {
        Loop loop = acc.firstLoop;
        acc.normal.divideLocal(acc.magnitude);

        do {
            propLoopNormal.set(loop, acc.normal);

            if(loop.nextEdgeLoop == loop)
                break;

            loop = loop.nextEdgeLoop.nextFaceLoop;
        } while(loop != endLoop);
    }
}
