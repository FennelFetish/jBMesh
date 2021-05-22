package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.property.ObjectProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Edge;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.util.List;

public class TriangleExport extends Export<Loop> {
    private final ObjectProperty<Loop, Vertex> propLoopVertex;
    private final TriangleIndices triangleIndices;


    public TriangleExport(BMesh bmesh) {
        super(bmesh);

        Vec3Property<Vertex> propPosition = Vec3Property.get(Vertex.Position, bmesh.vertices());
        addVertexProperty(VertexBuffer.Type.Position, propPosition);

        // TODO: Do this somewhere else. Default JME property config?
        Vec3Property<Loop> propLoopNormals = Vec3Property.get(Loop.Normal, bmesh.loops());
        if(propLoopNormals != null) {
            Vec3Property<Vertex> propVertexNormals = Vec3Property.getOrCreate(Vertex.Normal, bmesh.vertices());
            addPropertyMapping(VertexBuffer.Type.Normal, propLoopNormals, propVertexNormals);
        }

        propLoopVertex = ObjectProperty.getOrCreate(Loop.VertexMap, bmesh.loops(), Vertex[].class, Vertex[]::new);
        propLoopVertex.setComparable(false);

        triangleIndices = new TriangleIndices(bmesh, propLoopVertex);

        outputMesh.setMode(Mesh.Mode.Triangles);
    }


    public static Mesh apply(BMesh bmesh) {
        TriangleExport export = new TriangleExport(bmesh);
        return export.update();
    }


    @Override
    protected void setIndexBuffer() {
        triangleIndices.apply();
        triangleIndices.update(); // Requires duplication / existing LoopVertex property

        outputMesh.setBuffer(triangleIndices.getIndexBuffer());
    }


    @Override
    protected void getVertexNeighborhood(Vertex vertex, List<Loop> dest) {
        for(Edge edge : vertex.edges()) {
            for(Loop loop : edge.loops()) {
                if(loop.vertex == vertex)
                    dest.add(loop);
            }
        }
    }


    @Override
    protected void setVertexReference(Vertex contactPoint, Loop element, Vertex ref) {
        propLoopVertex.set(element, ref);
    }

    @Override
    protected Vertex getVertexReference(Vertex contactPoint, Loop element) {
        return propLoopVertex.get(element);
    }
}
