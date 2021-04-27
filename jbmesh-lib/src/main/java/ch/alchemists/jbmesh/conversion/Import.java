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
    private static final float RANGE = 0.01f;

    
    public static BMesh convertSimple(Mesh mesh) {
        BMesh bmesh = new BMesh();
        return convert(bmesh, mesh, new SimpleVertexDeduplication(bmesh, RANGE));
    }

    public static BMesh convertGrid(Mesh mesh) {
        BMesh bmesh = new BMesh();
        return convert(bmesh, mesh, new GridVertexDeduplication(bmesh, RANGE));
    }

    public static BMesh convertOptimizedGrid(Mesh mesh) {
        BMesh bmesh = new BMesh();
        return convert(bmesh, mesh, new OptimizedGridDeduplication(bmesh, RANGE));
    }


    public static BMesh convertSimpleMapped(Mesh mesh) {
        BMesh bmesh = new BMesh();
        return convertMapped(bmesh, mesh, new SimpleVertexDeduplication(bmesh, RANGE));
    }

    public static BMesh convertExactMapped(Mesh mesh) {
        BMesh bmesh = new BMesh();
        return convertMapped(bmesh, mesh, new ExactHashDeduplication(bmesh));
    }

    public static BMesh convertGridMapped(Mesh mesh) {
        BMesh bmesh = new BMesh();
        return convertMapped(bmesh, mesh, new GridVertexDeduplication(bmesh, RANGE));
    }

    public static BMesh convertOptimizedGridMapped(Mesh mesh) {
        BMesh bmesh = new BMesh();
        return convertMapped(bmesh, mesh, new OptimizedGridDeduplication(bmesh, RANGE));
    }


    private static BMesh convert(BMesh bmesh, Mesh inputMesh, VertexDeduplication dedup) {
        TriangleExtractor triangleExtractor = new TriangleExtractor(inputMesh);

        bmesh.vertices().ensureCapacity(triangleExtractor.getNumIndices());
        bmesh.edges().ensureCapacity(triangleExtractor.getNumIndices());
        bmesh.faces().ensureCapacity(triangleExtractor.getNumIndices() / 3);
        bmesh.loops().ensureCapacity(triangleExtractor.getNumIndices());

        triangleExtractor.process(triangleExtractor.new TriangleLocationVisitor() {
            @Override
            public void visitTriangle(Vector3f p0, Vector3f p1, Vector3f p2) {
                Vertex v0 = dedup.getOrCreateVertex(p0);
                Vertex v1 = dedup.getOrCreateVertex(p1);
                Vertex v2 = dedup.getOrCreateVertex(p2);
                
                // Check for degenerate triangles
                if(v0 != v1 && v0 != v2 && v1 != v2)
                    bmesh.createFace(v0, v1, v2);
            }
        });

        return bmesh;
    }


    /**
     * Deduplicates the vertices first, so each vertex is only checked once.
     * @param bmesh
     * @return
     */
    private static BMesh convertMapped(BMesh bmesh, Mesh inputMesh, VertexDeduplication dedup) {
        TriangleExtractor triangleExtractor = new TriangleExtractor(inputMesh);
        bmesh.vertices().ensureCapacity(triangleExtractor.getNumIndices());
        bmesh.edges().ensureCapacity(triangleExtractor.getNumIndices());
        bmesh.faces().ensureCapacity(triangleExtractor.getNumIndices() / 3);
        bmesh.loops().ensureCapacity(triangleExtractor.getNumIndices());

        // TODO: Keep duplicated vertices in LoopVertex property?
        Vertex[] indexMap = new Vertex[triangleExtractor.getNumIndices()];
        Vector3f location = new Vector3f();

        for(int i=0; i<indexMap.length; ++i) {
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


    public static BMesh convertSortMapped(Mesh inputMesh) {
        TriangleExtractor triangleExtractor = new TriangleExtractor(inputMesh);

        BMesh bmesh = new BMesh();
        bmesh.vertices().ensureCapacity(triangleExtractor.getNumIndices());
        bmesh.edges().ensureCapacity(triangleExtractor.getNumIndices());
        bmesh.faces().ensureCapacity(triangleExtractor.getNumIndices() / 3);
        bmesh.loops().ensureCapacity(triangleExtractor.getNumIndices());

        SortedVertexDeduplication dedup = new SortedVertexDeduplication(bmesh, RANGE);
        Vector3f location = new Vector3f();
        for(int i=0; i<triangleExtractor.getNumIndices(); ++i) {
            int vertexIndex = triangleExtractor.getIndex(i);
            triangleExtractor.getVertex(vertexIndex, location);
            dedup.add(vertexIndex, location);
        }

        int numLocations = dedup.map();
        //System.out.println("Reduced vertex count from " + triangleExtractor.getNumVertices() + " to " + bmesh.vertices().size() + " (#locations: " + numLocations + ")");

        triangleExtractor.process((int i0, int i1, int i2) -> {
            Vertex v0 = dedup.getVertex(i0);
            Vertex v1 = dedup.getVertex(i1);
            Vertex v2 = dedup.getVertex(i2);

            // Check for existing faces - WRONG!
            //if(v0.getEdgeTo(v1) != null && v1.getEdgeTo(v2) != null && v2.getEdgeTo(v0) != null) {
                // Need to check for existing loops.....
            //    return;
            //}

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
