package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.data.property.*;
import ch.alchemists.jbmesh.operator.sweeptriang.SweepTriangulation;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.logging.Logger;

public class TriangleIndices {
    private static class Triangle extends Element {
        @Override
        protected void releaseElement() {}
    }


    private static final Logger LOG = Logger.getLogger(TriangleIndices.class.getName());

    private static final String PROPERTY_INDICES  = "TriangleIndices";
    private static final String PROPERTY_TRILOOPS = "TriangleIndices_Loops";


    // Store triangulation in virtual Loops. Use their references to link them. They are not connected to the real structure.
    // Make local triangulation accessible to Loop
    // so normal generator can access virtual triangulation?

    // Result must be int[] or short[]
    // Sorted by triangle!
    // Don't add property to Loop / don't create virtual Loops because in contrast to Vertex, the loops/indices don't need data:
    //   Virtual elements would waste memory in all properties but the index property.
    // Manage own BMeshData<Triangulation> ? Add IntTupleProperty<Triangulation>("indices", 3)

    private final BMesh bmesh;
    private final SweepTriangulation triangulation;

    private final ObjectProperty<Loop, Vertex> propLoopVertex;
    private final ObjectTupleProperty<Triangle, Loop> propTriangleLoops = new ObjectTupleProperty<>(PROPERTY_TRILOOPS, 3, Loop[]::new);

    private final BMeshData<Triangle> triangleData;
    private final VertexBuffer indexBuffer = new VertexBuffer(VertexBuffer.Type.Index);
    private Object lastIndexProperty = null;

    private final IntTupleProperty<Triangle> propIndicesInt = new IntTupleProperty<>(PROPERTY_INDICES, 3);
    private IntBuffer intBuffer;

    private final ShortTupleProperty<Triangle> propIndicesShort = new ShortTupleProperty<>(PROPERTY_INDICES, 3);
    private ShortBuffer shortBuffer;


    public TriangleIndices(BMesh bmesh, ObjectProperty<Loop, Vertex> propLoopVertex) {
        this.bmesh = bmesh;
        triangulation = new SweepTriangulation(bmesh);

        this.propLoopVertex = propLoopVertex;

        triangleData = new BMeshData<>(Triangle::new);
        triangleData.addProperty(propTriangleLoops);
    }


    public VertexBuffer getIndexBuffer() {
        //System.out.println("VertexBuffer[Index] size: " + indexBuffer.getData().limit());
        return indexBuffer;
    }


    /**
     * Updates face triangulation. This needs to be called when the face topology changes.
     * TODO: Call only for dirty faces?
     */
    public void apply() {
        Vec3Property<Vertex> propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());

        triangleData.clear();
        triangleData.ensureCapacity(bmesh.faces().size());

        ArrayList<Loop> loops = new ArrayList<>(6);

