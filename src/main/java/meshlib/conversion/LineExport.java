package meshlib.conversion;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.util.List;
import meshlib.data.BMeshProperty;
import meshlib.data.property.IntTupleProperty;
import meshlib.data.property.ObjectTupleProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Edge;
import meshlib.structure.Vertex;

public class LineExport extends Export<Edge> {
    // Edge color property? duplicate vertices where needed

    private final ObjectTupleProperty<Edge, Vertex> propEdgeVertex = new ObjectTupleProperty<>(BMeshProperty.Edge.VERTEX_MAP, 2, Vertex[]::new);
    private final IntTupleProperty<Edge> propEdgeIndices = new IntTupleProperty<>("LineExport_EdgeIndices", 2);


    public LineExport(BMesh bmesh) {
        this(bmesh, new EdgeDuplicationStrategy(bmesh));
    }

    public LineExport(BMesh bmesh, DuplicationStrategy<Edge> duplicationStrategy) {
        super(bmesh, duplicationStrategy);

        propEdgeVertex.setComparable(false);
        bmesh.edges().addProperty(propEdgeVertex);

        propEdgeIndices.setComparable(false);
        bmesh.edges().addProperty(propEdgeIndices);

        outputMesh.setMode(Mesh.Mode.Lines);
    }


    @Override
    protected void updateOutputMesh() {
        // Make index buffer
        for(Edge edge : bmesh.edges()) {
            Vertex v0 = propEdgeVertex.get(edge, 0);
            Vertex v1 = propEdgeVertex.get(edge, 1);
            //propEdgeIndices.set(edge, (short) v0.getIndex(), (short) v1.getIndex()); // TODO: int?
            propEdgeIndices.setValues(edge, v0.getIndex(), v1.getIndex()); // TODO: int?
        }

        outputMesh.setBuffer(VertexBuffer.Type.Index, 2, propEdgeIndices.array());
    }

    @Override
    protected void getVertexNeighborhood(Vertex vertex, List<Edge> dest) {
        for(Edge edge : vertex.edges())
            dest.add(edge);
    }

    @Override
    protected void setVertexReference(Vertex contactPoint, Edge element, Vertex ref) {
        if(element.vertex0 == contactPoint)
            propEdgeVertex.set(element, 0, ref);
        else
            propEdgeVertex.set(element, 1, ref);
    }

    @Override
    protected Vertex getVertexReference(Vertex contactPoint, Edge element) {
        if(element.vertex0 == contactPoint)
            return propEdgeVertex.get(element, 0);
        else
            return propEdgeVertex.get(element, 1);
    }


    public static class EdgeDuplicationStrategy implements DuplicationStrategy<Edge> {
        private final BMesh bmesh;
        private final Vec3Property<Vertex> propPosition;

        public EdgeDuplicationStrategy(BMesh bmesh) {
            this.bmesh = bmesh;
            propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
        }

        @Override
        public boolean splitVertex(Edge a, Edge b) {
            return bmesh.edges().equals(a, b);
        }

        @Override
        public void copyProperties(Edge src, Vertex dest) {
            // TODO: Colors
        }

        @Override
        public void setBuffers(Mesh outputMesh) {
            // TODO: Reuse buffers
            outputMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(propPosition.array()));
        }
    }
}
