package meshlib.operator.skeleton;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;

class SkeletonContext {
    static final float EPSILON = 0.0001f;
    static final float EPSILON_SQUARED = EPSILON * EPSILON;
    static final float EPSILON_MINUS_ONE = -1f + EPSILON; // 0.999

    private int nextMovingNodeId = 1;

    // TODO: Don't store MovingNodes in array, just use the links (prev/next). Only store one node for each loop. -> Optimizes add/remove
    public final ArrayList<MovingNode> movingNodes = new ArrayList<>();
    private final PriorityQueue<SkeletonEvent> eventQueue = new PriorityQueue<>();

    public float distance;
    public float distanceSign;
    public float time = 0;


    SkeletonContext() {}


    public void reset(float distance, float distanceSign) {
        this.distance = distance;
        this.distanceSign = distanceSign;
        time = 0;

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

    public SkeletonEvent pollQueue() {
        return eventQueue.poll();
    }

    private void enqueue(SkeletonEvent event) {
        assert event.time >= time;
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
        System.out.println("Events:");
        eventQueue.stream().sorted().forEach(event -> {
            System.out.println(" - " + event + " in " + event.time);
        });
    }


    //
    // Event Factory
    //

    public void tryQueueEdgeEvent(MovingNode n0, MovingNode n1) {
        if(sameSign(distanceSign, n0.edgeLengthChange)) {
            float eventTime = time + EdgeEvent.calcTime(n0, n1);
            if(eventTime <= distance)
                enqueue(new EdgeEvent(n0, n1, eventTime));
        }
    }

    private static boolean sameSign(float a, float b) {
        return (a >= 0) ^ (b < 0);
    }


    public void tryQueueSplitEvent(MovingNode reflexNode, MovingNode op0, MovingNode op1) {
        assert reflexNode.isReflex();

        float t = SplitEvent.calcTime(reflexNode, op0, op1, distanceSign);
        float eventTime = time + t;

        if(eventTime > distance)
            return;

        if(SplitEvent.canHit(reflexNode, op0, op1, distanceSign, t))
            enqueue(new SplitEvent(reflexNode, op0, op1, eventTime));
    }
}
