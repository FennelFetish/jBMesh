// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.data;

import java.lang.reflect.Array;
import java.nio.Buffer;
import java.util.*;

public class BMeshData<E extends Element> implements Iterable<E> {
    public interface ElementFactory<E extends Element> {
        E createElement();
    }


    private final ElementFactory<E> factory;
    private final ArrayList<E> elements = new ArrayList<>();

    private static final int INITIAL_ARRAY_SIZE = 32;
    private static final float GROW_FACTOR = 1.5f;
    private int arraySize = INITIAL_ARRAY_SIZE;
    private int numVirtual = 0;

    private int modCount = 0;
    
    private final Map<String, BMeshAttribute<E, ?>> attributes = new HashMap<>();


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
            copyAttributes(lastElement, element);
            elements.set(index, lastElement);
            lastElement.setIndex(index);
        }

        elements.remove(lastIndex);
        element.release();
        modCount++;

        // TODO: Reset attribute values?
    }


    public void addAttribute(BMeshAttribute<E, ?> attribute) {
        Objects.requireNonNull(attribute);

        if(attributes.containsKey(attribute.name))
            throw new IllegalStateException("Attribute '" + attribute.name + "' already exists");

        if(attribute.data != null)
            throw new IllegalStateException("Attribute '" + attribute.name + "' already associated with another data set");

        Object oldArray = attribute.allocReplace(arraySize);
        assert oldArray == null;
        attributes.put(attribute.name, attribute);
    }

    public <TArray> void addAttribute(BMeshAttribute<E, TArray> attribute, TArray data) {
        Objects.requireNonNull(attribute);
        Objects.requireNonNull(data);

        if(attributes.containsKey(attribute.name))
            throw new IllegalStateException("Attribute '" + attribute.name + "' already exists");

        if(attribute.data != null)
            throw new IllegalStateException("Attribute '" + attribute.name + "' already associated with another data set");

        int len = Array.getLength(data);
        if(len != arraySize * attribute.numComponents)
            throw new IllegalArgumentException("Array length (" + (len/attribute.numComponents) + ") does not match managed length (" + arraySize + ")");

        attribute.data = data;
        attributes.put(attribute.name, attribute);
    }


    // getAttribute(name, Vec3Attribute.class) should return Vec3Attribute<E> ?? to avoid casting at call site
    public BMeshAttribute<E, ?> getAttribute(String name) {
        return attributes.get(name);
    }

    @SuppressWarnings("unchecked")
    public <TArray> BMeshAttribute<E, TArray> getAttribute(String name, Class<TArray> arrayType) {
        BMeshAttribute<E, TArray> attribute = (BMeshAttribute<E, TArray>) attributes.get(name);

        if(attribute == null)
            return null;

        if(attribute.data.getClass() != arrayType)
            throw new ClassCastException("Attribute data type does not match requested type");

        return attribute;
    }


    public void removeAttribute(BMeshAttribute<E, ?> attribute) {
        if(attributes.remove(attribute.name) != null)
            attribute.release();
        else
            throw new IllegalArgumentException("Attribute not associated with this data set");
    }

    public BMeshAttribute<E, ?> removeAttribute(String name) {
        BMeshAttribute<E, ?> attribute = attributes.remove(name);
        if(attribute != null)
            attribute.release();
        return attribute;
    }


    public void clearAttributes() {
        for(BMeshAttribute<E, ?> attribute : attributes.values())
            attribute.release();
        attributes.clear();
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
        for(BMeshAttribute<E, ?> attribute : attributes.values()) {
            attribute.realloc(size, copyLength);
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


    public <TArray> TArray getCompactData(BMeshAttribute<E, TArray> attribute) {
        final int size = elements.size() * attribute.numComponents;
        TArray array = attribute.alloc(size);
        System.arraycopy(attribute.data, 0, array, 0, size);
        return array;
    }


    public <TArray> void putCompactData(BMeshAttribute<E, TArray> attribute, Buffer buffer) {
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
        for(BMeshAttribute<E, ?> attr : attributes.values()) {
            if(attr.isComparable() && !attr.equals(a, b))
                return false;
        }

        return true;
    }


    public void copyAttributes(E from, E to) {
        for(BMeshAttribute<E, ?> attr : attributes.values()) {
            attr.copy(from, to);
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