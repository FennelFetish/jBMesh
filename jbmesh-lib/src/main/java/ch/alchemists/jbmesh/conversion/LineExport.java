package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.ObjectTupleAttribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Edge;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.util.List;

public class LineExport extends Export<Edge> {
    private final ObjectTupleAttribute<Edge, Vertex> attrEdgeVertex;
    private final Indices<Edge> indices;


    public LineExport(BMesh bmesh) {
        super(bmesh);

        useVertexAttribute(VertexBuffer.Type.Position, BMeshAttribute.Position);

        attrEdgeVertex = ObjectTupleAttribute.getOrCreate(BMeshAttribute.VertexMap, 2, bmesh.edges(), Vertex[].class, Vertex[]::new);
        attrEdgeVertex.setComparable(false);

        indices = new Indices<>(bmesh.edges(), 2);
        outputMesh.setMode(Mesh.Mode.Lines);
    }


    public static Mesh apply(BMesh bmesh) {
        LineExport export = new LineExport(bmesh);
        return export.update();
    }


    @Override
    protected void setIndexBuffer() {
        int maxVertexIndex = bmesh.vertices().totalSize()-1;
        indices.prepare(maxVertexIndex);

        indices.updateIndices((Edge edge, int[] indices) -> {
            indices[0] = attrEdgeVertex.get(edge, 0).getIndex();
            indices[1] = attrEdgeVertex.get(edge, 1).getIndex();
        });

        indices.applyIndexBuffer(outputMesh);
    }


    @Override
    protected void getVertexNeighborhood(Vertex vertex, List<Edge> dest) {
        for(Edge edge : vertex.edges())
            dest.add(edge);
    }


    @Override
    protected void setVertexReference(Vertex contactPoint, Edge element, Vertex ref) {
        if(element.vertex0 == contactPoint)
            attrEdgeVertex.set(element, 0, ref);
        else {
            assert element.vertex1 == contactPoint;
            attrEdgeVertex.set(element, 1, ref);
        }
    }


    @Override
    protected Vertex getVertexReference(Vertex contactPoint, Edge element) {
        if(element.vertex0 == contactPoint)
            return attrEdgeVertex.get(element, 0);
        else {
            assert element.vertex1 == contactPoint;
            return attrEdgeVertex.get(element, 1);
        }
    }
}
