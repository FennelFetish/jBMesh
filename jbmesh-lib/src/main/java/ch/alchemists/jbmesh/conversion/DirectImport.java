package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.property.ObjectProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.lookup.ExactHashDeduplication;
import ch.alchemists.jbmesh.lookup.VertexDeduplication;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;

public class DirectImport {
    public static BMesh importTriangles(Mesh inputMesh) {
        BMesh bmesh = new BMesh();

        TriangleExtractor triangleExtractor = new TriangleExtractor(inputMesh);
        final int numVertices = triangleExtractor.getNumVertices();
        final int numIndices = triangleExtractor.getNumIndices();

        bmesh.edges().ensureCapacity(numIndices);
        bmesh.faces().ensureCapacity(numIndices / 3);
        bmesh.loops().ensureCapacity(numIndices);

        Vec3Property<Vertex> propPosition = replacePositionData(bmesh, numVertices, triangleExtractor.getPositionArray());

        // Detect Vertex duplicates and create mapping: Virtual vertex => Actual vertex in the structure
        Vertex[] virtualVertexMap = createVertices(bmesh, triangleExtractor, numVertices);

        ObjectProperty<Loop, Vertex> propLoopVertex = new ObjectProperty<>(Loop.VertexMap, Vertex[]::new);
        bmesh.loops().addProperty(propLoopVertex);

        triangleExtractor.process((int i0, int i1, int i2) -> {
            Vertex v0 = virtualVertexMap[i0];
            Vertex v1 = virtualVertexMap[i1];
            Vertex v2 = virtualVertexMap[i2];

            // Check for degenerate triangles
            if(v0 != v1 && v0 != v2 && v1 != v2) {
                Face face = bmesh.createFace(v0, v1, v2);
                propLoopVertex.set(face.loop, bmesh.vertices().get(i0));
                propLoopVertex.set(face.loop.nextFaceLoop, bmesh.vertices().get(i1));
                propLoopVertex.set(face.loop.prevFaceLoop, bmesh.vertices().get(i2));
            }

            // TODO: Also set normals, tex coords etc.
        });

        // TODO: Add triangles to triangulation? And set existing index array

        assert propPosition.array() == triangleExtractor.getPositionArray();
        return bmesh;
    }


    private static Vec3Property<Vertex> replacePositionData(BMesh bmesh, int arrayLength, float[] data) {
        Vec3Property<Vertex> propPosition = Vec3Property.get(Vertex.Position, bmesh.vertices());
        bmesh.vertices().removeProperty(propPosition);
        bmesh.vertices().compactData();
        bmesh.vertices().ensureCapacity(arrayLength);
        bmesh.vertices().addProperty(propPosition, data);
        return propPosition;
    }


    private static Vertex[] createVertices(BMesh bmesh, TriangleExtractor triangleExtractor, int numVertices) {
        Vertex[] virtualVertexMap = new Vertex[numVertices];
        VertexDeduplication dedup = new ExactHashDeduplication(bmesh);
        Vector3f p = new Vector3f();

        for(int i=0; i<numVertices; ++i) {
            triangleExtractor.getVertex(i, p);

            Vertex v = dedup.getVertex(p);
            if(v == null) {
                v = bmesh.createVertex(p);
                dedup.addExisting(v);
            }
            else
                bmesh.vertices().createVirtual();

            virtualVertexMap[i] = v;
        }

        //System.out.println("Reduced vertex count from " + triangleExtractor.getNumVertices() + " to " + bmesh.vertices().size());
        //System.out.println("Num virtual vertices: " + (bmesh.vertices().totalSize()-bmesh.vertices().size()));
        return virtualVertexMap;
    }


    /*private static BMesh importLines(Mesh inputMesh) {
        return null;
    }*/
}
