package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import meshlib.lookup.ExactHashDeduplication;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;
import meshlib.util.PlanarCoordinateSystem;

public class SkeletonVisualization {
    private final PlanarCoordinateSystem coordSys;
    private final ArrayList<SkeletonNode> initialNodes ;
    private final ArrayList<MovingNode> movingNodes;
    private final float distanceSign;


    SkeletonVisualization(PlanarCoordinateSystem coordSys, ArrayList<SkeletonNode> initialNodes, ArrayList<MovingNode> movingNodes, float distanceSign) {
        this.coordSys = coordSys;
        this.initialNodes = initialNodes;
        this.movingNodes = movingNodes;
        this.distanceSign = distanceSign;
    }


    public BMesh createStraightSkeletonVis() {
        BMesh bmesh = new BMesh();
        ExactHashDeduplication dedup = new ExactHashDeduplication(bmesh);
        Set<SkeletonNode> nodesDone = new HashSet<>();

        for(SkeletonNode node : initialNodes) {
            straightSkeletonVis_addEdge(bmesh, dedup, nodesDone, node);
        }

        System.out.println("Straight Skeleton Visualization: " + nodesDone.size() + " unique nodes");
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

    private void straightSkeletonVis_addEdge(BMesh bmesh, ExactHashDeduplication dedup, Set<SkeletonNode> nodesDone, SkeletonNode src) {
        if(!nodesDone.add(src)) {
            //System.out.println("straightSkeletonVis: node duplicate at " + coordSys.unproject(src.p));
            return;
        }

        Vertex v0 = getVertex(bmesh, dedup, src.p);
        for(SkeletonNode target : src.outgoingEdges) {
            Vertex v1 = getVertex(bmesh, dedup, target.p);
            if(v0 != v1 && v0.getEdgeTo(v1) == null)
                bmesh.createEdge(v0, v1);

            straightSkeletonVis_addEdge(bmesh, dedup, nodesDone, target);
        }
    }



    public BMesh createMovingNodesVis() {
        BMesh bmesh = new BMesh();
        if(movingNodes.isEmpty())
            return bmesh;

        List<Vertex> vertices = new ArrayList<>();

        MovingNode start = movingNodes.get(0);
        MovingNode current = start;
        do {
            Vertex v = bmesh.createVertex( coordSys.unproject(current.skelNode.p) );
            vertices.add(v);

            current = current.next;

            if(current == null) {
                System.out.println("NULL in StraightSkeletonNew.createMovingNodesVis()");
                break;
            }
        } while(current != start);

        for(int i=0; i<vertices.size(); ++i) {
            int nextIndex = (i+1) % vertices.size();
            bmesh.createEdge(vertices.get(i), vertices.get(nextIndex));
        }

        return bmesh;
    }

    public List<Vector3f> getMovingNodesPos() {
        List<Vector3f> nodes = new ArrayList<>();
        for(MovingNode movingNode : movingNodes) {
            nodes.add( coordSys.unproject(movingNode.skelNode.p) );
        }
        return nodes;
    }



    public BMesh createBisectorVis() {
        BMesh bmesh = new BMesh();

        for(MovingNode movingNode : movingNodes) {
            Vector2f p0 = movingNode.skelNode.p;
            Vector2f p1 = movingNode.bisector.mult(0.33f * distanceSign).addLocal(p0);

            Vertex v0 = bmesh.createVertex( coordSys.unproject(p0) );
            Vertex v1 = bmesh.createVertex( coordSys.unproject(p1) );

            bmesh.createEdge(v0, v1);
        }

        return bmesh;
    }
}
