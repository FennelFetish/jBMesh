package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.ObjectAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.lookup.ExactHashDeduplication;
import ch.alchemists.jbmesh.lookup.VertexDeduplication;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.util.ArrayList;
import java.util.List;

public class DirectImport {
    public interface VertexDeduplicationFactory {
        VertexDeduplication createVertexDeduplication(BMesh bmesh);
    }


    private static class AttributeMapping {
        public final BMeshAttribute<Vertex, ?> vertexAttribute;
        public final BMeshAttribute<Loop, ?> loopAttribute;

        public AttributeMapping(BMeshAttribute<Vertex, ?> vertexAttribute, BMeshAttribute<Loop, ?> loopAttribute) {
            this.vertexAttribute = vertexAttribute;
            this.loopAttribute = loopAttribute;
        }
    }


    private final Mesh inputMesh;
    private final List<AttributeMapping> mappedAttributes = new ArrayList<>();


    public DirectImport(Mesh inputMesh) {
        this.inputMesh = inputMesh;
    }


    public static BMesh importTriangles(Mesh mesh) {
        DirectImport directImport = new DirectImport(mesh);
        return directImport.importTriangles();
    }


    public BMesh importTriangles() {
        return importTriangles(ExactHashDeduplication::new);
    }

    public BMesh importTriangles(VertexDeduplicationFactory dedupFactory) {
        BMesh bmesh = new BMesh();

        TriangleExtractor triangleExtractor = new TriangleExtractor(inputMesh);
        final int numVertices = triangleExtractor.getNumVertices();
        final int numIndices = triangleExtractor.getNumIndices();

        bmesh.edges().ensureCapacity(numIndices);
        bmesh.faces().ensureCapacity(numIndices / 3);
        bmesh.loops().ensureCapacity(numIndices);

        Vec3Attribute<Vertex> attrPosition = replacePositionData(bmesh, numVertices, triangleExtractor.getPositionArray());
        createVertexAttributes(bmesh, numVertices);

        // Detect Vertex duplicates and create mapping: [Virtual vertex] => [Actual vertex] in the structure
        VertexDeduplication dedup = dedupFactory.createVertexDeduplication(bmesh);
        Vertex[] virtualVertexMap = createVertices(bmesh, triangleExtractor, dedup); // TODO: should createVerticess() come before dealing with attributes?
        boolean hasVirtual = (virtualVertexMap.length > bmesh.vertices().size());

        ObjectAttribute<Loop, Vertex> attrLoopVertex = new ObjectAttribute<>(Loop.VertexMap, Vertex[]::new);
        bmesh.loops().addAttribute(attrLoopVertex);

        triangleExtractor.process((int i0, int i1, int i2) -> {
            Vertex v0 = virtualVertexMap[i0];
            Vertex v1 = virtualVertexMap[i1];
            Vertex v2 = virtualVertexMap[i2];

            // Check for degenerate triangles
            if(v0 != v1 && v0 != v2 && v1 != v2) {
                Face face = bmesh.createFace(v0, v1, v2);
                attrLoopVertex.set(face.loop,              bmesh.vertices().get(i0));
                attrLoopVertex.set(face.loop.nextFaceLoop, bmesh.vertices().get(i1));
                attrLoopVertex.set(face.loop.prevFaceLoop, bmesh.vertices().get(i2));
            }
        });

        copyAttributesToLoops(bmesh, attrLoopVertex);

        // TODO: Add triangles to TriangleIndices? And set existing index array

        assert attrPosition.array() == triangleExtractor.getPositionArray();
        return bmesh;
    }


    private Vertex[] createVertices(BMesh bmesh, TriangleExtractor triangleExtractor, VertexDeduplication dedup) {
        final int numVertices = triangleExtractor.getNumVertices();
        Vertex[] virtualVertexMap = new Vertex[numVertices];
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


    private void createVertexAttributes(BMesh bmesh, int numVertices) {
        for(VertexBuffer buffer : inputMesh.getBufferList()) {
            switch(buffer.getBufferType()) {
                case Index:
                    // --> Attribute for Loops
                    break;

                case Position:
                    // --> We already have that
                    break;

                default: {
                    BMeshAttribute<Vertex, ?> vertexAttribute = VertexBufferUtils.createBMeshAttribute(buffer, Vertex.class);
                    VertexBufferUtils.setData(bmesh.vertices(), buffer, vertexAttribute);

                    BMeshAttribute<Loop, ?> loopAttribute = VertexBufferUtils.createBMeshAttribute(buffer, Loop.class);
                    bmesh.loops().addAttribute(loopAttribute);
                    mappedAttributes.add(new AttributeMapping(vertexAttribute, loopAttribute));
                }
            }
        }
    }


    // Keep Position attribute because BMesh holds a reference to the original instance.
    private Vec3Attribute<Vertex> replacePositionData(BMesh bmesh, int arrayLength, float[] data) {
        Vec3Attribute<Vertex> attrPosition = Vec3Attribute.get(Vertex.Position, bmesh.vertices());
        //bmesh.vertices().removeAttribute(attrPosition);
        bmesh.vertices().clearAttributes();
        bmesh.vertices().compactData();
        bmesh.vertices().ensureCapacity(arrayLength);
        bmesh.vertices().addAttribute(attrPosition, data);
        return attrPosition;
    }


    private void copyAttributesToLoops(BMesh bmesh, ObjectAttribute<Loop, Vertex> attrLoopVertex) {
        // TODO: Transfer attributes from Vertex to Loop
        // TODO: Check if that's even necessary? (Only necessary if there are virtual vertices?)

        for(AttributeMapping mapping : mappedAttributes) {
            for(Loop loop : bmesh.loops()) {
                Vertex vertex = attrLoopVertex.get(loop);
                mapping.vertexAttribute.copy(vertex, mapping.loopAttribute, loop);
            }
        }
    }


    /*private static BMesh importLines(Mesh inputMesh) {
        return null;
    }*/
}
