package meshlib.operator.skeleton;

abstract class SkeletonEvent implements Comparable<SkeletonEvent> {
    public static final float INVALID_TIME = Float.NaN;
    public final float time; // Always positive


    protected SkeletonEvent(float time) {
        this.time = time;
    }

    @Override
    public int compareTo(SkeletonEvent o) {
        return Float.compare(this.time, o.time);
    }


    public abstract boolean shouldAbort(MovingNode adjacentNode);
    public abstract boolean shouldAbort(MovingNode edgeNode0, MovingNode edgeNode1);

    public abstract void handle(SkeletonContext ctx);


    /**
     * Always aborts events of 'node'.
     */
    static void handle(MovingNode node, SkeletonContext ctx) {
        //System.out.println("handle " + node);
        while(ensureValidPolygon(node, ctx)) {
            boolean validBisector = node.calcBisector(ctx.distanceSign);
            if(validBisector) {
                node.leaveSkeletonNode();

                node.calcEdgeLengthChange(ctx.distanceSign);
                node.prev.calcEdgeLengthChange(ctx.distanceSign);

                createEvents(node, ctx);
                break;
            }

            node = handleDegenerateAngle(node, ctx);
        }
    }

    static void handleInit(MovingNode node, SkeletonContext ctx) {
        while(ensureValidPolygon(node, ctx)) {
            boolean validBisector = node.calcBisector(ctx.distanceSign);
            if(validBisector) {
                node.calcEdgeLengthChange(ctx.distanceSign);
                node.prev.calcEdgeLengthChange(ctx.distanceSign);
                break;
            }

            node = handleDegenerateAngle(node, ctx);
        }
    }


    /**
     * Check for valid polygon and handle case in which a polygon degenerates to a line.
     * @return True if polygon consists of >2 edges.
     */
    private static boolean ensureValidPolygon(MovingNode node, SkeletonContext ctx) {
        MovingNode next = node.next;
        assert next != node;

        if(next != node.prev)
            return true;

        // Degenerated polygon
        node.skelNode.addDegenerationEdge(next.skelNode);
        ctx.removeMovingNode(node);
        ctx.removeMovingNode(next);

        return false;
    }


    private static void createEvents(MovingNode node, SkeletonContext ctx) {
        ctx.abortEvents(node);

        // Create edge events if edge is shrinking
        ctx.tryQueueEdgeEvent(node, node.next);
        ctx.tryQueueEdgeEvent(node.prev, node);

        createSplitEvents(node, ctx);
    }


    /**
     * Tests adjacent edges of 'node' against other eligible reflex vertices in MovingNodes-loop.
     * If 'node' is reflex, tests it against all eligible edges.
     * Eligible tests: Minimum distance between reflex node and candidate edge = 2 edges in between
     *
     * A triangle cannot be concave. A concave quadrilateral (arrowhead) doesn't need split events.
     * Minimum vertices for split events = 5.
     */
    private static void createSplitEvents(MovingNode node, SkeletonContext ctx) {
        MovingNode current = node.next.next;   // processed in first step before loop
        final MovingNode end = node.prev.prev; // excluded from loop, but processed in last step after loop

        // Ignore triangles and quads
        if(current == end.next || current == end)
            return;

        final boolean nodeIsReflex = node.isReflex();

        // First step: Test 'current' vertex only against first adjacent edge (node.prev->node).
        //             Test 'node' against current edge.
        if(current.isReflex())
            ctx.tryQueueSplitEvent(current, node.prev, node);

        if(nodeIsReflex)
            ctx.tryQueueSplitEvent(node, current, current.next);

        // Intermediate steps, all tests
        current = current.next;
        for(; current != end; current = current.next) {
            if(current.isReflex()) {
                ctx.tryQueueSplitEvent(current, node, node.next);
                ctx.tryQueueSplitEvent(current, node.prev, node);
            }

            if(nodeIsReflex) // Condition is constant. Manual optimization (loop unswitching) not worth it.
                ctx.tryQueueSplitEvent(node, current, current.next);
        }

        // Last step: Test 'current' only against second adjacent edge (node->node.next)
        //            Don't test 'node' against this last edge.
        if(current.isReflex())
            ctx.tryQueueSplitEvent(current, node, node.next);
    }


    private static MovingNode handleDegenerateAngle(MovingNode node, SkeletonContext ctx) {
        // Remove node, connect node.prev <-> node.next
        MovingNode o1 = node.prev;
        MovingNode o2 = node.next;
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
