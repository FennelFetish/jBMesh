package ch.alchemists.jbmesh.operator.skeleton;

class EdgeEvent extends SkeletonEvent {
    private final MovingNode n0; // Edge start
    private final MovingNode n1; // Edge end


    EdgeEvent(MovingNode n0, MovingNode n1, float time) {
        super(time);
        this.n0 = n0;
        this.n1 = n1;

        assert n0 != n1;
        assert n0.next == n1;
    }


    @Override
    public void onEventQueued() {
        n0.addEvent(this);
        n1.addEvent(this);
    }

    @Override
    public void onEventAborted(MovingNode adjacentNode, SkeletonContext ctx) {
        // Remove other
        if(adjacentNode == n0)
            n1.removeEvent(this);
        else
            n0.removeEvent(this);
    }

    @Override
    public void onEventAborted(MovingNode edgeNode0, MovingNode edgeNode1, SkeletonContext ctx) {}


    @Override
    public void handle(SkeletonContext ctx) {
        assert n0.next == n1;

        // Merge nodes: keep n0, remove n1
        MovingNode next = n1.next;
        n0.next = next;
        next.prev = n0;

        if(n0.isReflex() || n1.isReflex())
            n0.skelNode.setReflex();

        n1.skelNode.remapIncoming(n0.skelNode);
        ctx.removeMovingNode(n1);

        handle(n0, ctx);
    }


    @Override
    public String toString() {
        return "EdgeEvent{" + n0.id + "-" + n1.id + "}";
    }
}
