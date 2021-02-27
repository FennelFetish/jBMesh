package ch.alchemists.jbmesh.operator.skeleton;

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

        assert reflexNode != op0;
        assert reflexNode != op1;
        assert op0 != op1;
        assert op0.next == op1;
    }


    public static float calcTime(MovingNode reflexNode, MovingNode edgeStart, float distanceSign) {
        // Calc component of bisector orthogonal to edge
        float bisectorSpeed = reflexNode.bisector.determinant(edgeStart.edgeDir);
        float edgeSpeed = -distanceSign;
        float approachSpeed = bisectorSpeed + edgeSpeed;

        // Check on which side the reflex node lies, relative to directed edge.
        // The determinant's sign indicates the side. Its magnitude is the orthogonal distance of the reflex node to the edge.
        // (Component of 'reflexRelative' orthogonal to edgeDir)
        Vector2f reflexRelative = reflexNode.skelNode.p.subtract(edgeStart.skelNode.p);
        float sideDistance = reflexRelative.determinant(edgeStart.edgeDir);
        if(sideDistance == 0)
            return canHit(reflexNode, edgeStart, distanceSign, 0);

        // Negative speed means distance between reflex vertex and opposite edge increases with time
        if(correctSpeed(approachSpeed, sideDistance) <= 0)
            return INVALID_TIME;

        // One of these values will be negative. The resulting time is always positive.
        float time = -sideDistance / approachSpeed;
        return canHit(reflexNode, edgeStart, distanceSign, time);
    }

    private static float correctSpeed(float approachSpeed, float sideDistance) {
        // Adjust speed to side.
        return (sideDistance > 0) ? -approachSpeed : approachSpeed;
    }


    /**
     * Check if reflex actually collides with opposite edge in the future.
     * Do this before creating the event and not inside of handle() to avoid creating unnecessary events
     * which would slow down the queue and introduce superfluous scaling steps and hence rounding errors.
     */
    private static float canHit(MovingNode reflexNode, MovingNode edgeStart, float distanceSign, float time) {
        // Check if edge collapses before split occurs. If edge grows (invalid edgeCollapseTime = NaN), this will evaluate to false.
        if(time >= edgeStart.edgeCollapseTime)
            return INVALID_TIME;

        // This check is not reliable because there could be another event that prevents the collapse of neighboring edges.
            /*
            // Check if reflexNode's neighbor edges collapse before split occurs. This would abort the SplitEvent anyway, making it superfluous.
            if(time >= reflexNode.edgeCollapseTime || time >= reflexNode.prev.edgeCollapseTime) {
                return INVALID_TIME;
            }
            */

        // Check on which side 'reflexFuture' lies relative to the bisectors at start and end of this edge
        Vector2f reflexFuture = reflexNode.bisector.mult(time).addLocal(reflexNode.skelNode.p);

        Vector2f reflexRelative = reflexFuture.subtract(edgeStart.skelNode.p);
        float side0 = edgeStart.bisector.determinant(reflexRelative);
        if(side0 * distanceSign < 0)
            return INVALID_TIME;

        MovingNode edgeEnd = edgeStart.next;
        reflexRelative.set(reflexFuture).subtractLocal(edgeEnd.skelNode.p);
        float side1 = edgeEnd.bisector.determinant(reflexRelative);
        if(side1 * distanceSign > 0)
            return INVALID_TIME;

        return time;
    }


    @Override
    public void onEventQueued() {
        reflexNode.addEvent(this);
        op0.addEvent(this);
        op1.addEvent(this);
    }

    @Override
    public void onEventAborted(MovingNode adjacentNode, SkeletonContext ctx) {
        ctx.addAbortedReflex(reflexNode);

        if(adjacentNode == reflexNode) {
            op0.removeEvent(this);
            op1.removeEvent(this);
        }
        else if(adjacentNode == op0) {
            reflexNode.removeEvent(this);
            op1.removeEvent(this);
        }
        else {
            assert adjacentNode == op1;
            reflexNode.removeEvent(this);
            op0.removeEvent(this);
        }
    }

    @Override
    public void onEventAborted(MovingNode edgeNode0, MovingNode edgeNode1, SkeletonContext ctx) {
        ctx.addAbortedReflex(reflexNode);
        reflexNode.removeEvent(this);
    }


    @Override
    public void handle(SkeletonContext ctx) {
        assert op0.next == op1;
        ctx.abortEvents(op0, op1);

        MovingNode node0 = reflexNode;
        MovingNode reflexNext = reflexNode.next;
        MovingNode reflexPrev = reflexNode.prev;

        MovingNode node1 = ctx.createMovingNode(reflexNode.id + "+");
        // Both MovingNodes use same SkeletonNode which will stay at this place.
        // If they receive a valid bisector, a new SkeletonNode is made for them later in handle().
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

        handle(node0, ctx); // Aborts events of reflexNode
        handle(node1, ctx);
    }


    @Override
    public String toString() {
        return "SplitEvent{" + reflexNode.id + " => " + op0.id + "-" + op1.id + "}";
    }
}
