// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.operator.skeleton;

import java.util.*;

class SkeletonContext {
    private int nextMovingNodeId = 1;

    private final LinkedHashSet<MovingNode> movingNodes = new LinkedHashSet<>();
    private final TreeSet<SkeletonEvent> eventQueue = new TreeSet<>(); // Must support sorting, fast add & remove, poll lowest

    // Contains reflex nodes of aborted SplitEvents. Since we only enqueue the nearest SplitEvent to reduce strain on the queue,
    // when a SplitEvent is aborted we must recheck if a reflex node collides with another edge that was originally further away.
    private final Set<MovingNode> abortedReflex = new HashSet<>();

    public float distance;
    public float distanceSign;
    public float time = 0;

    public float epsilon = 0.0001f;
    public float epsilonMinusOne = epsilon - 1f; // -0.9999


    SkeletonContext() {}


    public void setEpsilon(float epsilon) {
        this.epsilon = epsilon;
        this.epsilonMinusOne = epsilon - 1f;
    }


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
        abortedReflex.clear();
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

    public void enqueue(SkeletonEvent event) {
        assert event.time >= time : "time: " + time + ", event.time: " + event.time + " // " + event;

        boolean added = eventQueue.add(event);
        assert added;
        event.onEventQueued();
    }

    public void addAbortedReflex(MovingNode reflexNode) {
        abortedReflex.add(reflexNode);
    }


    /**
     * Node invalidated.
     */
    public void abortEvents(MovingNode adjacentNode) {
        for(SkeletonEvent event : adjacentNode.events()) {
            event.onEventAborted(adjacentNode, this);
            eventQueue.remove(event);
        }

        adjacentNode.clearEvents();
    }

    /**
     * Edge invalidated.
     */
    public void abortEvents(MovingNode edgeNode0, MovingNode edgeNode1) {
        // Abort all events that exist in both edgeNode0's and edgeNode1's list
        for(Iterator<SkeletonEvent> it0=edgeNode0.events().iterator(); it0.hasNext(); ) {
            SkeletonEvent event = it0.next();
            if(edgeNode1.tryRemoveEvent(event)) {
                it0.remove();
                event.onEventAborted(edgeNode0, edgeNode1, this);
                eventQueue.remove(event);
            }
        }
    }


    public void printEvents() {
        System.out.println("Events:");
        eventQueue.stream().sorted().forEach(event -> {
            System.out.println(" - " + event + " in " + event.time);
        });
    }

    public void printNodes() {
        System.out.println("Nodes:");
        for(MovingNode node : movingNodes) {
            System.out.println(" - " + node);
        }
    }


    //
    // Event Factory
    //

    public void tryQueueEdgeEvent(MovingNode n0, MovingNode n1) {
        float eventTime = time + n0.getEdgeCollapseTime();

        // In case of an invalid time (=NaN), this condition will be false.
        if(eventTime <= distance)
            enqueue(new EdgeEvent(n0, n1, eventTime));
    }


    public void tryQueueSplitEvent(MovingNode reflexNode, MovingNode op0, MovingNode op1) {
        assert reflexNode.isReflex();

        float eventTime = time + SplitEvent.calcTime(reflexNode, op0, distanceSign);

        // In case of an invalid time (=NaN), this condition will be false.
        if(eventTime <= distance) {
            SplitEvent splitEvent = new SplitEvent(reflexNode, op0, op1, eventTime);
            enqueue(splitEvent);
        }
    }


    public SplitEvent tryReplaceNearestSplitEvent(MovingNode reflexNode, MovingNode op0, MovingNode op1, SplitEvent nearest) {
        assert reflexNode.isReflex();

        float eventTime = time + SplitEvent.calcTime(reflexNode, op0, distanceSign);

        if(nearest != null && nearest.time <= eventTime)
            return nearest;

        // In case of an invalid time (=NaN), this condition will be false.
        if(eventTime <= distance) {
            SplitEvent splitEvent = new SplitEvent(reflexNode, op0, op1, eventTime);
            return splitEvent;
        }

        return nearest;
    }


    public void recheckAbortedReflexNodes() {
        for(MovingNode reflexNode : abortedReflex) {
            if(reflexNode.next != null && reflexNode.isReflex())
                SkeletonEvent.createSplitEvents(reflexNode, this);
        }

        abortedReflex.clear();
    }
}
