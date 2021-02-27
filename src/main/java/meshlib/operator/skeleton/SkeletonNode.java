package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;
import java.util.HashMap;
import java.util.Map;

class SkeletonNode {
    public static enum EdgeType {
        Mapping, Degeneracy
    }

    public final Vector2f p = new Vector2f();
    public final Map<SkeletonNode, EdgeType> outgoingEdges = new HashMap<>(2);
    public final Map<SkeletonNode, EdgeType> incomingEdges = new HashMap<>(2);


    SkeletonNode() {}


    public void addEdge(SkeletonNode target) {
        addEdge(target, EdgeType.Mapping);
    }


    // TODO: This should be a different type of edge? It doesn't map an initial vertex to another SkeletonNode. The mapping should stay.
    //       Edges from degeneration don't continue mapping of initial vertices. Degeneration = stop moving inwards, only connect inner skeleton nodes.
    public void addDegenerationEdge(SkeletonNode target) {
        addEdge(target, EdgeType.Degeneracy);
    }


    private void addEdge(SkeletonNode target, EdgeType type) {
        outgoingEdges.put(target, type);
        target.incomingEdges.put(this, type);
    }


    public void remapIncoming(SkeletonNode newTarget) {
        assert outgoingEdges.isEmpty();

        for(Map.Entry<SkeletonNode, EdgeType> entry : incomingEdges.entrySet()) {
            entry.getKey().outgoingEdges.remove(this);
            entry.getKey().addEdge(newTarget, entry.getValue());
        }

        incomingEdges.clear();
    }
}
