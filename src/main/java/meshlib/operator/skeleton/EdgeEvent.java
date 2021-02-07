package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;

class EdgeEvent extends SkeletonEvent {
    private final MovingNode n0;
    private final MovingNode n1;
    public final Vector2f p = new Vector2f();


    EdgeEvent(MovingNode n0, MovingNode n1) {
        super(getTime(n0, n1));
        this.n0 = n0;
        this.n1 = n1;
        p.set(n0.bisector).multLocal(time).addLocal(n0.skelNode.p);
    }


    private static float getTime(MovingNode n0, MovingNode n1) {
        Vector2f edge = n1.skelNode.p.subtract(n0.skelNode.p);
        float time = edge.length() / Math.abs(n0.edgeLengthChange);
        return time;
    }


    protected boolean shouldAbort(MovingNode adjacentNode) {
        return n0 == adjacentNode || n1 == adjacentNode;
    }

    protected boolean shouldAbort(MovingNode edgeNode0, MovingNode edgeNode1) {
        return n0 == edgeNode0 && n1 == edgeNode1;
    }


    @Override
    public void handle(SkeletonContext ctx) {
        System.out.println("{{ handle " + this);

        // Merge nodes, keep n0, remove n1
        MovingNode next = n1.next;
        n0.next = next;
        next.prev = n0;

        n1.skelNode.remapIncoming(n0.skelNode);
        ctx.removeMovingNode(n1);

        handle(n0, ctx);

        System.out.println("num nodes: " + ctx.movingNodes.size());
        System.out.println("}} handled");
    }


    @Override
    public String toString() {
        return "EdgeEvent{" + n0.id + "-" + n1.id + "}";
    }
}
