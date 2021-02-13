package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.*;
import meshlib.lookup.ExactHashDeduplication;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;
import meshlib.util.PlanarCoordinateSystem;

public class SkeletonVisualization {
    public static class VisNode {
        public final Vector3f pos = new Vector3f();
        public final String name;

        public VisNode(String name) {
            this.name = name;
        }
    }


    private final PlanarCoordinateSystem coordSys;
    private final ArrayList<SkeletonNode> initialNodes ;
    private final SkeletonContext ctx;


    SkeletonVisualization(PlanarCoordinateSystem coordSys, ArrayList<SkeletonNode> initialNodes, SkeletonContext ctx) {
        this.coordSys = coordSys;
        this.initialNodes = initialNodes;
        this.ctx = ctx;
    }


    public BMesh createSkeletonMappingVis() {
        return createStraightSkeletonVis(SkeletonNode.EdgeType.Mapping);
    }

    public BMesh createSkeletonDegeneracyVis() {
        return createStraightSkeletonVis(SkeletonNode.EdgeType.Degeneracy);
    }

    private BMesh createStraightSkeletonVis(SkeletonNode.EdgeType edgeType) {
        BMesh bmesh = new BMesh();
        ExactHashDeduplication dedup = new ExactHashDeduplication(bmesh);
        Set<SkeletonNode> nodesDone = new HashSet<>();

        for(SkeletonNode node : initialNodes) {
            straightSkeletonVis_addEdge(bmesh, dedup, nodesDone, node, edgeType);
        }

        //System.out.println("Straight Skeleton Visualization: " + nodesDone.size() + " unique nodes");
        return bmesh;
    }

    private boolean isInvalid(Vector2f v) {
        return Float.isNaN(v.x) || Float.isInfinite(v.x);
    }

    private Vertex getVertex(BMesh bmesh, ExactHashDeduplication dedup, Vector2f v) {
        Vector2f pos = new Vector2f(v);
        if(isInvalid(pos)) {
            pos.set(-50, -50);
        }
        return dedup.getOrCreateVertex(bmesh, coordSys.unproject(pos));
    }

    private void straightSkeletonVis_addEdge(BMesh bmesh, ExactHashDeduplication dedup, Set<SkeletonNode> nodesDone, SkeletonNode src, SkeletonNode.EdgeType edgeType) {
        if(!nodesDone.add(src)) {
            //System.out.println("straightSkeletonVis: node duplicate at " + coordSys.unproject(src.p));
            return;
        }

        Vertex v0 = getVertex(bmesh, dedup, src.p);
        for(Map.Entry<SkeletonNode, SkeletonNode.EdgeType> entry : src.outgoingEdges.entrySet()) {
            SkeletonNode target = entry.getKey();
            if(entry.getValue() == edgeType) {
                Vertex v1 = getVertex(bmesh, dedup, target.p);
                if(v0 != v1 && v0.getEdgeTo(v1) == null)
                    bmesh.createEdge(v0, v1);
            }

            straightSkeletonVis_addEdge(bmesh, dedup, nodesDone, target, edgeType);
        }
    }


    public BMesh createMovingNodesVis() {
        BMesh bmesh = new BMesh();
        Set<MovingNode> nodesRemaining = new HashSet<>(ctx.getNodes());

        while(!nodesRemaining.isEmpty()) {
            Optional<MovingNode> any = nodesRemaining.stream().findAny();
            createMovingNodesVis(bmesh, any.get(), nodesRemaining);
        }

        return bmesh;
    }

    private void createMovingNodesVis(BMesh bmesh, MovingNode startNode, Set<MovingNode> nodesRemaining) {
        List<Vertex> vertices = new ArrayList<>();

        MovingNode current = startNode;
        do {
            Vertex v = bmesh.createVertex( coordSys.unproject(current.skelNode.p) );
            vertices.add(v);

            nodesRemaining.remove(current);
            current = current.next;
        } while(current != startNode);

        for(int i=0; i<vertices.size(); ++i) {
            int nextIndex = (i+1) % vertices.size();
            bmesh.createEdge(vertices.get(i), vertices.get(nextIndex));
        }
    }


    public List<VisNode> getMovingNodes() {
        List<VisNode> nodes = new ArrayList<>();
        for(MovingNode movingNode : ctx.getNodes()) {
            VisNode node = new VisNode(movingNode.id);
            coordSys.unproject(movingNode.skelNode.p, node.pos);
            nodes.add(node);
        }
        return nodes;
    }


    public BMesh createBisectorVis() {
        BMesh bmesh = new BMesh();

        for(MovingNode movingNode : ctx.getNodes()) {
            Vector2f p0 = movingNode.skelNode.p;
            Vector2f p1 = movingNode.bisector.mult(0.33f * ctx.distanceSign).addLocal(p0);

            Vertex v0 = bmesh.createVertex( coordSys.unproject(p0) );
            Vertex v1 = bmesh.createVertex( coordSys.unproject(p1) );

            bmesh.createEdge(v0, v1);
        }

        return bmesh;
    }


    public BMesh createMappingVis() {
        BMesh bmesh = new BMesh();

        List<SkeletonNode> targets = new ArrayList<>();

        for(SkeletonNode initial : initialNodes) {
            targets.clear();
            followGraph(initial, targets);

            Vertex v0 = bmesh.createVertex( coordSys.unproject(initial.p) );
            for(SkeletonNode target : targets) {
                Vertex v1 = bmesh.createVertex( coordSys.unproject(target.p) );
                bmesh.createEdge(v0, v1);
            }
        }

        return bmesh;
    }

    private void followGraph(SkeletonNode node, List<SkeletonNode> targets) {
        int numMappingEdges = 0;
        for(Map.Entry<SkeletonNode, SkeletonNode.EdgeType> entry : node.outgoingEdges.entrySet()) {
            if(entry.getValue() == SkeletonNode.EdgeType.Mapping) {
                followGraph(entry.getKey(), targets);
                numMappingEdges++;
            } /*else {
                targets.add(entry.getKey());
            }*/
        }

        if(numMappingEdges == 0) {
            targets.add(node);
        }
    }
}
