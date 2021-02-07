package meshlib.operator.skeleton;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;

class SkeletonContext {
    private int nextMovingNodeId = 1;

    public final ArrayList<MovingNode> movingNodes = new ArrayList<>();
    private final PriorityQueue<SkeletonEvent> eventQueue = new PriorityQueue<>();
    public float distanceSign;


    SkeletonContext() {}


    public void reset(float distanceSign) {
        this.distanceSign = distanceSign;
        nextMovingNodeId = 1;

        movingNodes.clear();
        eventQueue.clear();
    }


    //
    // Moving Nodes
    //

    public MovingNode createMovingNode() {
        MovingNode node = createMovingNode(Integer.toString(nextMovingNodeId));
        nextMovingNodeId++;
        return node;
    }

    public MovingNode createMovingNode(String id) {
        MovingNode node = new MovingNode(id);
        movingNodes.add(node);
        return node;
    }

    protected void removeMovingNode(MovingNode node) {
        System.out.println("removing MovingNode" + node.id);
        node.next = null;
        node.prev = null;
        abortEvents(node);
        movingNodes.remove(node);
    }


    //
    // Event Queue
    //

    public PriorityQueue<SkeletonEvent> getQueue() {
        return eventQueue;
    }

    public void enqueue(SkeletonEvent event) {
        eventQueue.offer(event);
    }

    public void abortEvents(MovingNode adjacentNode) {
        //System.out.println("Aborting events for adjacent node: " + adjacentNode);
        for(Iterator<SkeletonEvent> it = eventQueue.iterator(); it.hasNext(); ) {
            SkeletonEvent event = it.next();
            if(event.shouldAbort(adjacentNode)) {
                //System.out.println("  - abort " + event);
                it.remove();
            }
        }
    }

    public void abortEvents(MovingNode edgeNode0, MovingNode edgeNode1) {
        //System.out.println("Aborting events for edge: " + edgeNode0 + "-" + edgeNode1);
        for(Iterator<SkeletonEvent> it = eventQueue.iterator(); it.hasNext(); ) {
            SkeletonEvent event = it.next();
            if(event.shouldAbort(edgeNode0, edgeNode1)) {
                //System.out.println("  - abort " + event);
                it.remove();
            }
        }
    }

    public void printEvents() {
        eventQueue.stream().sorted().forEach(event -> {
            System.out.println(event + " in " + event.time);
        });
    }
}
