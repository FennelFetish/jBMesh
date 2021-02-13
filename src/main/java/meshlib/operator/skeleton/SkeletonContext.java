package meshlib.operator.skeleton;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import meshlib.util.Profiler;

class SkeletonContext {
    static final float EPSILON = 0.0001f;
    static final float EPSILON_SQUARED = EPSILON * EPSILON;
    static final float EPSILON_MINUS_ONE = EPSILON - 1f; // 0.9999

    private int nextMovingNodeId = 1;

    private final Set<MovingNode> movingNodes = new HashSet<>();
    private final TreeSet<SkeletonEvent> eventQueue = new TreeSet<>(); // Must support sorting, fast add & remove, poll lowest

    public float distance;
    public float distanceSign;
    public float time = 0;


    SkeletonContext() {}


    public Set<MovingNode> getNodes() {
        return Collections.unmodifiableSet(movingNodes);
    }


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
        node.next = null;
        node.prev = null;
        abortEvents(node);
        movingNodes.remove(node);
    }


    //
    // Event Queue
    //

    public SkeletonEvent pollQueue() {
        return eventQueue.pollFirst();
    }

    private void enqueue(SkeletonEvent event) {
        assert event.time >= time : "time: " + time + ", event.time: " + event.time + " // " + event;
        //System.out.println("Queued: " + event);
        //eventQueue.offer(event);
        eventQueue.add(event);
    }

    public void abortEvents(MovingNode adjacentNode) {
        try(Profiler p = Profiler.start("SkeletonContext.abortEvents(node)")) {
            eventQueue.removeIf(event -> event.shouldAbort(adjacentNode));

            /*System.out.println("Aborting events for adjacent node: " + adjacentNode);
            for(Iterator<SkeletonEvent> it = eventQueue.iterator(); it.hasNext(); ) {
                SkeletonEvent event = it.next();
                if(event.shouldAbort(adjacentNode)) {
                    System.out.println("  - abort " + event);
                    it.remove();
                    //event.aborted = true;
                }
            }*/
        }
    }

    public void abortEvents(MovingNode edgeNode0, MovingNode edgeNode1) {
        try(Profiler p = Profiler.start("SkeletonContext.abortEvents(node, node)")) {
            eventQueue.removeIf(event -> event.shouldAbort(edgeNode0, edgeNode1));

            /*System.out.println("Aborting events for edge: " + edgeNode0 + "-" + edgeNode1);
            for(Iterator<SkeletonEvent> it = eventQueue.iterator(); it.hasNext(); ) {
                SkeletonEvent event = it.next();
                if(event.shouldAbort(edgeNode0, edgeNode1)) {
                    System.out.println("  - abort " + event);
                    it.remove();
                    //event.aborted = true;
                }
            }*/
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
        float t = n0.edgeCollapseTime;
        if(t == SkeletonEvent.INVALID_TIME)
            return;

        float eventTime = time + t;
        if(eventTime <= distance)
            enqueue(new EdgeEvent(n0, n1, eventTime));
    }


    public void tryQueueSplitEvent(MovingNode reflexNode, MovingNode op0, MovingNode op1) {
        assert reflexNode.isReflex();

        float t = SplitEvent.calcTime(reflexNode, op0, distanceSign);
        if(t == SkeletonEvent.INVALID_TIME)
            return;

        // Check if edge collapses before time -> prevent events = smaller event queue
        if(t >= op0.edgeCollapseTime)
            return;

        float eventTime = time + t;
        if(eventTime <= distance && SplitEvent.canHit(reflexNode, op0, op1, distanceSign, t))
            enqueue(new SplitEvent(reflexNode, op0, op1, eventTime));
    }
}
