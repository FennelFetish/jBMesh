package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.*;
import java.util.*;
import java.util.logging.Logger;

public abstract class Export<E extends Element> {
    private static class AttributeMapping<E extends Element, TArray> {
        private final VertexBuffer.Type type;
        private final Class<?> arrayType;

        private final BMeshAttribute<E, TArray> src; // null for vertex attributes
        private final BMeshAttribute<Vertex, TArray> dest;

        public AttributeMapping(VertexBuffer.Type type, BMeshAttribute<E, TArray> src, BMeshAttribute<Vertex, TArray> dest) {
            this.type = type;
            this.src  = src;
            this.dest = dest;

            this.arrayType = dest.array().getClass();
        }
    }


    private static final Logger LOG = Logger.getLogger(Export.class.getName());

    protected final BMesh bmesh;
    private final Mesh outputMesh = new Mesh();

    private final Map<VertexBuffer.Type, AttributeMapping<E, ?>> attributes = new HashMap<>(8);
    private final List<AttributeMapping<E, ?>> mappedAttributes = new ArrayList<>(4);

    private final List<Vertex> tempVertices = new ArrayList<>();
    private final List<Vertex> virtualVertices = new ArrayList<>();

    private float bufferLoadFactor = 0.75f;


    protected Export(BMesh bmesh, Mesh.Mode mode) {
        this.bmesh = bmesh;
        outputMesh.setMode(mode);
    }


    protected abstract void applyIndexBuffer(Mesh mesh);

    protected abstract void getVertexNeighborhood(Vertex vertex, List<E> dest);
    protected abstract void setVertexReference(Vertex contactPoint, E element, Vertex ref);
    protected abstract Vertex getVertexReference(Vertex contactPoint, E element);


    public final Mesh getMesh() {
        return outputMesh;
    }


    /**
     * When an attribute's data uses less that this percentage of an existing VertexBuffer's capacity,
     * the buffer is resized to the size of the data to save memory.<br><br>
     * Set to 0.0 to disable shrinking of buffers.<br>
     * Set to 1.0 to always shrink buffers.<br>
     * Defaults to 0.75.
     * @param loadFactor Percentage (0.0 - 1.0). Values greater than 1.0 are truncated to 1.0.
     */
    public void setBufferLoadFactor(float loadFactor) {
        this.bufferLoadFactor = Math.min(loadFactor, 1.0f);
    }


    public void useVertexAttribute(BMeshAttribute<Vertex, ?> vertexAttribute) {
        Objects.requireNonNull(vertexAttribute);
        VertexBuffer.Type type = VertexBufferUtils.getVertexBufferType(vertexAttribute.name);
        useVertexAttribute(type, vertexAttribute);
    }

    public void useVertexAttribute(String attributeName) {
        VertexBuffer.Type type = VertexBufferUtils.getVertexBufferType(attributeName);
        useVertexAttribute(type, attributeName);
    }

    public void useVertexAttribute(VertexBuffer.Type type, String attributeName) {
        BMeshAttribute<Vertex, ?> attribute = bmesh.vertices().getAttribute(attributeName);
        if(attribute == null)
            throw new IllegalArgumentException("Vertex attribute '" + attributeName + "' does not exist.");
        useVertexAttribute(type, attribute);
    }

    public void useVertexAttribute(VertexBuffer.Type type, BMeshAttribute<Vertex, ?> vertexAttribute) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(vertexAttribute);

