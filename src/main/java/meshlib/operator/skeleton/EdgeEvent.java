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
        p.set(n0.bisector).multLocal(time).addLocal(n0.node.p);
    }


    private static float getTime(MovingNode n0, MovingNode n1) {
        Vector2f edge = n1.node.p.subtract(n0.node.p);
        float time = edge.length() / Math.abs(n0.edgeLengthChange);
        return time;
    }


    @Override
    public void handle(List<MovingNode> movingNodes, PriorityQueue<SkeletonEvent> eventQueue) {
        System.out.println("{{ handle " + this);

        abortEvents(eventQueue, n0);
        abortEvents(eventQueue, n1);

        // Keep n0, remove n1
        MovingNode next = n1.next;
        n0.next = next;
        next.prev = n0;

        n1.next = null;
        n1.prev = null;
        System.out.println("removing MovingNode" + n1.id);
        movingNodes.remove(n1);
        System.out.println("num nodes: " + movingNodes.size());

        //remapEdges()

        // Leave a node at old place and create new one
        leaveNode(n0);

        if(!checkDegenerate(n0, movingNodes)) {
            boolean degenerate = n0.calcBisector();
            if(!degenerate) {
                System.out.println("EdgeEvent: not degenerated");
                // Recalculate edge shrink rate of two adjacent edges
                n0.calcEdgeLengthChange();
                n0.prev.calcEdgeLengthChange();

                // TODO: Check if the events are needed (same sign as distanceSign)
                eventQueue.add(new EdgeEvent(n0, n0.next));
                eventQueue.add(new EdgeEvent(n0.prev, n0));
            } else {
                System.out.println("EdgeEvent: degenerated");
                MovingNode o1 = n0.next;
                MovingNode o2 = n0.prev;
                assert o1.prev == n0;
                assert o2.next == n0;
                o1.prev = o2;
                o2.next = o1;
                o1.reset();
                o2.reset();
                //o1.bisector.zero();
                //o2.bisector.zero();

                // TODO: This is needed for rectangles, but fails in 'bug3', because of circular references in resulting skeleton graph
                abortEvents(eventQueue, o1);
                abortEvents(eventQueue, o2);

                // Create skeleton edge to near node
                if(n0.node.p.distanceSquared(o1.node.p) < n0.node.p.distanceSquared(o2.node.p))
                    n0.node.addEdge(o1.node);
                else
                    n0.node.addEdge(o2.node);

                movingNodes.remove(n0);
                checkDegenerate(o1, movingNodes);
            }
        }

        System.out.println("}} handled");
        printEvents(eventQueue);
    }


    private void leaveNode(MovingNode node) {
        // Leave a node at old place and create new one
        SkeletonNode oldSkelNode = node.node;
        node.node = new SkeletonNode();
        node.node.p.set(oldSkelNode.p);
        oldSkelNode.addEdge(node.node);
    }


    private boolean checkDegenerate(MovingNode node, List<MovingNode> movingNodes) {
        if(node.next != node.prev)
            return false;

        // Handle case in which a triangle degenerates to a line
        System.out.println("degenerate");
        node.node.addEdge(node.next.node);

        node.reset();
        node.next.reset();

        movingNodes.remove(node);
        movingNodes.remove(node.next);

        // TODO: In this case it shouldn't scale any further (empty event queue?)
        //       thus making the above reset() unnecessary.
        //       --> Remove/invalidate events that can't happen anymore
        //       Or really use this current method as abort condition? Or return boolean from handle()?

        return true;
    }


    private void remapEdges() {
        // Re-map outgoingEdges with target 'n1.node' to 'n0.node'
        /*for(SkeletonNode inc : n1.node.incomingEdges) {
            inc.outgoingEdges.remove(n1.node);
            inc.outgoingEdges.add(n0.node);
            n0.node.incomingEdges.add(inc);
        }
        n1.node.incomingEdges.clear();*/
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
