package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshProperty;
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
import com.jme3.scene.VertexBuffer;
import java.util.ArrayList;
import java.util.List;

public class DirectImport {
    public interface VertexDeduplicationFactory {
        VertexDeduplication createVertexDeduplication(BMesh bmesh);
    }


    private static class PropertyMapping {
        public final BMeshProperty<Vertex, ?> vertexProperty;
        public final BMeshProperty<Loop, ?> loopProperty;

        public PropertyMapping(BMeshProperty<Vertex, ?> vertexProperty, BMeshProperty<Loop, ?> loopProperty) {
            this.vertexProperty = vertexProperty;
            this.loopProperty = loopProperty;
        }
    }


    private final Mesh inputMesh;
    private final List<PropertyMapping> propertyMappings = new ArrayList<>();


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

        Vec3Property<Vertex> propPosition = replacePositionData(bmesh, numVertices, triangleExtractor.getPositionArray());
        createVertexProperties(bmesh, numVertices);

        // Detect Vertex duplicates and create mapping: [Virtual vertex] => [Actual vertex] in the structure
        VertexDeduplication dedup = dedupFactory.createVertexDeduplication(bmesh);
        Vertex[] virtualVertexMap = createVertices(bmesh, triangleExtractor, dedup); // TODO: should createVerticess() come before dealing with properties?
        boolean hasVirtual = (virtualVertexMap.length > bmesh.vertices().size());

        ObjectProperty<Loop, Vertex> propLoopVertex = new ObjectProperty<>(Loop.VertexMap, Vertex[]::new);
        bmesh.loops().addProperty(propLoopVertex);

        triangleExtractor.process((int i0, int i1, int i2) -> {
            Vertex v0 = virtualVertexMap[i0];
            Vertex v1 = virtualVertexMap[i1];
            Vertex v2 = virtualVertexMap[i2];

            // Check for degenerate triangles
            if(v0 != v1 && v0 != v2 && v1 != v2) {
                Face face = bmesh.createFace(v0, v1, v2);
                propLoopVertex.set(face.loop,              bmesh.vertices().get(i0));
                propLoopVertex.set(face.loop.nextFaceLoop, bmesh.vertices().get(i1));
                propLoopVertex.set(face.loop.prevFaceLoop, bmesh.vertices().get(i2));
            }
        });

        copyPropertiesToLoops(bmesh, propLoopVertex);

        // TODO: Add triangles to TriangleIndices? And set existing index array

        assert propPosition.array() == triangleExtractor.getPositionArray();
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


    private void createVertexProperties(BMesh bmesh, int numVertices) {
        for(VertexBuffer buffer : inputMesh.getBufferList()) {
            switch(buffer.getBufferType()) {
                case Index:
                    // --> Property for Loops
                    break;

                case Position:
                    // --> We already have that
                    break;

                default: {
                    BMeshProperty<Vertex, ?> vertexProperty = VertexBufferUtils.createProperty(buffer, Vertex.class);
                    VertexBufferUtils.setData(bmesh.vertices(), buffer, vertexProperty);

                    BMeshProperty<Loop, ?> loopProperty = VertexBufferUtils.createProperty(buffer, Loop.class);
                    bmesh.loops().addProperty(loopProperty);
                    propertyMappings.add(new PropertyMapping(vertexProperty, loopProperty));
                }
            }
        }
    }


    // Keep Vertex.Position property because BMesh hold a reference to the original instance.
    private Vec3Property<Vertex> replacePositionData(BMesh bmesh, int arrayLength, float[] data) {
        Vec3Property<Vertex> propPosition = Vec3Property.get(Vertex.Position, bmesh.vertices());
        //bmesh.vertices().removeProperty(propPosition);
        bmesh.vertices().clearProperties();
        bmesh.vertices().compactData();
        bmesh.vertices().ensureCapacity(arrayLength);
        bmesh.vertices().addProperty(propPosition, data);
        return propPosition;
    }


    private void copyPropertiesToLoops(BMesh bmesh, ObjectProperty<Loop, Vertex> propLoopVertex) {
        // TODO: Transfer properties from Vertex to Loop
        // TODO: Check if that's even necessary? (Only necessary if there are virtual vertices?)
        // TODO: We need  a) A function to clone BMeshProperty for creating properties for the loops
        //                b) A function for copying values from one array to another property-array

        for(PropertyMapping mapping : propertyMappings) {
            for(Loop loop : bmesh.loops()) {
                Vertex vertex = propLoopVertex.get(loop);
                mapping.vertexProperty.copy(vertex, mapping.loopProperty, loop);
            }
        }
    }


    /*private static BMesh importLines(Mesh inputMesh) {
        return null;
    }*/
}