        AttributeMapping<E, ?> mapping = new AttributeMapping<>(type, null, vertexAttribute);
        if(attributes.put(type, mapping) != null)
            LOG.warning("Overriding use of vertex attribute: Now using vertex attribute " + vertexAttribute.name + " for VertexBuffer " + type.name());
    }


    public <TArray> void mapAttribute(BMeshAttribute<E, TArray> src, String vertexAttributeName) {
        VertexBuffer.Type type = VertexBufferUtils.getVertexBufferType(vertexAttributeName);
        mapAttribute(type, src, vertexAttributeName);
    }

    public <TArray> void mapAttribute(VertexBuffer.Type type, BMeshAttribute<E, TArray> src) {
        String vertexAttributeName = VertexBufferUtils.getBMeshAttributeName(type);
        mapAttribute(type, src, vertexAttributeName);
    }

    @SuppressWarnings("unchecked")
    public <TArray> void mapAttribute(VertexBuffer.Type type, BMeshAttribute<E, TArray> src, String vertexAttributeName) {
        BMeshAttribute<Vertex, TArray> dest = (BMeshAttribute<Vertex, TArray>) bmesh.vertices().getAttribute(vertexAttributeName);

        if(dest == null) {
            dest = (BMeshAttribute<Vertex, TArray>) VertexBufferUtils.createBMeshAttribute(type, src.numComponents, Vertex.class);
            if(dest == null)
                throw new IllegalStateException("Target vertex attribute '" + vertexAttributeName + "' does not exist and couldn't be created.");

            bmesh.vertices().addAttribute(dest);
        }

        mapAttribute(type, src, dest);
    }

    public <TArray> void mapAttribute(BMeshAttribute<E, TArray> src, BMeshAttribute<Vertex, TArray> dest) {
        VertexBuffer.Type type = VertexBufferUtils.getVertexBufferType(dest.name);
        mapAttribute(type, src, dest);
    }

    public <TArray> void mapAttribute(VertexBuffer.Type type, BMeshAttribute<E, TArray> src, BMeshAttribute<Vertex, TArray> dest) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(src);
        Objects.requireNonNull(dest);

        if(src.array().getClass() != dest.array().getClass())
            throw new IllegalArgumentException("Attribute data types don't match.");

        if(src.numComponents != dest.numComponents)
            throw new IllegalArgumentException("Attributes don't have the same number of components.");

        AttributeMapping<E, TArray> mapping = new AttributeMapping<>(type, src, dest);
        AttributeMapping<E, ?> prev = attributes.put(type, mapping);
        if(prev != null) {
            mappedAttributes.remove(prev);
            LOG.warning("Overriding mapping: Now mapping element attribute " + src.name + " to VertexBuffer " + type.name());
        }

        mappedAttributes.add(mapping);
    }


    public void clearAttributes() {
        for(VertexBuffer.Type type : attributes.keySet())
            outputMesh.clearBuffer(type);

        attributes.clear();
        mappedAttributes.clear();
    }


    public Mesh update() {
        // TODO: Pool virtual vertices and reuse objects? They are destroyed and recreated immediately.
        //       Do this by decorating BMeshData with free list functionality?
        for(Vertex v : virtualVertices)
            bmesh.vertices().destroy(v);
        virtualVertices.clear();

        // If there are no mapped element attributes, there is nothing to duplicate
        if(mappedAttributes.isEmpty())
            mapElementsToVertices();
        else
            duplicateVertices();

        //bmesh.vertices().compactData(); // Optional
        applyIndexBuffer(outputMesh);

        for(AttributeMapping<E, ?> attribute : attributes.values())
            applyVertexBuffer(attribute);

        outputMesh.updateBound();

        LOG.fine("Exported " + bmesh.vertices().size() + " vertices");
        return outputMesh;
    }


    private void mapElementsToVertices() {
        try {
            bmesh.vertices().getAll(tempVertices);
            List<E> neighbors = new ArrayList<>(6);

            for(Vertex vertex : tempVertices) {
                // Get elements that use vertex
                getVertexNeighborhood(vertex, neighbors);

                for(E element : neighbors)
                    setVertexReference(vertex, element, vertex);

                neighbors.clear();
            }
        }
        finally {
            tempVertices.clear();
        }
    }


    /**
     * Creates virtual vertices.
     */
    private void duplicateVertices() {
        try {
            // We need a copy of the vertices because the BMeshData will be modified by adding virtual elements
            // TODO: BMeshData Iterator that only returns non-virtuals and doesn't throw when virtual elements are added?
            bmesh.vertices().getAll(tempVertices);
            List<E> neighbors = new ArrayList<>(6);

            for(Vertex vertex : tempVertices) {
                // Get elements that use vertex
                neighbors.clear();
                getVertexNeighborhood(vertex, neighbors);
                if(neighbors.isEmpty())
                    continue;

                E element = neighbors.get(0);
                setVertexReference(vertex, element, vertex);
                copyAttributes(element, vertex);

                // Create virtual Vertex (slot in data array) for elements with different attributes
                for(int i = 1; i < neighbors.size(); ++i) {
                    element = neighbors.get(i);

                    Vertex ref = tryVirtualize(vertex, neighbors, element, i);
                    setVertexReference(vertex, element, ref);
                }
            }
        }
        finally {
            tempVertices.clear();
        }
    }


    private Vertex tryVirtualize(Vertex vertex, List<E> neighbors, E element, int i) {
        // Compare element attributes with previous elements
        for(int k=0; k<i; ++k) {
            E prev = neighbors.get(k);
            if(equalAttributes(element, prev)) {
                Vertex ref = getVertexReference(vertex, prev);
                assert ref != null;
                return ref;
            }
        }

        // Different attributes found, duplicate vertex
        Vertex ref = bmesh.vertices().createVirtual();
        virtualVertices.add(ref);
        bmesh.vertices().copyAttributes(vertex, ref);
        copyAttributes(element, ref);
        return ref;
    }


    private boolean equalAttributes(E a, E b) {
        for(AttributeMapping<E, ?> mapping : mappedAttributes) {
            if(!mapping.src.equals(a, b))
                return false;
        }

        return true;
    }


    private void copyAttributes(E src, Vertex dest) {
        for(AttributeMapping<E, ?> mapping : mappedAttributes)
            mapping.src.copy(src, mapping.dest, dest);
    }


    private void applyVertexBuffer(AttributeMapping<E, ?> attribute) {
        final Class<?> arrayType = attribute.arrayType;

        if(arrayType == float[].class) {
            applyVertexBuffer(attribute, VertexBuffer.Format.Float, BufferUtils::createFloatBuffer,
                (FloatBuffer buffer, float[] array, int length) -> buffer.put(array, 0, length));
        }
        else if(arrayType == short[].class) {
            applyVertexBuffer(attribute, VertexBuffer.Format.UnsignedShort, BufferUtils::createShortBuffer,
                (ShortBuffer buffer, short[] array, int length) -> buffer.put(array, 0, length));
        }
        else if(arrayType == int[].class) {
            applyVertexBuffer(attribute, VertexBuffer.Format.UnsignedInt, BufferUtils::createIntBuffer,
                (IntBuffer buffer, int[] array, int length) -> buffer.put(array, 0, length));
        }
        else if(arrayType == byte[].class) {
            applyVertexBuffer(attribute, VertexBuffer.Format.UnsignedByte, BufferUtils::createByteBuffer,
                (ByteBuffer buffer, byte[] array, int length) -> buffer.put(array, 0, length));
        }
        else if(arrayType == double[].class) {
            applyVertexBuffer(attribute, VertexBuffer.Format.Double, VertexBufferUtils::createDoubleBuffer,
                (DoubleBuffer buffer, double[] array, int length) -> buffer.put(array, 0, length));
        }
        else
            throw new UnsupportedOperationException("Data of type '" + arrayType.getName() + "' is not supported.");
    }


    @SuppressWarnings("unchecked")
    private <TArray, B extends Buffer> void applyVertexBuffer(AttributeMapping<E, ?> attribute, VertexBuffer.Format format,
                                                              CreateBufferFunctor<B> createBuffer, PopulateBufferFunctor<B, TArray> populateBuffer)
    {
        final VertexBuffer.Type type = attribute.type;
        final int components         = attribute.dest.numComponents;
        final TArray array           = (TArray) attribute.dest.array();
        final int dataSize           = bmesh.vertices().totalSize() * components;

        final VertexBuffer vertexBuffer = outputMesh.getBuffer(type);

        if(vertexBuffer != null) {
            // If VertexBuffer is incompatible, clear it and create new one below
            if(vertexBuffer.getNumComponents() != components || vertexBuffer.getFormat() != format) {
                outputMesh.clearBuffer(type);
            }
            // Valid buffer exists
            else {
                B buffer = (B) vertexBuffer.getData();
                if(buffer.capacity() < dataSize || buffer.capacity() * bufferLoadFactor > dataSize)
                    buffer = createBuffer.apply(dataSize); // Resize buffer
                else
                    buffer.clear(); // Reuse buffer

                populateBuffer.apply(buffer, array, dataSize);
                buffer.flip();

                vertexBuffer.updateData(buffer);
                outputMesh.updateCounts();
                return;
            }
        }

        // Create a new VertexBuffer
        B buffer = createBuffer.apply(dataSize);
        populateBuffer.apply(buffer, array, dataSize);
        buffer.flip();
        outputMesh.setBuffer(type, components, format, buffer);
    }


    @FunctionalInterface
    private interface CreateBufferFunctor<B extends Buffer> {
        B apply(int size);
    }

    @FunctionalInterface
    private interface PopulateBufferFunctor<B extends Buffer, TArray> {
        void apply(B buffer, TArray array, int length);
    }
}
