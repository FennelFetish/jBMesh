package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;

class SplitEvent extends SkeletonEvent {
    private final MovingNode reflexNode;
    private final MovingNode op0; // Opposite edge start
    private final MovingNode op1; // Opposite edge end


    SplitEvent(MovingNode reflexNode, MovingNode opposite0, MovingNode opposite1, float time) {
        super(time);
        this.reflexNode = reflexNode;
        this.op0 = opposite0;
        this.op1 = opposite1;
    }


    public static float calcTime(MovingNode reflexNode, MovingNode op0, MovingNode op1, float distanceSign) {
        Vector2f edgeDir = op1.skelNode.p.subtract(op0.skelNode.p).normalizeLocal();

        // Calc component of bisector orthogonal to edge
        float bisectorSpeed = reflexNode.bisector.determinant(edgeDir);
        float edgeSpeed = -1.0f;
        float approachSpeed = (bisectorSpeed+edgeSpeed) * distanceSign;

        // Check on which side the reflex node lies, relative to directed edge.
        // The determinant's sign indicates the side. Its magnitude is the orthogonal distance of the reflex node to the edge.
        // (Component of 'reflexRelative' orthogonal to edgeDir)
        Vector2f reflexRelative = reflexNode.skelNode.p.subtract(op0.skelNode.p);
        float sideDistance = reflexRelative.determinant(edgeDir);

        // Negative speed means distance between reflex vertex and opposite edge increases with time
        if(correctSpeed(approachSpeed, sideDistance) <= 0)
            return Float.POSITIVE_INFINITY;

        // One of these values will be negative. The resulting time is always positive.
        float time = -sideDistance / approachSpeed;
        return time;
    }

    private static float correctSpeed(float approachSpeed, float side) {
        // Adjust speed to side
        return (side > 0) ? -approachSpeed : approachSpeed;
    }


    /**
     * Check if reflex actually collides with opposite edge in the future.
     * Do this to avoid creating unnecessary events which would introduce superfluous scaling steps and hence rounding errors.
     */
    public static boolean canHit(MovingNode reflexNode, MovingNode op0, MovingNode op1, float distanceSign, float time) {
        // Check on which side 'reflexFuture' lies relative to bisectors of op0 and op1
        Vector2f reflexFuture = reflexNode.bisector.mult(distanceSign*time).addLocal(reflexNode.skelNode.p);

        Vector2f reflexRelative = reflexFuture.subtract(op0.skelNode.p);
        float side0 = op0.bisector.determinant(reflexRelative);
        if(side0 < 0)
            return false;

        reflexRelative.set(reflexFuture).subtractLocal(op1.skelNode.p);
        float side1 = op1.bisector.determinant(reflexRelative);
        if(side1 > 0)
            return false;

        return true;
    }


    @Override
    protected boolean shouldAbort(MovingNode adjacentNode) {
        return reflexNode == adjacentNode || op0 == adjacentNode || op1 == adjacentNode;
    }

    @Override
    protected boolean shouldAbort(MovingNode edgeNode0, MovingNode edgeNode1) {
        return op0 == edgeNode0 && op1 == edgeNode1;
    }


    @Override
    public void handle(SkeletonContext ctx) {
        // Check if reflexNode is between edge
        Vector2f edge = op1.skelNode.p.subtract(op0.skelNode.p);
        float edgeLength = edge.length();
        Vector2f edgeDir = edge.divideLocal(edgeLength);
        Vector2f reflexRelative = reflexNode.skelNode.p.subtract(op0.skelNode.p);
        float t = reflexRelative.dot(edgeDir);

        if(t < 0.0f-SkeletonContext.EPSILON || t > edgeLength+SkeletonContext.EPSILON) {
        //if(t < 0.0f || t > edgeLength) {
            System.out.println("not on edge");
            assert false;
            return;
        }


        // TODO: 'bug8' (when growing): REFLEX VERTEX MovingNode{5} NOT ON EDGE 2-3
        // TODO: This doesn't happen anymore? But leave it here as assertion
        Vector2f projection = edgeDir.mult(t).addLocal(op0.skelNode.p);
        float projDist = reflexNode.skelNode.p.distanceSquared(projection);
        if(projDist > SkeletonContext.EPSILON_SQUARED) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> REFLEX VERTEX " + reflexNode + " NOT ON EDGE " + op0.id + "-" + op1.id);
            assert false;
            return;
        }


        assert op0.next == op1;
        assert op1.prev == op0;
        ctx.abortEvents(op0, op1);

        MovingNode node0 = reflexNode;
        MovingNode reflexNext = reflexNode.next;
        MovingNode reflexPrev = reflexNode.prev;

        MovingNode node1 = ctx.createMovingNode(reflexNode.id + "+");
        // Both MovingNodes use same SkeletonNode! If they receive a valid bisector, a new SkeletonNode is made for them.
        node1.skelNode = node0.skelNode;

        // Update node0 links
        assert node0.next == reflexNext;
        assert reflexNext.prev == node0;

        node0.prev = op0;
        op0.next = node0;

        // Update node1 links
        node1.next = op1;
        op1.prev = node1;

        node1.prev = reflexPrev;
        reflexPrev.next = node1;

        handle(node0, ctx);
        handle(node1, ctx);
    }


    @Override
    public String toString() {
        return "SplitEvent{" + reflexNode.id + " => " + op0.id + "-" + op1.id + "}";
    }
}
