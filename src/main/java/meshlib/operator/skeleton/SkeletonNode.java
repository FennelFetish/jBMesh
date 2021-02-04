package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;
import java.util.ArrayList;
import java.util.List;

class SkeletonNode {
    public final Vector2f p = new Vector2f();
    public final List<SkeletonNode> outgoingEdges = new ArrayList<>(1);
    public final List<SkeletonNode> incomingEdges = new ArrayList<>(2);

    public void addEdge(SkeletonNode target) {
        outgoingEdges.add(target);
        target.incomingEdges.add(this);
    }

    public void removeEdge(SkeletonNode target) {
        outgoingEdges.remove(target);
        target.incomingEdges.remove(this);
    }
}
