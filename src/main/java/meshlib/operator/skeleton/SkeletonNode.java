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


    // TODO: This should be a different type of edge? It doesn't map an initial vertex to another SkeletonNode. The mapping should stay.
    //       Edges from degeneration don't continue mapping of initial vertices. Degeneration = stop moving inwards, only connect inner skeleton nodes.
    public void addDegenerationEdge(SkeletonNode target) {
        addEdge(target);
    }


    public void removeEdge(SkeletonNode target) {
        outgoingEdges.remove(target);
        target.incomingEdges.remove(this);
    }


    public void remapIncoming(SkeletonNode newTarget) {
        assert outgoingEdges.isEmpty();

        for(SkeletonNode incoming : incomingEdges) {
            incoming.outgoingEdges.remove(this);
            incoming.addEdge(newTarget);
        }

        incomingEdges.clear();
    }
}
