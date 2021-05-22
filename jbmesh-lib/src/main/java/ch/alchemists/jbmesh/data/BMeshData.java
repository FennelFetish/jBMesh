package ch.alchemists.jbmesh.data;

import java.lang.reflect.Array;
import java.nio.Buffer;
import java.util.*;

public class BMeshData<E extends Element> implements Iterable<E> {
    public static interface ElementFactory<E extends Element> {
        E createElement();
    }


    private final ElementFactory<E> factory;
    private final ArrayList<E> elements = new ArrayList<>();

    private static final int INITIAL_ARRAY_SIZE = 32;
    private static final float GROW_FACTOR = 1.5f;
    private int arraySize = INITIAL_ARRAY_SIZE;
    private int numVirtual = 0;

    private int modCount = 0;
    
    private final Map<String, BMeshProperty<E, ?>> properties = new HashMap<>();


    public BMeshData(ElementFactory<E> factory) {
        this.factory = factory;
    }


    @Override
    public Iterator<E> iterator() {
        return new ElementIterator();
    }

    public int size() {
        return elements.size() - numVirtual;
    }

    /**
     * Includes count of virtual elements.
     * @return
     */
    public int totalSize() {
        return elements.size();
    }


    public E get(int index) {
        // TODO: Don't return virtual elements?
        return elements.get(index);
    }

    public Collection<E> getAll(Collection<E> dest) {
        // TODO: Don't return virtual elements?
        for(E e : this)
            dest.add(e);
        return dest;
    }

    public List<E> getAll() {
        // TODO: Don't return virtual elements?
        return new ArrayList<>(elements);
    }


    public void clear() {
        for(E element : elements) {
            element.release();
        }

        elements.clear();

        numVirtual = 0;
        modCount++;
    }


    public E create() {
        int newIndex = elements.size();
        if(newIndex >= arraySize) {
            int capacity = (int) Math.ceil(arraySize * GROW_FACTOR);
            ensureCapacity(capacity);
        }

        E element = factory.createElement();
        element.setIndex(newIndex);
        elements.add(element);

        modCount++;
        return element;
    }

    public E createVirtual() {
        E element = create();
        element.setFlags(Element.FLAG_VIRTUAL);
        numVirtual++;
        return element;
    }

    public void destroy(E element) {
        final int index = element.getIndex();
        if(index < 0)
            return;

        if(element.checkFlags(Element.FLAG_VIRTUAL))
            numVirtual--;

        // Move last element into this slot
        int lastIndex = elements.size() - 1;
        if(index != lastIndex) {
            E lastElement = elements.get(lastIndex);
            copyProperties(lastElement, element);
            elements.set(index, lastElement);
            lastElement.setIndex(index);
        }

        elements.remove(lastIndex);
        element.release();
        modCount++;

        // TODO: Reset property values?
    }


    public void addProperty(BMeshProperty<E, ?> property) {
        if(properties.containsKey(property.name))
            throw new IllegalStateException("Property '" + property.name + "' already exists");

        if(property.data != null)
            throw new IllegalStateException("Property '" + property.name + "' already associated with another data set");

        Object oldArray = property.allocReplace(arraySize);
        assert oldArray == null;
        properties.put(property.name, property);
    }

    public <TArray> void addProperty(BMeshProperty<E, TArray> property, TArray data) {
        if(properties.containsKey(property.name))
            throw new IllegalStateException("Property '" + property.name + "' already exists");

        if(property.data != null)
            throw new IllegalStateException("Property '" + property.name + "' already associated with another data set");

        int len = Array.getLength(data);
        if(len != arraySize * property.numComponents)
            throw new IllegalArgumentException("Array length (" + (len/property.numComponents) + ") does not match managed length (" + arraySize + ")");

        property.data = data;
        properties.put(property.name, property);
    }


    // getProperty(name, Vec3Property.class) should return Vec3Property<E> ?? to avoid casting at call site
    public BMeshProperty<E, ?> getProperty(String name) {
        return properties.get(name);
    }

