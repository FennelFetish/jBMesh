package ch.alchemists.jbmesh.conversion;

import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.data.property.*;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;

public class TriangleIndices {
    private static class Triangle extends Element {
        @Override
        protected void releaseElement() {}
    }


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
    private final ObjectProperty<Loop, Vertex> propLoopVertex;
    private final ObjectTupleProperty<Triangle, Loop> propTriangleLoops = new ObjectTupleProperty<Triangle, Loop>(PROPERTY_TRILOOPS, 3, Loop[]::new);

    private final BMeshData<Triangle> triangleData;
    private final VertexBuffer indexBuffer = new VertexBuffer(VertexBuffer.Type.Index);
    private Object lastIndexProperty = null;

    private final IntTupleProperty<Triangle> propIndicesInt = new IntTupleProperty<>(PROPERTY_INDICES, 3);
    private IntBuffer intBuffer;

    private final ShortTupleProperty<Triangle> propIndicesShort = new ShortTupleProperty<>(PROPERTY_INDICES, 3);
    private ShortBuffer shortBuffer;


    public TriangleIndices(BMesh bmesh, ObjectProperty<Loop, Vertex> propLoopVertex) {
        this.bmesh = bmesh;
        this.propLoopVertex = propLoopVertex;

        triangleData = new BMeshData<>(() -> new Triangle());
        triangleData.addProperty(propTriangleLoops);
    }


    /**
     * Updates face triangulation. This needs to be called when the face topology changes.
     * TODO: Call only for dirty faces?
     */
    public void apply() {
        Vec3Property<Vertex> propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());

        triangleData.clear();
        triangleData.ensureCapacity(bmesh.faces().size());

        Vector3f v1 = new Vector3f();
        Vector3f v2 = new Vector3f();

        ArrayList<Loop> loops = new ArrayList<>(6);
        for(Face face : bmesh.faces()) {
            loops.clear();
            face.getLoops(loops);

            switch(loops.size()) {
                case 0:
                case 1:
                case 2:
                    assert false;
                    break;

                case 3:
                    addTriangle(loops,0, 1, 2);
                    break;

                case 4: {
                    propPosition.get(loops.get(0).vertex, v1);
                    propPosition.subtract(loops.get(2).vertex, v1);

                    propPosition.get(loops.get(1).vertex, v2);
                    propPosition.subtract(loops.get(3).vertex, v2);

                    if(v1.lengthSquared() <= v2.lengthSquared()) {
                        addTriangle(loops, 0, 1, 2);
                        addTriangle(loops, 0, 2, 3);
                    } else {
                        addTriangle(loops, 0, 1, 3);
                        addTriangle(loops, 1, 2, 3);
                    }

                    break;
                }

                default: { // 5+
                    // Fan-like triangulation
                    // TODO: This creates superfluous triangles for collinear edges?
                    for(int i=2; i<loops.size(); ++i) {
                        addTriangle(loops, 0, i-1, i);
                    }
                }
            } // switch
        } // for
    }


    private void addTriangle(ArrayList<Loop> loops, int i1, int i2, int i3) {
        Triangle tri = triangleData.create();
        Loop l1 = loops.get(i1);
        Loop l2 = loops.get(i2);
        Loop l3 = loops.get(i3);
        propTriangleLoops.setValues(tri, l1, l2, l3);
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


    public VertexBuffer getIndexBuffer() {
        //System.out.println("VertexBuffer[Index] size: " + indexBuffer.getData().limit());
        return indexBuffer;
    }
}
