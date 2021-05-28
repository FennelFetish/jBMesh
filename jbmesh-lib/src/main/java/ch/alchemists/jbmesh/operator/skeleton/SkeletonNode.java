package ch.alchemists.jbmesh.operator.skeleton;

import com.jme3.math.Vector2f;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkeletonNode {
    public static enum EdgeType {
        Mapping, Degeneracy
    }

    final Vector2f p = new Vector2f();
    final Map<SkeletonNode, EdgeType> outgoingEdges = new HashMap<>(2);
    final Map<SkeletonNode, EdgeType> incomingEdges = new HashMap<>(2);

    private boolean reflex = false;


    SkeletonNode() {}


    /**
     * Marks this SkeletonNode as connected to a reflex vertex.
     */
    void setReflex() {
        reflex = true;
    }

    /**
     * @return Whether this SkeletonNode is connected to a reflex vertex.
     */
    public boolean isReflex() {
        return reflex;
    }


    void addEdge(SkeletonNode target) {
        addEdge(target, EdgeType.Mapping);
    }


    // TODO: This should be a different type of edge? It doesn't map an initial vertex to another SkeletonNode. The mapping should stay.
    //       Edges from degeneration don't continue mapping of initial vertices. Degeneration = stop moving inwards, only connect inner skeleton nodes.
    void addDegenerationEdge(SkeletonNode target) {
        addEdge(target, EdgeType.Degeneracy);
    }


    private void addEdge(SkeletonNode target, EdgeType type) {
        outgoingEdges.put(target, type);
        target.incomingEdges.put(this, type);
    }


    void remapIncoming(SkeletonNode newTarget) {
        assert outgoingEdges.isEmpty();

        for(Map.Entry<SkeletonNode, EdgeType> entry : incomingEdges.entrySet()) {
            entry.getKey().outgoingEdges.remove(this);
            entry.getKey().addEdge(newTarget, entry.getValue());
        }

        incomingEdges.clear();
    }


    public void followGraphInward(List<SkeletonNode> storeTargets) {
        boolean leaf = true;

        for(Map.Entry<SkeletonNode, SkeletonNode.EdgeType> entry : outgoingEdges.entrySet()) {
            if(entry.getValue() == SkeletonNode.EdgeType.Mapping) {
                entry.getKey().followGraphInward(storeTargets);
                leaf = false;
            }
            /*else {
                storeTargets.add(entry.getKey());
            }*/
        }

        if(leaf)
            storeTargets.add(this);
    }
}