        for(Face face : bmesh.faces()) {
            loops.clear();
            face.getLoops(loops);
            final int numVertices = loops.size();

            if(numVertices == 3)
                addTriangle(loops,0, 1, 2);
            else if(numVertices == 4)
                triangulateQuad(propPosition, loops);
            else if(numVertices > 4)
                triangulatePolygon(loops);
            else
                LOG.warning("Couldn't triangulate face with " + numVertices + " vertices.");

            // TODO: Ear clipping for faces with 5-10 vertcies?
        }
    }


    private void addTriangle(ArrayList<Loop> loops, int i1, int i2, int i3) {
        Triangle tri = triangleData.create();
        Loop l1 = loops.get(i1);
        Loop l2 = loops.get(i2);
        Loop l3 = loops.get(i3);
        propTriangleLoops.setValues(tri, l1, l2, l3);
    }


    /**
     * Triangulates a quadliteral with a split along the shorter diagonal.
     * If a vertex is reflex and the quad forms an arrowhead, this reflex vertex will be part of the chosen diagonal.
     */
    private void triangulateQuad(Vec3Property<Vertex> propPosition, ArrayList<Loop> loops) {
        Vector3f p0 = propPosition.get(loops.get(0).vertex);
        Vector3f p1 = propPosition.get(loops.get(1).vertex);
        Vector3f p2 = propPosition.get(loops.get(2).vertex);
        Vector3f p3 = propPosition.get(loops.get(3).vertex);

        // Test 1 & 3 against diagonal 0->2
        Vector3f diagonal = p2.subtract(p0);

        Vector3f v = p1.subtract(p0);
        Vector3f cross = diagonal.cross(v); // cross = 0->2 x 0->1

        v.set(p3).subtractLocal(p0);
        v.crossLocal(diagonal); // v = 0->3 x 0->2

        // If 1 & 3 are on different sides of 0->2, diagonal is valid
        float length0_2 = Float.POSITIVE_INFINITY;
        if(cross.dot(v) > 0)
            length0_2 = diagonal.lengthSquared();

        // Test 0 & 2 against diagonal 1->3
        diagonal.set(p3).subtractLocal(p1);

        v.set(p0).subtractLocal(p1);
        cross.set(diagonal).crossLocal(v); // cross = 1->3 x 1->0

        v.set(p2).subtractLocal(p1);
        v.crossLocal(diagonal); // v = 1->2 x 1->3

        // If 0 & 2 are on different sides of 1->3, diagonal is valid
        float length1_3 = Float.POSITIVE_INFINITY;
        if(cross.dot(v) > 0)
            length1_3 = diagonal.lengthSquared();

        // Choose shorter diagonal
        if(length0_2 <= length1_3) {
            addTriangle(loops, 0, 1, 2);
            addTriangle(loops, 0, 2, 3);
        }
        else {
            addTriangle(loops, 0, 1, 3);
            addTriangle(loops, 1, 2, 3);
        }
    }


    private void triangulatePolygon(ArrayList<Loop> loops) {
        try {
            triangulation.setTriangleCallback((v1, v2, v3) -> {
                addTriangle(loops, v1.index, v2.index, v3.index);
            });

            triangulation.addFaceWithLoops(loops);
            triangulation.triangulate();
        }
        catch(Throwable t) {
            LOG.warning("Couldn't triangulate face with " + loops.size() + " vertices.");
        }
    }

    
    /**
     * Updates index buffer with existing triangulation and Loop->Vertex mapping.
     * This needs to be called when Loop->Vertex mapping (duplication) is changed, e.g. after NormalGenerator.
     * TODO: Maintain Triangle dirty state so only indices of changed faces are updated?
     */
    public void update() {
        int maxVertexIndex = bmesh.vertices().totalSize()-1;
        int numIndices = triangleData.size() * 3;

        if(maxVertexIndex > Short.MAX_VALUE)
            updateInt(numIndices);
        else
            updateShort(numIndices);

        // TODO: How to change format in existing VertexBuffer? int -> short / short -> int
        //       Clear first and then set again?
        // TODO: Lazy switching of buffer type (don't change every frame)?
    }


    private void updateInt(int numIndices) {
        if(lastIndexProperty != propIndicesInt) {
            if(lastIndexProperty == propIndicesShort)
                triangleData.removeProperty(propIndicesShort);

            triangleData.addProperty(propIndicesInt);
            lastIndexProperty = propIndicesInt;
        }

        for(Triangle tri : triangleData) {
            int i0 = mapTriangleLoopVertexIndex(tri, 0);
            int i1 = mapTriangleLoopVertexIndex(tri, 1);
            int i2 = mapTriangleLoopVertexIndex(tri, 2);
            propIndicesInt.setValues(tri, i0, i1, i2);
        }

        int[] data = triangleData.getCompactData(propIndicesInt);
        if(intBuffer == null || intBuffer.capacity() < numIndices) {
            intBuffer = BufferUtils.createIntBuffer(data);
            //System.out.println("made new int index buffer");
        } else {
            //System.out.println("updating int index buffer");
            intBuffer.clear();
            intBuffer.put(data);
            intBuffer.flip();
        }

        indexBuffer.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.UnsignedInt, intBuffer);
    }

    private void updateShort(int numIndices) {
        if(lastIndexProperty != propIndicesShort) {
            if(lastIndexProperty == propIndicesInt)
                triangleData.removeProperty(propIndicesInt);

            triangleData.addProperty(propIndicesShort);
            lastIndexProperty = propIndicesShort;
        }

        for(Triangle tri : triangleData) {
            int i0 = mapTriangleLoopVertexIndex(tri, 0);
            int i1 = mapTriangleLoopVertexIndex(tri, 1);
            int i2 = mapTriangleLoopVertexIndex(tri, 2);
            propIndicesShort.setValues(tri, (short) i0, (short) i1, (short) i2);
        }

        short[] data = triangleData.getCompactData(propIndicesShort);
        if(shortBuffer == null || shortBuffer.capacity() < numIndices) {
            shortBuffer = BufferUtils.createShortBuffer(data);
            //System.out.println("made new short index buffer");
        } else {
            //System.out.println("updating short index buffer");
            shortBuffer.clear();
            shortBuffer.put(data);
            shortBuffer.flip();
        }

        indexBuffer.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.UnsignedShort, shortBuffer);
    }


    // Triangle -> Loop -> Vertex -> Index
    private int mapTriangleLoopVertexIndex(Triangle tri, int i) {
        Loop loop = propTriangleLoops.get(tri, i);
        return propLoopVertex.get(loop).getIndex();
    }
}
