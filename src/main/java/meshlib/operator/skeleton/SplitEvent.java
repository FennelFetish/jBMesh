package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;

class SplitEvent extends SkeletonEvent {
    private final MovingNode reflexNode;
    private final MovingNode op0;
    private final MovingNode op1;


    SplitEvent(MovingNode reflexNode, MovingNode opposite0, MovingNode opposite1, float distanceSign) {
        super(getTime(reflexNode, opposite0, opposite1, distanceSign));
        this.reflexNode = reflexNode;
        this.op0 = opposite0;
        this.op1 = opposite1;
    }


    private static float getTime(MovingNode reflexNode, MovingNode op0, MovingNode op1, float distanceSign) {
        Vector2f edge = op1.skelNode.p.subtract(op0.skelNode.p);
        Vector2f edgeDir = edge.normalize();

        // Project reflexNode orthogonal onto edge
        Vector2f reflexRelative = reflexNode.skelNode.p.subtract(op0.skelNode.p);
        float t = reflexRelative.dot(edgeDir);
        Vector2f projection = edgeDir.mult(t).addLocal(op0.skelNode.p);
        float orthoDistance = reflexNode.skelNode.p.distance(projection);

        float sin = reflexRelative.determinant(edgeDir); // cross(reflexRelative, edgeDir).length() -> but determinant can be negative!
        float side = 0f-Math.signum(sin);

        // Rotate edgeDir by 90° counterclockwise
        Vector2f edgeOrtho = new Vector2f();
        edgeOrtho.x = -edgeDir.y;
        edgeOrtho.y = edgeDir.x;

        // Calc component of bisector orthogonal to edge
        float bisectorSpeed = reflexNode.bisector.dot(edgeOrtho) * -distanceSign * side;
        //System.out.println("bisector speed (" + reflexNode.id + " => " + op0.id + "-" + op1.id + "):" + bisectorSpeed + " (side: " + side + ")");
        float edgeSpeed = -distanceSign * side;
        //float edgeSpeed = 1.0f * distanceSign;
        //float edgeSpeed = 1.0f;

        // Negative speed means distance between reflex vertex and opposite edge increases with time
        float totalSpeed = bisectorSpeed + edgeSpeed;
        if(totalSpeed <= 0)
            return Float.POSITIVE_INFINITY;

        // TODO: Check side
        float time = orthoDistance / totalSpeed;
        return time;
    }


    // TODO: Check if reflexNode's path actually hits opposite edge even if edge shrinks/moves
    /*private static float getTime(MovingNode reflexNode, MovingNode op0, MovingNode op1, float distanceSign) {
        Vector2f edge = op1.skelNode.p.subtract(op0.skelNode.p);
        Vector2f edgeDir = edge.normalize();

        // Project reflexNode orthogonal onto edge
        Vector2f reflexRelative = reflexNode.skelNode.p.subtract(op0.skelNode.p);
        float t = reflexRelative.dot(edgeDir);
        Vector2f projection = edgeDir.mult(t).addLocal(op0.skelNode.p);
        float orthoDistance = reflexNode.skelNode.p.distance(projection);

        // Rotate edgeDir by 90° counterclockwise
        Vector2f edgeOrtho = new Vector2f();
        edgeOrtho.x = -edgeDir.y;
        edgeOrtho.y = edgeDir.x;
        //edgeOrtho.multLocal(-distanceSign);

        // Calc component of bisector orthogonal to edge
        float bisectorSpeed = reflexNode.bisector.dot(edgeOrtho) * -distanceSign;
        System.out.println("bisector speed (" + reflexNode.id + " => " + op0.id + "-" + op1.id + "):" + bisectorSpeed);
        float edgeSpeed = -distanceSign;
        //float edgeSpeed = 1.0f * distanceSign;
        //float edgeSpeed = 1.0f;

        // Negative speed means distance between reflex vertex and opposite edge increases with time
        float totalSpeed = bisectorSpeed + edgeSpeed;
        if(totalSpeed <= 0)
            return Float.POSITIVE_INFINITY;

        // TODO: Check side
        float time = orthoDistance / totalSpeed;
        return time;
    }*/


    protected boolean shouldAbort(MovingNode adjacentNode) {
        return reflexNode == adjacentNode || op0 == adjacentNode || op1 == adjacentNode;
    }

    protected boolean shouldAbort(MovingNode edgeNode0, MovingNode edgeNode1) {
        return op0 == edgeNode0 && op1 == edgeNode1;
    }


    @Override
    public void handle(SkeletonContext ctx) {
        //System.out.println("{{ handle: " + this);

        // Check if reflexNode is between edge
        Vector2f edge = op1.skelNode.p.subtract(op0.skelNode.p);
        Vector2f edgeDir = edge.normalize();
        Vector2f reflexRelative = reflexNode.skelNode.p.subtract(op0.skelNode.p);
        float t = reflexRelative.dot(edgeDir);

        if(t < 0.0f || t > edge.length()) {
            //System.out.println("not on edge");
            return;
        }


        // TODO: 'bug8' (when growing): REFLEX VERTEX MovingNode{5} NOT ON EDGE 2-3
        // TODO: This doesn't happen anymore? But leave it here as assertion
        Vector2f projection = edgeDir.mult(t).addLocal(op0.skelNode.p);
        float projDist = reflexNode.skelNode.p.distance(projection);
        if(projDist > 0.01f) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> REFLEX VERTEX " + reflexNode + " NOT ON EDGE " + op0.id + "-" + op1.id);
            return;
        }


        System.out.println("{{ handle: " + this);

        assert op0.next == op1;
        assert op1.prev == op0;

        ctx.abortEvents(op0, op1);

        MovingNode node0 = reflexNode;
        MovingNode reflexNext = reflexNode.next;
        MovingNode reflexPrev = reflexNode.prev;

        ////SkeletonNode splitSkelNode = reflexNode.skelNode;
        ////node0.skelNode = new SkeletonNode();
        ////node0.skelNode.p.set(splitSkelNode.p);

        MovingNode node1 = ctx.createMovingNode(reflexNode.id + "+");
        node1.skelNode = node0.skelNode; // Both MovingNodes use same SkeletonNode! If they receive a valid bisector, a new SkeletonNode is made for them.
        ////node1.skelNode.p.set(splitSkelNode.p);
        //node1.skelNode.p.set(reflexNode.skelNode.p);
        //reflexNode.skelNode.addEdge(node1.skelNode);

        ////splitSkelNode.addEdge(node0.skelNode);
        ////splitSkelNode.addEdge(node1.skelNode);

        // Create new skeleton node
        //leaveNode(reflexNode);

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

        System.out.println("}} handled");
    }


    @Override
    public String toString() {
        return "SplitEvent{" + reflexNode.id + " => " + op0.id + "-" + op1.id + "}";
    }
}
