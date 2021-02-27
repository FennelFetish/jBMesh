package ch.alchemists.jbmesh.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ch.alchemists.jbmesh.structure.*;

// TODO: Should be done only through euler operators but this seems unnecessarily difficult in this case (when considering non-manifold T-structures for example).
//       Maybe turn this into an euler operator itself?
public class CollapseEdge {
    private static final class FaceVertices {
        public final List<Vertex> vertices = new ArrayList<>(4);
    }

    private final BMesh bmesh;


    public CollapseEdge(BMesh bmesh) {
        this.bmesh = bmesh;
    }


    public void apply(Edge edge) {
        apply(bmesh, edge);
    }



    /**
     * edge.vertex0 will remain, edge.vertex1 will be removed
     * @param bmesh
     * @param edge
     */
    public static void apply(BMesh bmesh, Edge edge) {
        Vertex v0 = edge.vertex0; // remains
        Vertex v1 = edge.vertex1; // removed

        // Get faces which are adjacent to 'v1', which are not triangles and won't degenerate.
        // Avoid duplicates (a face may have multiple edges that touch 'v1').
        Map<Face, FaceVertices> adjacentFaces = new HashMap<>();
        List<Edge> adjacentEdges = new ArrayList<>(4);

        for(Edge diskEdge : v1.edges()) {
            adjacentEdges.add(diskEdge);

            for(Loop radialLoop : diskEdge.loops()) {
                Face face = radialLoop.face;
                if(adjacentFaces.containsKey(face))
                    continue;

                FaceVertices faceVertices = new FaceVertices(); // TODO: Make ArrayList instead of object
                adjacentFaces.put(face, faceVertices);

                for(Loop faceLoop : face.loops()) {
                    // Map 'v1' => 'v0'
                    Vertex vertex = (faceLoop.vertex == v1) ? v0 : faceLoop.vertex;
                    faceVertices.vertices.add(vertex);
                }
            }
        }

        // Remove edges adjacent to 'v1'
        for(Edge adjacentEdge : adjacentEdges) {
            bmesh.removeEdge(adjacentEdge);
        }

        // Rebuild faces
        for(FaceVertices faceVertices : adjacentFaces.values()) {
            // Remove degenerate edges (where it was collapsed)
            Vertex last = faceVertices.vertices.get(faceVertices.vertices.size()-1);
            for(int i=0; i<faceVertices.vertices.size(); ++i) {
                Vertex current = faceVertices.vertices.get(i);
                if(last == current) {
                    faceVertices.vertices.remove(i);
                    break;
                }

                last = current;
            }

            if(faceVertices.vertices.size() >= 3)
                bmesh.createFace(faceVertices.vertices.toArray(new Vertex[faceVertices.vertices.size()]));
        }
    }





    /**
     * edge.vertex0 will remain, edge.vertex1 will be removed
     * @param bmesh
     * @param edge
     */
    /*public static void apply(BMesh bmesh, Edge edge) {
        Vertex v0 = edge.vertex0;
        Vertex v1 = edge.vertex1;

        // Edge either needs to be moved (adjacent face #verts > 3) or deleted (adjacent face = triangle, will degenerate to line with 2 collinear edges)

        // Make list of incoming Loops and outgoing Loops at vertex1.
        // They need to be reconnected from vertex1 to vertex0.
        List<Loop> v1Outgoing = new ArrayList<>();
        List<Loop> v1Incoming = new ArrayList<>();

        for(Edge v1Edge : v1.edges()) {
            for(Loop loop : v1Edge.loops()) {
                if(loop.vertex == v1)
                    loop.vertex = v0;
            }
        }
    }*/


    /**
     * edge.vertex0 will remain, edge.vertex1 will be removed
     * @param bmesh
     * @param edge
     */
    /*public static void apply(BMesh bmesh, Edge edge) {
        List<Face> adjacentFaces = new ArrayList<>(2);
        for(Loop radialLoop : edge.loops())
            adjacentFaces.add(radialLoop.face);

        Vertex v0 = edge.vertex0;
        Vertex v1 = edge.vertex1;

        // Relink edges from v1 to v0
        for(Edge relinkEdge : v1.edges()) {
            if(relinkEdge == edge)
                continue;


        }

        boolean success = bmesh.joinEdge(edge, v1);
        assert success;

        // Check for degenerate faces (num vertices < 3)
    }*/
}
