package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.lookup.*;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;

// https://www.researchgate.net/publication/4070748_Efficient_topology_construction_from_triangle_soup
// Instead:
// - Add all vertices to VertexDeduplication first
// - That will create mapping
//
// TODO? Selection (Box, Sphere...)/Views: Convert only parts of a mesh if only that part needs to be worked with.
//       Constrain operations to that part. Insert locked elements in BMeshData so the data is not used/overriden?
public class Import {
    private static final float DEFAULT_EPSILON = 0.01f;


    public static BMesh convert(Mesh mesh) {
        return convert(mesh, DEFAULT_EPSILON);
    }

    public static BMesh convert(Mesh mesh, float epsilon) {
        BMesh bmesh = new BMesh();
        return convert(mesh, bmesh, new HashGridDeduplication(bmesh, epsilon));
    }


    public static BMesh convertExact(Mesh mesh) {
        BMesh bmesh = new BMesh();
        return convert(mesh, bmesh, new ExactHashDeduplication(bmesh));
    }


    /**
     * Deduplicates the vertices first, so each vertex is only checked once.
     * @param bmesh
     * @return
     */
    private static BMesh convert(Mesh inputMesh, BMesh bmesh, VertexDeduplication dedup) {
        TriangleExtractor triangleExtractor = new TriangleExtractor(inputMesh);
        final int numIndices = triangleExtractor.getNumIndices();

        bmesh.vertices().ensureCapacity(numIndices);
        bmesh.edges().ensureCapacity(numIndices);
        bmesh.faces().ensureCapacity(numIndices / 3);
        bmesh.loops().ensureCapacity(numIndices);

        // TODO: Keep duplicated vertices in LoopVertex attribute?
        Vertex[] indexMap = new Vertex[numIndices];
        Vector3f location = new Vector3f();

        for(int i=0; i<numIndices; ++i) {
            int vertexIndex = triangleExtractor.getIndex(i);
            triangleExtractor.getVertex(vertexIndex, location);
            indexMap[vertexIndex] = dedup.getOrCreateVertex(location);
        }

        //System.out.println("Reduced vertex count from " + triangleExtractor.getNumVertices() + " to " + bmesh.vertices().size());

        triangleExtractor.process((int i0, int i1, int i2) -> {
            Vertex v0 = indexMap[i0];
            Vertex v1 = indexMap[i1];
            Vertex v2 = indexMap[i2];

            // Check for degenerate triangles
            if(v0 != v1 && v0 != v2 && v1 != v2)
                bmesh.createFace(v0, v1, v2);
        });

        return bmesh;
    }


    public static BMesh importKeep(Mesh inputMesh) {
        // Keep normals
        // Keep duplication: Create virtual vertices for index targets with multiple uses
        // Keep triangulation: Indices -> Create Triangle objects in Triangulate
        // Copy and reuse arrays from buffer
        return null;
    }
}
