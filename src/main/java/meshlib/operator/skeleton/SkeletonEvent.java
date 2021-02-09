package meshlib.operator.skeleton;

abstract class SkeletonEvent implements Comparable<SkeletonEvent> {
    public float time; // Always positive

    protected SkeletonEvent(float time) {
        this.time = time;
    }

    @Override
    public int compareTo(SkeletonEvent o) {
        return Float.compare(this.time, o.time);
    }


    protected abstract boolean shouldAbort(MovingNode adjacentNode);
    protected abstract boolean shouldAbort(MovingNode edgeNode0, MovingNode edgeNode1);

    public abstract void handle(SkeletonContext ctx);


    /**
     * Always aborts events of 'node'.
     */
    protected void handle(MovingNode node, SkeletonContext ctx) {
        System.out.println("handle " + node);
        while(checkValidPolygon(node, ctx)) {
            boolean validBisector = node.calcBisector(ctx.distanceSign);
            if(validBisector) {
                leaveNode(node);
                node.calcEdgeLengthChange();
                node.prev.calcEdgeLengthChange();
                createEvents(node, ctx);
                break;
            }

            node = handleDegenerateAngle(node, ctx);
        }
    }


    /*protected void handleOld(MovingNode node, SkeletonContext ctx) {
        System.out.println("handle " + node);
        while(checkValidPolygon(node, ctx)) {
            node = calcBisector_handleDegenerateAngle(node, ctx);
            if(node == null)
                break;
        }
    }*/


    /**
     * Check for valid polygon and handle case in which a triangle degenerates to a line.
     * @return True if polygon consists of >2 edges.
     */
    private boolean checkValidPolygon(MovingNode node, SkeletonContext ctx) {
        MovingNode next = node.next;
        assert next != node;

        if(next != node.prev)
            return true;

        System.out.println("EdgeEvent: Degenerated triangle");
        node.skelNode.addDegenerationEdge(next.skelNode);
        ctx.removeMovingNode(node);
        ctx.removeMovingNode(next);

        return false;
    }


    /**
     * Always aborts events of 'node'.
     */
    /*private MovingNode calcBisector_handleDegenerateAngle(MovingNode node, SkeletonContext ctx) {
        // New bisector = change of direction = new skeleton node
        boolean degenerate = node.calcBisector(ctx.distanceSign);
        if(!degenerate) {
            ctx.abortEvents(node);
            leaveNode(node);

            // Recalculate edge shrink rate of two adjacent edges
            node.calcEdgeLengthChange();
            node.prev.calcEdgeLengthChange();

            // Create edge events if edge is shrinking
            ctx.tryQueueEdgeEvent(node, node.next);
            ctx.tryQueueEdgeEvent(node.prev, node);

            // Create SplitEvents: Test both adjacent edges against all reflex vertices (distanceSign dependent) in current MovingNode loop.
            createSplitEvents_forEdge(node, node.next, ctx);
            createSplitEvents_forEdge(node.prev, node, ctx);

            // If 'node' is reflex (distanceSign dependent), test against all edges in current MovingNode loop.
            if(node.isReflex())
                createSplitEvents_forVertex(node, ctx);

            return null;
        }

        // Remove n0, connect prev/next
        MovingNode o1 = node.prev;
        MovingNode o2 = node.next;
        System.out.println("EdgeEvent: Degenerated angle, connecting " + o1.id + " to " + o2.id);
        assert o1.next == node;
        assert o2.prev == node;
        o1.next = o2;
        o2.prev = o1;

        MovingNode connectionTarget;
        if(node.skelNode.p.distanceSquared(o1.skelNode.p) < node.skelNode.p.distanceSquared(o2.skelNode.p))
            connectionTarget = o1;
        else
            connectionTarget = o2;

        node.skelNode.addDegenerationEdge(connectionTarget.skelNode);
        ctx.removeMovingNode(node);

        return connectionTarget;
    }*/


    protected void leaveNode(MovingNode node) {
        System.out.println("leaveNode " + node);

        // Leave a node at old place and create new one
        SkeletonNode oldSkelNode = node.skelNode;
        node.skelNode = new SkeletonNode();
        node.skelNode.p.set(oldSkelNode.p);
        oldSkelNode.addEdge(node.skelNode);
    }


    private void createEvents(MovingNode node, SkeletonContext ctx) {
        ctx.abortEvents(node);

        // Create edge events if edge is shrinking
        ctx.tryQueueEdgeEvent(node, node.next);
        ctx.tryQueueEdgeEvent(node.prev, node);

        // Create SplitEvents: Test both adjacent edges against all reflex vertices (distanceSign dependent) in current MovingNode loop.
        createSplitEvents_forEdge(node, node.next, ctx);
        createSplitEvents_forEdge(node.prev, node, ctx);

        // If 'node' is reflex (distanceSign dependent), test against all edges in current MovingNode loop.
        if(node.isReflex())
            createSplitEvents_forVertex(node, ctx);
    }

    private void createSplitEvents_forEdge(MovingNode op0, MovingNode op1, SkeletonContext ctx) {
        assert op0.next == op1;
        assert op1.prev == op0;

        MovingNode current = op1.next;
        MovingNode end = op0.prev;

        while(current != end) {
            if(current.isReflex())
                ctx.tryQueueSplitEvent(current, op0, op1);

            current = current.next;
        }
    }

    private void createSplitEvents_forVertex(MovingNode reflexNode, SkeletonContext ctx) {
        MovingNode current = reflexNode.next;
        MovingNode end = reflexNode.prev.prev;

        while(current != end) {
            ctx.tryQueueSplitEvent(reflexNode, current, current.next);
            current = current.next;
        }
    }


    private MovingNode handleDegenerateAngle(MovingNode node, SkeletonContext ctx) {
        // Remove node, connect node.prev <-> node.next
        MovingNode o1 = node.prev;
        MovingNode o2 = node.next;
        System.out.println("EdgeEvent: Degenerated angle, connecting " + o1.id + " to " + o2.id);
        assert o1.next == node;
        assert o2.prev == node;
        o1.next = o2;
        o2.prev = o1;

        MovingNode connectionTarget;
        if(node.skelNode.p.distanceSquared(o1.skelNode.p) < node.skelNode.p.distanceSquared(o2.skelNode.p))
            connectionTarget = o1;
        else
            connectionTarget = o2;

        node.skelNode.addDegenerationEdge(connectionTarget.skelNode);
        ctx.removeMovingNode(node);

        return connectionTarget;
    }
}
