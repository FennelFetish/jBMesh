package meshlib.conversion;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import meshlib.lookup.GridVertexDeduplication;
import meshlib.lookup.SimpleVertexDeduplication;
import meshlib.lookup.VertexDeduplication;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;


public class MeshConverter {
    // Deduplicates each vertex once per referencing index
    public static BMesh convert(Mesh mesh) {
        BMesh convertedMesh = new BMesh();
        VertexDeduplication dedup = new SimpleVertexDeduplication();

        TriangleExtractor triangleExtractor = new TriangleExtractor(mesh);
        triangleExtractor.process(triangleExtractor.new TriangleLocationVisitor() {
            @Override
            public void visitTriangle(Vector3f p0, Vector3f p1, Vector3f p2) {
                Vertex v0 = dedup.getOrCreateVertex(convertedMesh, p0);
                Vertex v1 = dedup.getOrCreateVertex(convertedMesh, p1);
                Vertex v2 = dedup.getOrCreateVertex(convertedMesh, p2);
                // TODO: Check for degenerate triangles
                convertedMesh.createFace(v0, v1, v2);
            }
        });

        return convertedMesh;
    }


    /**
     * Deduplicates the vertices first, so each vertex is only checked once.
     * @param mesh
     * @return
     */
    public static BMesh convert2(Mesh mesh) {
        final int numIndices = mesh.getBuffer(VertexBuffer.Type.Index).getNumElements();
        Vertex[] indexMap = new Vertex[numIndices];

        BMesh bmesh = new BMesh();
        TriangleExtractor triangleExtractor = new TriangleExtractor(mesh);
        VertexDeduplication dedup = new GridVertexDeduplication();
        Vector3f location = new Vector3f();

        for(int i=0; i<numIndices; ++i) {
            triangleExtractor.getVertex(i, location);
            indexMap[i] = dedup.getOrCreateVertex(bmesh, location);
        }
        
        triangleExtractor.process((int i0, int i1, int i2) -> {
            // TODO: Check for degenerate triangles
            bmesh.createFace(indexMap[i0], indexMap[i1], indexMap[i2]);
        });

        return bmesh;
    }
}
