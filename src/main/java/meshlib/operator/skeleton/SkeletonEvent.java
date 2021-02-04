package meshlib.operator.skeleton;

import java.util.List;
import java.util.PriorityQueue;

abstract class SkeletonEvent implements Comparable<SkeletonEvent> {
    public float time; // Always positive

    protected SkeletonEvent(float time) {
        this.time = time;
    }

    @Override
    public int compareTo(SkeletonEvent o) {
        return Float.compare(this.time, o.time);
    }

    public abstract void handle(List<MovingNode> movingNodes, PriorityQueue<SkeletonEvent> eventQueue);
}
