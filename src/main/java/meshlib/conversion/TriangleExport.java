package meshlib.conversion;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.util.List;
import meshlib.data.BMeshProperty;
import meshlib.data.property.ColorProperty;
import meshlib.data.property.ObjectProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Edge;
import meshlib.structure.Loop;
import meshlib.structure.Vertex;

public class TriangleExport extends Export<Loop> {
    private final ObjectProperty<Loop, Vertex> propLoopVertex;
    private final TriangleIndices triangleIndices;


    public TriangleExport(BMesh bmesh) {
        this(bmesh, new LoopDuplicationStrategy(bmesh));
    }

    public TriangleExport(BMesh bmesh, DuplicationStrategy<Loop> duplicationStrategy) {
        super(bmesh, duplicationStrategy);

        ObjectProperty<Loop, Vertex> propLoopVertex = ObjectProperty.get(BMeshProperty.Loop.VERTEX_MAP, Vertex[].class, bmesh.loops());
        if(propLoopVertex == null) {
            propLoopVertex = new ObjectProperty<>(BMeshProperty.Loop.VERTEX_MAP, Vertex[]::new);
            bmesh.loops().addProperty(propLoopVertex);
        }
        propLoopVertex.setComparable(false);
        this.propLoopVertex = propLoopVertex;

        triangleIndices = new TriangleIndices(bmesh, propLoopVertex);

        outputMesh.setMode(Mesh.Mode.Triangles);
    }


    @Override
    protected void updateOutputMesh() {
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


    public static class LoopDuplicationStrategy implements DuplicationStrategy<Loop> {
        private final BMesh bmesh;

        private final Vec3Property<Vertex> propPosition;
        private final Vec3Property<Loop> propLoopNormal;
        private final Vec3Property<Vertex> propVertexNormal;

        private final Vector3f tempNormal = new Vector3f();

        public LoopDuplicationStrategy(BMesh bmesh) {
            this.bmesh = bmesh;

            propPosition     = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
            propLoopNormal   = Vec3Property.get(BMeshProperty.Loop.NORMAL, bmesh.loops());
            propVertexNormal = Vec3Property.getOrCreate(BMeshProperty.Vertex.NORMAL, bmesh.vertices());
        }

        @Override
        public boolean splitVertex(Loop a, Loop b) {
            return bmesh.loops().equals(a, b);
        }

        @Override
        public void copyProperties(Loop src, Vertex dest) {
            propLoopNormal.get(src, tempNormal);
            propVertexNormal.set(dest, tempNormal);

            // TODO: Colors
        }

        @Override
        public void setBuffers(Mesh outputMesh) {
            // TODO: Reuse buffers
            outputMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(propPosition.array()));
            outputMesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(propVertexNormal.array()));

            ColorProperty<Vertex> propVertexColor = ColorProperty.get(BMeshProperty.Vertex.COLOR, bmesh.vertices());
            if(propVertexColor != null)
                outputMesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(propVertexColor.array()));
        }
    }
}