    /*public <TArray> BMeshProperty<E, TArray> getProperty(String name, Class<TArray> arrayType) {
        return (BMeshProperty<E, TArray>) properties.get(name);
    }*/


    public void removeProperty(BMeshProperty<E, ?> property) {
        if(properties.remove(property.name) != null)
            property.release();
        else
            throw new IllegalArgumentException("Property not associated with this data set");
    }

    public BMeshProperty<E, ?> removeProperty(String name) {
        BMeshProperty<E, ?> property = properties.remove(name);
        if(property != null)
            property.release();
        return property;
    }


    public void clearProperties() {
        for(BMeshProperty<E, ?> property : properties.values())
            property.release();
        properties.clear();
    }


    /**
     * Increases the capacity of the underlying data structures, if necessary,
     * to ensure that it can hold at least the number of elements specified by the minimum capacity argument.
     * @param minCapacity the desired minimum capacity
     */
    public void ensureCapacity(int minCapacity) {
        elements.ensureCapacity(minCapacity);

        if(arraySize < minCapacity)
            resize(minCapacity, arraySize);
    }

    /**
     * Ensures that at least <i>count</i> more elements can be created before further reallocations
     * of the underlying data structures become necessary.
     * @param count the desired number of elements to reserve space for
     */
    public void reserveCapacity(int count) {
        ensureCapacity(elements.size() + count);
    }


    private void resize(int size, int copyLength) {
        for(BMeshProperty prop : properties.values()) {
            prop.realloc(size, copyLength);
        }

        arraySize = size;
    }


    // TODO: Rename to trimToSize()?
    // TODO: The arrays don't really need to be trimmed after each change. The size of the target OpenGL buffers matters more.
    //         -> Only write necessary data but allow arrays to be longer.
    public void compactData() {
        int numElements = elements.size();
        //elements.trimToSize();

        if(arraySize != numElements) {
            resize(numElements, numElements);
            arraySize = numElements;
            modCount++;
        }
    }


    public <TArray> TArray getCompactData(BMeshProperty<E, TArray> property) {
        final int size = elements.size() * property.numComponents;
        TArray array = property.alloc(size);
        System.arraycopy(property.data, 0, array, 0, size);
        return array;
    }


    public <TArray> void putCompactData(BMeshProperty<E, TArray> property, Buffer buffer) {
        buffer.clear();
        // ...
    }


    public void sort(Comparator<E> comparator) {
        // Sort backing arrays, reassign element indices
        // For optimizing OpenGL performance? Does this matter?
        //   -> Yes because of vertex cache. The vertex shader may be called multiple times for the same vertex
        //   if there's too much space between uses (indices).
        // e.g. sort vertices by face for better cache utilisation, sort loops by face
        // https://gamedev.stackexchange.com/questions/59163/is-creating-vertex-index-buffer-optimized-this-way

        // Also provide back-to-front sorting for indices
    }


    public boolean equals(E a, E b) {
        for(BMeshProperty<E, ?> prop : properties.values()) {
            if(prop.isComparable() && !prop.equals(a, b))
                return false;
        }

        return true;
    }


    public void copyProperties(E from, E to) {
        for(BMeshProperty<E, ?> prop : properties.values()) {
            prop.copy(from, to);
        }
    }



    private class ElementIterator implements Iterator<E> {
        private final int expectedModCount;
        private int index = -1;

        private ElementIterator() {
            expectedModCount = modCount;
            skipToNextListed();
        }

        @Override
        public boolean hasNext() {
            if(modCount != expectedModCount)
                throw new ConcurrentModificationException();

            return index < elements.size();
        }

        @Override
        public E next() {
            if(modCount != expectedModCount)
                throw new ConcurrentModificationException();

            E element = elements.get(index);
            skipToNextListed();
            return element;
        }

        private void skipToNextListed() {
            // Skip to next listed element (alive and non-virtual)
            while(++index < elements.size() && !elements.get(index).isListed()) {}
        }
    }
}