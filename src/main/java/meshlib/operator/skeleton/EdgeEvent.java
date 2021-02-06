package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

class EdgeEvent extends SkeletonEvent {
    private final MovingNode n0;
    private final MovingNode n1;
    public final Vector2f p = new Vector2f();


    public EdgeEvent(MovingNode n0, MovingNode n1) {
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


    @Override
    public void handle(List<MovingNode> movingNodes, PriorityQueue<SkeletonEvent> eventQueue) {
        System.out.println("{{ handle " + this);

        //abortEvents(eventQueue, n0); // TODO: This already happens in calcBisector_handleDegenerateAngle()
        //abortEvents(eventQueue, n1); // TODO: Put this into removeNode()

        // Merge nodes, keep n0, remove n1
        MovingNode next = n1.next;
        n0.next = next;
        next.prev = n0;

        n1.skelNode.remapIncoming(n0.skelNode);
        removeNode(n1, movingNodes, eventQueue);

        MovingNode node = n0;
        while(!checkDegenerateTriangle(node, movingNodes, eventQueue)) {
            node = calcBisector_handleDegenerateAngle(node, movingNodes, eventQueue);
            if(node == null)
                break;
        }

        System.out.println("num nodes: " + movingNodes.size());
        System.out.println("}} handled");
        printEvents(eventQueue);
    }


    public void handleOld(List<MovingNode> movingNodes, PriorityQueue<SkeletonEvent> eventQueue) {
        System.out.println("{{ handle " + this);

        abortEvents(eventQueue, n0);
        abortEvents(eventQueue, n1);

        // Merge nodes, keep n0, remove n1
        MovingNode next = n1.next;
        n0.next = next;
        next.prev = n0;

        n1.skelNode.remapIncoming(n0.skelNode);
        removeNode(n1, movingNodes, eventQueue);

        // Leave a node at old place and create new one
        //leaveNode(n0);

        if(checkDegenerateTriangle(n0, movingNodes, eventQueue)) {
            //leaveNode(n0);
        }
        else {
            boolean degenerate = n0.calcBisector();
            if(!degenerate) {
                leaveNode(n0);

                System.out.println("EdgeEvent: not degenerated");
                // Recalculate edge shrink rate of two adjacent edges
                n0.calcEdgeLengthChange();
                n0.prev.calcEdgeLengthChange();

                // TODO: Check if the events are needed (same sign as distanceSign)
                eventQueue.add(new EdgeEvent(n0, n0.next));
                eventQueue.add(new EdgeEvent(n0.prev, n0));
            } else {
                // Remove n0, connect prev/next
                MovingNode o1 = n0.prev;
                MovingNode o2 = n0.next;
                System.out.println("EdgeEvent: degenerated, connecting " + o1.id + " to " + o2.id);
                assert o1.next == n0;
                assert o2.prev == n0;
                o1.next = o2;
                o2.prev = o1;
                //o1.reset();
                //o2.reset();

                o1.calcBisector();
                o2.calcBisector();
                // TODO: check return value (degenerate?), calcEdgeLengthChange, addEvents ... like above?


                // TODO: This is needed for rectangles (because of circular references in resulting skeleton graph)
                //       but creates error (remaining triangle) in 'bug1',
                //abortEvents(eventQueue, o1);
                //abortEvents(eventQueue, o2);

                // TODO: Create event for this new edge between o1/o2?

                /*if(!checkDegenerate(o2, movingNodes))*/ {
                    // Create skeleton edge to near node
                    MovingNode connectionTarget;
                    if(n0.skelNode.p.distanceSquared(o1.skelNode.p) < n0.skelNode.p.distanceSquared(o2.skelNode.p))
                        connectionTarget = o1;
                    else
                        connectionTarget = o2;

                    System.out.println("skel connection from " + n0 + " to " + connectionTarget);
                    n0.skelNode.addEdge(connectionTarget.skelNode);
                    removeNode(n0, movingNodes, eventQueue);

                    // This would work.... but makes a stupid graph
                    //n0.node.addEdge(o1.node);
                    //n0.node.addEdge(o2.node);
                }

                checkDegenerateTriangle(o2, movingNodes, eventQueue);
            }
        }

        System.out.println("num nodes: " + movingNodes.size());
        System.out.println("}} handled");
        printEvents(eventQueue);
    }


    /**
     * // Handle case in which a triangle degenerates to a line
     * @param node
     * @param movingNodes
     * @return Degenerated?
     */
    private boolean checkDegenerateTriangle(MovingNode node, List<MovingNode> movingNodes, PriorityQueue<SkeletonEvent> eventQueue) {
        MovingNode next = node.next;
        if(next != node.prev)
            return false;

        System.out.println("Degenerated triangle");
        node.skelNode.addDegenerationEdge(next.skelNode);

        removeNode(node, movingNodes, eventQueue);
        removeNode(next, movingNodes, eventQueue);

        /*node.reset();
        next.reset();

        movingNodes.remove(node);
        movingNodes.remove(next);*/

        // TODO: In this case it shouldn't scale any further (empty event queue?)
        //       thus making the above reset() unnecessary.
        //       --> Remove/invalidate events that can't happen anymore
        //       Or really use this current method as abort condition? Or return boolean from handle()?

        return true;
    }


    /**
     * Always aborts events of 'node'.
     * @param node
     * @param movingNodes
     * @param eventQueue
     * @return
     */
    private MovingNode calcBisector_handleDegenerateAngle(MovingNode node, List<MovingNode> movingNodes, PriorityQueue<SkeletonEvent> eventQueue) {
        boolean degenerate = node.calcBisector();
        if(!degenerate) {
            abortEvents(eventQueue, node);
            leaveNode(node);

            System.out.println("EdgeEvent: not degenerated");
            // Recalculate edge shrink rate of two adjacent edges
            node.calcEdgeLengthChange();
            node.prev.calcEdgeLengthChange();

            // TODO: Check if the events are needed (same sign as distanceSign)
            eventQueue.add(new EdgeEvent(node, node.next));
            eventQueue.add(new EdgeEvent(node.prev, node));

            return null;
        }

        // Remove n0, connect prev/next
        MovingNode o1 = node.prev;
        MovingNode o2 = node.next;
        System.out.println("EdgeEvent: degenerated, connecting " + o1.id + " to " + o2.id);
        assert o1.next == node;
        assert o2.prev == node;
        o1.next = o2;
        o2.prev = o1;

        MovingNode connectionTarget;
        if(node.skelNode.p.distanceSquared(o1.skelNode.p) < node.skelNode.p.distanceSquared(o2.skelNode.p))
            connectionTarget = o1;
        else
            connectionTarget = o2;

        System.out.println("skel connection from " + node + " to " + connectionTarget);
        node.skelNode.addDegenerationEdge(connectionTarget.skelNode);
        removeNode(node, movingNodes, eventQueue);

        return connectionTarget;
    }


    private void removeNode(MovingNode node, List<MovingNode> movingNodes, PriorityQueue<SkeletonEvent> eventQueue) {
        System.out.println("removing MovingNode" + node.id);
        node.next = null;
        node.prev = null;
        abortEvents(eventQueue, node);
        movingNodes.remove(node);
    }

    private void leaveNode(MovingNode node) {
        System.out.println("leaveNode " + node);

        // Leave a node at old place and create new one
        SkeletonNode oldSkelNode = node.skelNode;
        node.skelNode = new SkeletonNode();
        node.skelNode.p.set(oldSkelNode.p);
        oldSkelNode.addEdge(node.skelNode);
    }


    private void abortEvents(PriorityQueue<SkeletonEvent> eventQueue, MovingNode node) {
        for(Iterator<SkeletonEvent> it=eventQueue.iterator(); it.hasNext(); ) {
            SkeletonEvent event = it.next();
            if(event instanceof EdgeEvent) {
                EdgeEvent edgeEvent = (EdgeEvent) event;
                if(edgeEvent.n0 == node || edgeEvent.n1 == node) {
                    System.out.println("abort " + edgeEvent);
                    it.remove();
                }
            }
        }
    }


    private void printEvents(PriorityQueue<SkeletonEvent> eventQueue) {
        for(SkeletonEvent event : eventQueue) {
            if(event instanceof EdgeEvent) {
                EdgeEvent edgeEvent = (EdgeEvent) event;
                System.out.println(edgeEvent + " collapse in " + edgeEvent.time);
            }
        }
    }


    @Override
    public String toString() {
        return "EdgeEvent{" + n0.id + "-" + n1.id + "}";
    }
}
