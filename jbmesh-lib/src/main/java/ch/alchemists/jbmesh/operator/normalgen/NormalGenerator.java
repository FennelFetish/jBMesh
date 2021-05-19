package ch.alchemists.jbmesh.operator.normalgen;

import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.structure.*;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

/**
 * Requires manifold.
 * @author rem
 */
public class NormalGenerator {
    public static interface NormalCalculator {
        /**
         * Called once before processing the vertices.
         */
        void prepare(BMesh bmesh, float creaseAngle);

        /**
         * Called once after processing.
         * @param bmesh
         */
        default void cleanup(BMesh bmesh) {}

        /**
         * @param loop
         * @param store
         * @return Weight of normal.
         */
        void getWeightedNormal(Loop loop, Vector3f store);

        /**
         * @param edge
         * @param face1
         * @param face2
         * @param creaseAngle
         * @return Whether there should be a crease between the two faces.
         */
        boolean isCrease(Edge edge, Face face1, Face face2, float creaseAngle);
    }


    private final BMesh bmesh;
    private NormalCalculator normalCalculator;

    private final Vec3Property<Loop> propLoopNormal;
    private final Vector3f tempNormal = new Vector3f();

    private float creaseAngle = 0.0f; // Minimum angle for hard edges

    // TODO: Tip of cone? -> Ignore: Jaimie's tail has a cone and it should be smooth
    // TODO: Support hard edges?


    public NormalGenerator(BMesh bmesh) {
        this(bmesh, 60.0f, new AngleNormalCalculator());
    }

    public NormalGenerator(BMesh bmesh, float creaseAngle) {
        this(bmesh, creaseAngle, new AngleNormalCalculator());
    }

    public NormalGenerator(BMesh bmesh, float creaseAngle, NormalCalculator normalCalculator) {
        this.bmesh = bmesh;
        this.normalCalculator = normalCalculator;
        setCreaseAngle(creaseAngle);

        propLoopNormal = Vec3Property.getOrCreate(Loop.Normal, bmesh.loops());
    }


    public void setCreaseAngle(float angleDeg) {
        this.creaseAngle = FastMath.DEG_TO_RAD * angleDeg;
    }


    // TODO: Apply to selection (Faces & Vertices)
    public void apply() {
        normalCalculator.prepare(bmesh, creaseAngle);
        NormalAccumulator.Pool accumulators = new NormalAccumulator.Pool();

        for(Vertex vertex : bmesh.vertices()) {
            Edge startEdge = getEdgeWithLoop(vertex);
            if(startEdge == null)
                continue;

            accumulators.clear();
            populateAccumulators(vertex, startEdge, accumulators);

            int lastIndex = accumulators.size()-1;
            NormalAccumulator last = accumulators.get(lastIndex);
            if(lastIndex == 0) {
                // All smooth
                applyAccumulator(last, last.firstLoop);
            }
            else {
                // Check if last edge was smooth (>1 values accumulated). If it was, combine the last accumulator with the first one.
                if(last.normal.x != 0 || last.normal.y != 0 || last.normal.z != 0) {
                    NormalAccumulator first = accumulators.get(0);
                    first.normal.addLocal(last.normal);
                    first.firstLoop = last.firstLoop;
                }

                // Don't process last accumulator. It only serves as a sentinel, so the second last accumulator knows where to stop.
                for(int i=0; i<lastIndex; ++i) {
                    applyAccumulator(accumulators.get(i), accumulators.get(i + 1).firstLoop);
                }
            }
        }

        normalCalculator.cleanup(bmesh);
    }


    private Edge getEdgeWithLoop(Vertex vertex) {
        //assert edgeSet.isEmpty();
        /*edgeSet.clear();
        vertex.edges().forEach(e -> edgeSet.add(e));
        if(edgeSet.isEmpty())
            continue;*/

        Edge edge = vertex.edge;
        if(edge == null)
            return null;

        do {
            if(edge.loop != null)
                return edge;
            edge = edge.getNextEdge(vertex);
        } while(edge != vertex.edge);

        return null;
    }


    private void populateAccumulators(Vertex vertex, Edge startEdge, NormalAccumulator.Pool accumulators) {
        // Find outgoing loop that uses this vertex
        Loop loop = startEdge.loop;
        if(loop.vertex != vertex)
            loop = loop.nextFaceLoop;

        final Loop startLoop = loop;
        NormalAccumulator acc = accumulators.pushBack(startLoop);

        // Iterate faces/edges around this Vertex by traversing the adjacent Loops (not the disk cycle formed by edges).
        // This gives proper clockwise order, but can only work when each edge only has a maximum of two adjacent faces (no T-structures).
        do {
            // Requires manifold (<=2 Loops per radial cycle of Edge)
            if(loop.nextEdgeLoop.nextEdgeLoop != loop)
                break;
            assert loop.vertex == vertex;

            addToAccumulator(acc, loop);

            // If the vertex lies on the border of a hole (equivalent to the border of a surface),
            // we may have to complete traversal in the other direction.
            // TODO: Make this work with bowtie structures? -> Remove processed edges from a Set, continue until empty
            if(loop.nextEdgeLoop == loop) {
                populateAccumulatorsCounterclockwise(vertex, accumulators);
                break;
            }

            // Skip clockwise to next outgoing loop that uses vertex
            Edge edge = loop.edge;
            Face face1 = loop.face;
            loop = loop.nextEdgeLoop.nextFaceLoop;

            Face face2 = loop.face;
            if(normalCalculator.isCrease(edge, face1, face2, creaseAngle))
                acc = accumulators.pushBack(loop);
        } while(loop != startLoop);
    }


    private void populateAccumulatorsCounterclockwise(Vertex vertex, NormalAccumulator.Pool accumulators) {
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

            Edge edge = loop.edge;
            Face face2 = loop.face;
            if(normalCalculator.isCrease(edge, face1, face2, creaseAngle))
                acc = accumulators.pushFront(loop);
            else
                acc.firstLoop = loop;

            addToAccumulator(acc, loop);
        };

        // Sentinel indicates that last edge wasn't smooth
        accumulators.pushBack(null); // TODO: Can this cause NPE?
    }


    private void addToAccumulator(NormalAccumulator acc, Loop loop) {
        normalCalculator.getWeightedNormal(loop, tempNormal);
        acc.normal.addLocal(tempNormal);
    }


    private void applyAccumulator(NormalAccumulator acc, final Loop endLoop) {
        Loop loop = acc.firstLoop;
        acc.normal.normalizeLocal();
        assert normalExists(acc.normal);

        do {
            propLoopNormal.set(loop, acc.normal);

            if(loop.nextEdgeLoop == loop)
                break;

            loop = loop.nextEdgeLoop.nextFaceLoop;
        } while(loop != endLoop);
    }


    private boolean normalExists(Vector3f normal) {
        if(normal.lengthSquared() < 0.9f) {
            normal.set(5, 5, 5);
            //return false;
        }

        return true;
    }
}
