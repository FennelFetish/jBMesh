package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public abstract class Export<E extends Element> {
    private static class PropertyMapping<E extends Element, TArray> {
        private final VertexBuffer.Type type;
        private final Class<?> arrayType;

        private final BMeshProperty<E, TArray> src; // null for vertex properties
        private final BMeshProperty<Vertex, TArray> dest;

        public PropertyMapping(VertexBuffer.Type type, BMeshProperty<E, TArray> src, BMeshProperty<Vertex, TArray> dest) {
            this.type = type;
            this.src  = src;
            this.dest = dest;

            this.arrayType = dest.array().getClass();
        }
    }


    private static final Logger LOG = Logger.getLogger(Export.class.getName());

    protected final BMesh bmesh;
    protected final Mesh outputMesh = new Mesh();

    private final List<PropertyMapping<E, ?>> vertexProperties = new ArrayList<>(4);
    private final List<PropertyMapping<E, ?>> propertyMappings = new ArrayList<>(4);

    private final List<Vertex> tempVertices = new ArrayList<>();
    private final List<Vertex> virtualVertices = new ArrayList<>();


    protected Export(BMesh bmesh) {
        this.bmesh = bmesh;
    }


    protected abstract void setIndexBuffer();

    protected abstract void getVertexNeighborhood(Vertex vertex, List<E> dest);
    protected abstract void setVertexReference(Vertex contactPoint, E element, Vertex ref);
    protected abstract Vertex getVertexReference(Vertex contactPoint, E element);


    public void addVertexProperty(VertexBuffer.Type type, BMeshProperty<Vertex, ?> vertexProperty) {
        Objects.requireNonNull(vertexProperty);
        vertexProperties.add(new PropertyMapping<>(type, null, vertexProperty));
    }

    // TODO: Create mapping [BMeshProperty name] -> [VertexBuffer.Type]
    public void addVertexProperty(VertexBuffer.Type type, String vertexPropertyName) {
        BMeshProperty<Vertex, ?> property = bmesh.vertices().getProperty(vertexPropertyName);
        if(property == null)
            throw new IllegalArgumentException("Vertex property '" + vertexPropertyName + "' does not exist.");

        vertexProperties.add(new PropertyMapping<>(type, null, property));
    }


    public <TArray> void addPropertyMapping(VertexBuffer.Type type, BMeshProperty<E, TArray> src, BMeshProperty<Vertex, TArray> dest) {
        Objects.requireNonNull(src);
        Objects.requireNonNull(dest);

        if(src.numComponents != dest.numComponents)
            throw new IllegalArgumentException("Properties don't have the same number of components.");

        propertyMappings.add(new PropertyMapping<>(type, src, dest));
    }

    @SuppressWarnings("unchecked")
    public <TArray> void addPropertyMapping(VertexBuffer.Type type, BMeshProperty<E, TArray> src, String vertexPropertyName) {
        Objects.requireNonNull(src);

        BMeshProperty<Vertex, TArray> dest = (BMeshProperty<Vertex, TArray>) bmesh.vertices().getProperty(vertexPropertyName);
        if(dest == null)
            throw new IllegalArgumentException("Target vertex property '" + vertexPropertyName + "' does not exist.");

        if(src.getClass() != dest.getClass())
            throw new IllegalArgumentException("Properties are not of the same type.");

        if(src.numComponents != dest.numComponents)
            throw new IllegalArgumentException("Properties don't have the same number of components.");

        propertyMappings.add(new PropertyMapping<E, TArray>(type, src, dest));
    }


    public final Mesh getMesh() {
        return outputMesh;
    }


    public Mesh update() {
        // TODO: Pool virtual vertices and reuse objects? They are destroyed and recreated immediately.
        //       Do this by decorating BMeshData with free list functionality?
        for(Vertex v : virtualVertices)
            bmesh.vertices().destroy(v);
        virtualVertices.clear();

        // If there are no mapped element properties, there is nothing to duplicate
        if(propertyMappings.isEmpty())
            mapElementsToVertices();
        else
            duplicateVertices();

        bmesh.compactData(); // TODO: Optional
        setIndexBuffer();
        setBuffers(outputMesh);
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
                copyProperties(element, vertex);

                // Create virtual Vertex (slot in data array) for elements with different properties
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
        // Compare element properties with previous elements
        for(int k=0; k<i; ++k) {
            E prev = neighbors.get(k);
            if(equalProperties(element, prev)) {
                Vertex ref = getVertexReference(vertex, prev);
                assert ref != null;
                return ref;
            }
        }

        // Different properties found, duplicate vertex
        Vertex ref = bmesh.vertices().createVirtual();
        virtualVertices.add(ref);
        bmesh.vertices().copyProperties(vertex, ref);
        copyProperties(element, ref);
        return ref;
    }


    private boolean equalProperties(E a, E b) {
        for(PropertyMapping<E, ?> mapping : propertyMappings) {
            if(!mapping.src.equals(a, b))
                return false;
        }

        return true;
    }


    private void copyProperties(E src, Vertex dest) {
        for(PropertyMapping<E, ?> mapping : propertyMappings)
            mapping.src.copy(src, mapping.dest, dest);
    }


    private void setBuffers(Mesh outputMesh) {
        for(PropertyMapping<E, ?> prop : vertexProperties)
            setBuffer(outputMesh, prop);

        for(PropertyMapping<E, ?> mapping : propertyMappings)
            setBuffer(outputMesh, mapping);
    }


    // TODO: Reuse existing buffers
    // TODO: Clear previously defined buffers that were removed by removeProperty() (tbd)?
    private void setBuffer(Mesh mesh, PropertyMapping<E, ?> property) {
        VertexBuffer.Type type = property.type;
        Class<?> arrayType     = property.arrayType;
        int components         = property.dest.numComponents;
        Object array           = property.dest.array();

        if(arrayType == float[].class)
            mesh.setBuffer(type, components, BufferUtils.createFloatBuffer((float[]) array));
        else if(arrayType == short[].class)
            mesh.setBuffer(type, components, BufferUtils.createShortBuffer((short[]) array));
        else if(arrayType == int[].class)
            mesh.setBuffer(type, components, BufferUtils.createIntBuffer((int[]) array));
        else if(arrayType == byte[].class)
            mesh.setBuffer(type, components, BufferUtils.createByteBuffer((byte[]) array));
        else if(arrayType == double[].class)
            mesh.setBuffer(type, components, VertexBuffer.Format.Double, VertexBufferUtils.createDoubleBuffer((double[]) array));
        else
            throw new UnsupportedOperationException("Data of type '" + arrayType.getName() + "' is not supported.");
    }



    private void printArr(String name, float[] arr, int comp) {
        System.out.println(name + " ------");

        int o=0;
        for(int i=0; i<arr.length; ) {
            System.out.print(o + ": ");
            for(int c=0; c<comp; ++c) {
                System.out.print(arr[i++]);
                System.out.print(", ");
            }
            System.out.println("");
            o++;
        }
    }

    private void printArr(String name, ArrayList<Integer> arr, int comp) {
        System.out.println(name + " ------");

        int o=0;
        for(int i=0; i<arr.size(); ) {
            System.out.print(o + ": ");
            for(int c=0; c<comp; ++c) {
                System.out.print(arr.get(i++));
                System.out.print(", ");
            }
            System.out.println("");
            o++;
        }
    }
}
