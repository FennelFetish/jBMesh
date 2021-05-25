package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;

public class VertexOps {
    private final BMesh bmesh;
    private final Vec3Attribute<Vertex> positions;


    public VertexOps(BMesh bmesh) {
        this.bmesh = bmesh;
        positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
    }


    public void move(Vertex vertex, Vector3f distance) {
        move(vertex, distance.x, distance.y, distance.z);
    }

    public void move(Vertex vertex, float dx, float dy, float dz) {
        positions.modify(vertex, v -> {
            v.x += dx;
            v.y += dy;
            v.z += dz;
        });
    }



    // Sorting is not possible in non-manifolds. And in manifolds the sorted edges can be retrieved by traversing only the loops.
    /*public void sortEdges(Vertex vertex) {
        sortEdges(vertex, new VertexEdgeComparator(vertex));
    }

    public void sortEdges(Vertex vertex, Comparator<Edge> comparator) {

    }


    private class VertexEdgeComparator implements Comparator<Edge> {
        private final Vertex vertex;
        private final Vector3f p = new Vector3f();
        private final Vector3f p1 = new Vector3f();
        private final Vector3f p2 = new Vector3f();

        public VertexEdgeComparator(Vertex vertex) {
            this.vertex = vertex;
            positions.get(vertex, p);
        }

        @Override
        public int compare(Edge edge1, Edge edge2) {
            positions.get(edge1.getOther(vertex), p1);
            positions.get(edge2.getOther(vertex), p2);
            p1.subtractLocal(p);
            p2.subtractLocal(p);

            // cross with smoothed normal?
            return 0;
        }
    }*/
}
