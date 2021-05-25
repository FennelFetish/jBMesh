package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.data.property.IntTupleAttribute;
import ch.alchemists.jbmesh.data.property.ShortTupleAttribute;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class Indices<E extends Element> {
    public interface IndexApplicator<E extends Element> {
        void applyIndices(E element, int[] indices);
    }


    private final BMeshData<E> meshData;
    private VertexBuffer indexBuffer;
    private VertexBuffer.Usage bufferUsage = VertexBuffer.Usage.Dynamic;

    private final IntTupleAttribute<E> intIndices;
    private IntBuffer intBuffer;

    private final ShortTupleAttribute<E> shortIndices;
    private ShortBuffer shortBuffer;

    private boolean useInt = false;
    private int shortHysteresis = Short.MAX_VALUE - 767;


    @SuppressWarnings("unchecked")
    public Indices(BMeshData<E> meshData, int indicesPerElement) {
        this.meshData = meshData;

        do {
            BMeshAttribute<E, ?> attr = meshData.getAttribute(BMeshAttribute.Index);
            if(attr != null) {
                if(attr.numComponents == indicesPerElement) {
                    if(attr.getClass() == IntTupleAttribute.class) {
                        intIndices = (IntTupleAttribute<E>) attr;
                        shortIndices = new ShortTupleAttribute<>(BMeshAttribute.Index, indicesPerElement);
                        break;
                    }
                    else if(attr.getClass() == ShortTupleAttribute.class) {
                        intIndices = new IntTupleAttribute<>(BMeshAttribute.Index, indicesPerElement);
                        shortIndices = (ShortTupleAttribute<E>) attr;
                        break;
                    }
                }

                meshData.removeAttribute(attr);
            }

            intIndices   = new IntTupleAttribute<>(BMeshAttribute.Index, indicesPerElement);
            shortIndices = new ShortTupleAttribute<>(BMeshAttribute.Index, indicesPerElement);
        } while(false);

        intIndices.setComparable(false);
        shortIndices.setComparable(false);
    }


    public void setBufferUsage(VertexBuffer.Usage usage) {
        this.bufferUsage = usage;

        if(indexBuffer != null)
            indexBuffer.setUsage(usage);

    }

    public void setShortHysteresis(int value) {
        shortHysteresis = value;
    }

    public VertexBuffer getIndexBuffer() {
        return indexBuffer;
    }


    public void prepare(int maxVertexIndex) {
        // If we were already using int buffer, use some hysteresis before switching back to short buffer.
        int limit = Short.MAX_VALUE;
        if(useInt)
            limit = shortHysteresis;

        // Use int buffer
        if(maxVertexIndex > limit) {
            useInt = true;
            if(!intIndices.isAttached()) {
                if(shortIndices.isAttached())
                    meshData.removeAttribute(shortIndices);

                meshData.addAttribute(intIndices);
                indexBuffer = null;
                shortBuffer = null;
            }
        }
        // Use short buffer
        else {
            useInt = false;
            if(!shortIndices.isAttached()) {
                if(intIndices.isAttached())
                    meshData.removeAttribute(intIndices);

                meshData.addAttribute(shortIndices);
                indexBuffer = null;
                intBuffer = null;
            }
        }
    }


    public void setIndices(E element, int... indices) {
        if(useInt)
            intIndices.setValues(element, indices);
        else
            shortIndices.setValues(element, indices);
    }


    public void updateIndices(IndexApplicator<E> indexApplicator) {
        if(useInt)
            updateIndicesInt(indexApplicator);
        else
            updateIndicesShort(indexApplicator);
    }

    private void updateIndicesInt(IndexApplicator<E> indexApplicator) {
        int[] elementIndices = new int[intIndices.numComponents];
        for(E element : meshData) {
            indexApplicator.applyIndices(element, elementIndices);
            intIndices.setValues(element, elementIndices);
        }
    }

    private void updateIndicesShort(IndexApplicator<E> indexApplicator) {
        int[] elementIndices = new int[shortIndices.numComponents];
        for(E element : meshData) {
            indexApplicator.applyIndices(element, elementIndices);
            shortIndices.setValues(element, elementIndices);
        }
    }


    public void applyIndexBuffer(Mesh mesh) {
        if(useInt)
            applyBufferInt(mesh);
        else
            applyBufferShort(mesh);
    }


    private void applyBufferInt(Mesh mesh) {
        final int dataSize = meshData.totalSize() * intIndices.numComponents;

        if(intBuffer == null || intBuffer.capacity() < dataSize)
            intBuffer = BufferUtils.createIntBuffer(dataSize);

        intBuffer.clear();
        intBuffer.put(intIndices.array(), 0, dataSize);
        intBuffer.flip();

        applyBuffer(mesh, intBuffer, VertexBuffer.Format.UnsignedInt);
    }


    private void applyBufferShort(Mesh mesh) {
        final int dataSize = meshData.totalSize() * shortIndices.numComponents;

        if(shortBuffer == null || shortBuffer.capacity() < dataSize)
            shortBuffer = BufferUtils.createShortBuffer(dataSize);

        shortBuffer.clear();
        shortBuffer.put(shortIndices.array(), 0, dataSize);
        shortBuffer.flip();

        applyBuffer(mesh, shortBuffer, VertexBuffer.Format.UnsignedShort);
    }


    private void applyBuffer(Mesh mesh, Buffer buffer, VertexBuffer.Format format) {
        if(indexBuffer != null) {
            indexBuffer.updateData(buffer);
            mesh.updateCounts();
        }
        else {
            indexBuffer = new VertexBuffer(VertexBuffer.Type.Index);
            indexBuffer.setupData(bufferUsage, shortIndices.numComponents, format, buffer);

            mesh.clearBuffer(VertexBuffer.Type.Index);
            mesh.setBuffer(indexBuffer);
        }
    }
}
