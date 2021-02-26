package meshlib.operator.skeleton;

abstract class SkeletonEvent implements Comparable<SkeletonEvent> {
    public static final float INVALID_TIME = Float.NaN;
    public final float time; // Always positive


    protected SkeletonEvent(float time) {
        this.time = time;
    }


    @Override
    public int compareTo(SkeletonEvent o) {
        if(this.time < o.time)
            return -1;
        if(this.time > o.time)
            return 1;

        // A TreeSet as event queue doesn't allow duplicate keys (time values).
        // Compare the hashes so compareTo() won't return 0 if events happen at the same time.
        return Integer.compare(hashCode(), o.hashCode());
    }


    public abstract void onEventQueued();
    public abstract void onEventAborted(MovingNode adjacentNode, SkeletonContext ctx);
    public abstract void onEventAborted(MovingNode edgeNode0, MovingNode edgeNode1, SkeletonContext ctx);

    public abstract void handle(SkeletonContext ctx);


    /**
     * Always aborts events of 'node'.
     */
    protected static void handle(MovingNode node, SkeletonContext ctx) {
        //System.out.println("handle " + node);
        while(ensureValidPolygon(node, ctx)) {
            boolean validBisector = node.calcBisector(ctx.distanceSign);
            if(validBisector) {
                node.leaveSkeletonNode();

                node.updateEdge();
                node.prev.updateEdge();

                createEvents(node, ctx);
                return;
            }

            node = handleDegenerateAngle(node, ctx);
        }
    }


    static void handleInit(MovingNode node, SkeletonContext ctx) {
        while(ensureValidPolygon(node, ctx)) {
            boolean validBisector = node.calcBisector(ctx.distanceSign);
            if(validBisector)
                return;

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

        createAllSplitEvents(node, ctx);
    }


    // TODO: Test reflex nodes against all edges in other loops too? That would allow multiple disconnected initial loops.
    /**
     * Tests adjacent edges of 'node' against other eligible reflex vertices in MovingNodes-loop.
     * If 'node' is reflex, tests it against all eligible edges.
     * Eligible tests: Minimum distance between reflex node and candidate edge = 2 edges in between
     *
     * A triangle cannot be concave. A concave quadrilateral (arrowhead) doesn't need split events.
     * Minimum vertices for split events = 5.
     */
    private static void createAllSplitEvents(MovingNode node, SkeletonContext ctx) {
        MovingNode current = node.next.next;   // processed in first step before loop
        final MovingNode end = node.prev.prev; // excluded from loop, but processed in last step after loop

        // Ignore triangles and quads
        if(current == end.next || current == end)
            return;

        final boolean nodeIsReflex = node.isReflex();
        SplitEvent nearestSplit = null;

        // First step: Test 'current' vertex only against first adjacent edge (node.prev->node).
        //             Test 'node' against current edge.
        if(current.isReflex())
            ctx.tryQueueSplitEvent(current, node.prev, node);

        if(nodeIsReflex)
            nearestSplit = ctx.tryReplaceNearestSplitEvent(node, current, current.next, nearestSplit);

        // Intermediate steps, all tests
        current = current.next;
        for(; current != end; current = current.next) {
            if(current.isReflex()) {
                ctx.tryQueueSplitEvent(current, node, node.next);
                ctx.tryQueueSplitEvent(current, node.prev, node);
            }

            if(nodeIsReflex) // Condition is constant. Manual optimization (loop unswitching) not worth it.
                nearestSplit = ctx.tryReplaceNearestSplitEvent(node, current, current.next, nearestSplit);
        }

        // Last step: Test 'current' only against second adjacent edge (node->node.next)
        //            Don't test "nodeIsReflex" against this last edge.
        if(current.isReflex())
            ctx.tryQueueSplitEvent(current, node, node.next);

        if(nearestSplit != null)
            ctx.enqueue(nearestSplit);
    }


    static void createSplitEvents(MovingNode reflexNode, SkeletonContext ctx) {
        MovingNode current = reflexNode.next.next;
        MovingNode end = reflexNode.prev.prev; // exclusive

        // Ignore triangles, quads will also be ignored by the loop condition below
        if(current == end.next)
            return;

        SplitEvent nearestSplit = null;
        for(; current != end; current = current.next)
            nearestSplit = ctx.tryReplaceNearestSplitEvent(reflexNode, current, current.next, nearestSplit);

        if(nearestSplit != null)
            ctx.enqueue(nearestSplit);
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
