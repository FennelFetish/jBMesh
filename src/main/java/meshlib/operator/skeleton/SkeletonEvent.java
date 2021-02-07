package meshlib.operator.skeleton;

import java.util.LinkedList;
import java.util.List;

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


    protected void handle(MovingNode node, SkeletonContext ctx) {
        System.out.println("handle " + node);
        while(!checkDegenerateTriangle(node, ctx)) {
            node = calcBisector_handleDegenerateAngle(node, ctx);
            if(node == null)
                break;
        }
    }


    /**
     * // Handle case in which a triangle degenerates to a line
     * @return Degenerated?
     */
    private boolean checkDegenerateTriangle(MovingNode node, SkeletonContext ctx) {
        MovingNode next = node.next;
        if(next != node.prev)
            return false;

        System.out.println("EdgeEvent: Degenerated triangle");
        node.skelNode.addDegenerationEdge(next.skelNode);

        ctx.removeMovingNode(node);
        ctx.removeMovingNode(next);

        return true;
    }


    /**
     * Always aborts events of 'node'.
     */
    private MovingNode calcBisector_handleDegenerateAngle(MovingNode node, SkeletonContext ctx) {
        // New bisector = change of direction = new skeleton node
        boolean degenerate = node.calcBisector(ctx.distanceSign);
        if(!degenerate) {
            ctx.abortEvents(node);

            // Only create new SkeletonNode when collapsing edges, not when splitting
            //if(node.skelNode.incomingEdges.size() > 1)
                leaveNode(node);

            // Recalculate edge shrink rate of two adjacent edges
            node.calcEdgeLengthChange();
            node.prev.calcEdgeLengthChange();

            // Create edge events if edge is shrinking
            createEdgeEvent(node, node.next, ctx);
            createEdgeEvent(node.prev, node, ctx);

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
    }


    protected void leaveNode(MovingNode node) {
        System.out.println("leaveNode " + node);

        // Leave a node at old place and create new one
        SkeletonNode oldSkelNode = node.skelNode;
        node.skelNode = new SkeletonNode();
        node.skelNode.p.set(oldSkelNode.p);
        oldSkelNode.addEdge(node.skelNode);
    }


    private void createEdgeEvent(MovingNode n0, MovingNode n1, SkeletonContext ctx) {
        // Check if the events are needed (same sign as distanceSign)
        if(sameSign(ctx.distanceSign, n0.edgeLengthChange)) {
            EdgeEvent edgeEvent = new EdgeEvent(n0, n1);
            ctx.enqueue(edgeEvent);
        }
    }

    private static boolean sameSign(float a, float b) {
        return (a >= 0) ^ (b < 0);
    }


    // TODO: Bug because it's a  triangle?
    private void createSplitEvents_forEdge(MovingNode op0, MovingNode op1, SkeletonContext ctx) {
        assert op0.next == op1;
        assert op1.prev == op0;

        MovingNode current = op1.next;
        MovingNode last = op0.prev;

        List<MovingNode> trace = new LinkedList<>();

        while(current != last) {
            if(current.isReflex()) {
                SplitEvent splitEvent = new SplitEvent(current, op0, op1, ctx.distanceSign);
                ctx.enqueue(splitEvent);
            }

            trace.add(current);
            current = current.next;

            if(trace.size() > 100) {
                System.out.println("createSplitEvents_forEdge OVERFLOW");
                //Thread.dumpStack();

                System.out.println("op0.next: " + op0.next);
                System.out.println("op0.prev: " + op0.prev);
                System.out.println("op0.prev.next: " + op0.prev.next);

                System.out.println("op1.next: " + op1.next);
                System.out.println("op1.prev: " + op1.prev);

                System.out.println("start: " + op1.next);
                System.out.println("last: " + last);
                for(MovingNode n : trace)
                    System.out.println(" - " + n);
                break;
            }
        }
    }


    private void createSplitEvents_forVertex(MovingNode reflexNode, SkeletonContext ctx) {
        MovingNode start = reflexNode.next;
        MovingNode end = reflexNode.prev.prev;

        MovingNode op0 = start;
        while(op0 != end) {
            SplitEvent splitEvent = new SplitEvent(reflexNode, op0, op0.next, ctx.distanceSign);
            ctx.enqueue(splitEvent);
            op0 = op0.next;
        }
    }
}
