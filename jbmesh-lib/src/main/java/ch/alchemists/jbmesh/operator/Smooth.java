package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.structure.*;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Catmull-Clark Subdivision Surface.
 * Output faces (all quads) not necessarily planar.
 */
public class Smooth {
    // https://en.wikipedia.org/wiki/Catmull%E2%80%93Clark_subdivision_surface
    // https://youtu.be/SPOOZsRQPac?t=116       G1 non-uniform Catmull-Clark surfaces (SIGGRAPH 2016 Presentation)

    // P: Vertex Point: Original face vertices      -> Own Position (15/36) + Opposite points on adjacent edges (6/36 x3) + Remaining points of adjacent faces (1/36 x3)

    // F: Face Point: Center of subdivided Face     -> Avg of original face vertices

    // R: Edge Point: Center of subdivided Edges    -> Own Edge Points (6/16 each) + Opposite vertices of both adjacent faces (1/16 x4)
    //                                              =  Average of edge v0 v1 + adjacent face points

    // n: Number of adjacent edges (= faces)

    // TODO: Somehow subdivide it more at extremal points, like the corners of a smoothed cube?

    private final BMesh bmesh;
    private final EdgeOps edgeOps;
    private final FaceOps faceOps;
    private final Vec3Attribute<Vertex> positions;

    private boolean processNonmanifolds = false;


    public Smooth(BMesh bmesh) {
        this.bmesh = bmesh;

        edgeOps = new EdgeOps(bmesh);
        faceOps = new FaceOps(bmesh);

        positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
    }


    public void setProcessNonmanifolds(boolean enabled) {
        processNonmanifolds = enabled;
    }


    public void apply(List<Face> faces) {
        // TODO: Keep the info for the next iteration?

        Set<Edge> edges = new HashSet<>(faces.size() * 4); // TODO: Instead of Set, use pass nr to ensure each edge is only processed once?
        Set<Vertex> vertexPoints = new HashSet<>(faces.size() * 4); // TODO: Or use pass nr so each edge is added to a list (instead of set) only once.

        for(Face face : faces) {
            for(Loop loop : face.loops()) {
                edges.add(loop.edge);
                vertexPoints.add(loop.vertex);
            }
        }

        // Split edges and remember resulting vertices
        List<Vertex> edgePoints = new ArrayList<>(edges.size());
        Vector3f center = new Vector3f();
        for(Edge edge : edges) {
            edgeOps.calcCenter(edge, center);
            Vertex edgePoint = bmesh.splitEdge(edge); // << Edge Point

            if(isManifold(edge.loop))
                edgePoints.add(edgePoint);

            positions.set(edgePoint, center);
        }

        // Subdivide faces
        ArrayList<Vertex> faceVertices = new ArrayList<>(8);
        for(Face face : faces) {
            faceVertices.clear();
            face.getVertices(faceVertices);
            assert (faceVertices.size() & 1) == 0; // Even number

            // Make sure first vertex in list is an original vertex
            if(!vertexPoints.contains(faceVertices.get(0))) {
                Vertex first = faceVertices.get(0);
                for(int i=1; i<faceVertices.size(); ++i)
                    faceVertices.set(i-1, faceVertices.get(i));
                faceVertices.set(faceVertices.size()-1, first);
            }

            // Build new quads
            faceOps.centroid(face, center); // Calculate earlier, before splitting edges?
            bmesh.removeFace(face);
            Vertex facePoint = bmesh.createVertex(center); // << Face Point

            int lastIndex = faceVertices.size() - 1;
            for(int i=0; i<faceVertices.size(); i+=2) {
                //int nextIndex = (i+1) % faceVertices.size();
                int nextIndex = i+1;

                Vertex v0 = faceVertices.get(lastIndex);
                Vertex v1 = faceVertices.get(i);
                Vertex v2 = faceVertices.get(nextIndex);
                bmesh.createFace(v0, v1, v2, facePoint);

                lastIndex = i+1;
            }
        }

        // Prepare EdgePoint locations
        Vector3f[] edgePointLoc = new Vector3f[edgePoints.size()];
        for(int i=0; i<edgePoints.size(); ++i) {
            Vertex edgePoint = edgePoints.get(i);
            Vector3f p = new Vector3f();

            int count = 0; // 4 in manifolds, less at borders
            for(Edge e : edgePoint.edges()) {
                Vertex other = e.getOther(edgePoint);
                positions.addLocal(p, other);
                count++;
            }

            assert count <= 4;
            p.divideLocal(count);
            edgePointLoc[i] = p;
        }

        // Update VertexPoints
        Vector3f avgFace = new Vector3f();
        Vector3f avgEdge = new Vector3f();
        outer:
        for(Vertex vertexPoint : vertexPoints) {
            Loop loop = vertexPoint.edge.loop;
            if(loop.vertex != vertexPoint)
                loop = loop.nextFaceLoop;
            final Loop startLoop = loop;

            avgFace.zero();
            avgEdge.zero();
            int count = 0;

            do {
                if(!isManifold(loop))
                    continue outer;
                // TODO: In case of non-manifolds we may have to traverse in the other direction
                // TODO: Make iterator for this

                positions.addLocal(avgEdge, loop.nextFaceLoop.vertex);
                positions.addLocal(avgFace, loop.nextFaceLoop.nextFaceLoop.vertex);
                count++;
                loop = loop.nextEdgeLoop.nextFaceLoop;
            } while(loop != startLoop);

            avgFace.divideLocal(count);
            avgEdge.multLocal(2.0f / count);

            Vector3f p = positions.get(vertexPoint);
            p.multLocal(count - 3); // Constant n-3
            p.addLocal(avgFace);
            p.addLocal(avgEdge);
            p.divideLocal(count);

            positions.set(vertexPoint, p);
        }

        // Apply EdgePoint location
        for(int i=0; i<edgePoints.size(); ++i) {
            Vertex edgePoint = edgePoints.get(i);
            positions.set(edgePoint, edgePointLoc[i]);
        }
    }


    private boolean isManifold(Loop loop) {
        if(processNonmanifolds)
            return true;

        // Only process elements at manifold places (exclude borders and T-structures)
        return loop.nextEdgeLoop != loop && loop.nextEdgeLoop.nextEdgeLoop == loop;
    }


    // Make it so subdivision is optional? -> Smooth without adding elements
    public void smooth() {

    }
}
