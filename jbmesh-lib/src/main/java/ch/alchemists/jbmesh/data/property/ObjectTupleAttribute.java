// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.Element;

public class ObjectTupleAttribute<E extends Element, T> extends BMeshAttribute<E, T[]> {
    private final ObjectAttribute.ArrayAllocator<T> allocator;


    public ObjectTupleAttribute(String name, int components, ObjectAttribute.ArrayAllocator<T> allocator) {
        super(name, components);
        this.allocator = allocator;
    }


    public T getComponent(E element, int component) {
        return data[indexOf(element, component)];
    }

    public void setComponent(E element, int component, T value) {
        data[indexOf(element, component)] = value;
    }


    public void setValues(E element, T... values) {
        if(values.length != numComponents)
            throw new IllegalArgumentException("Number of values does not match number of components.");

        int index = indexOf(element);
        for(int i = 0; i < numComponents; ++i)
            data[index++] = values[i];
    }


    @Override
    public boolean equals(E a, E b) {
        int indexA = indexOf(a);
        int indexB = indexOf(b);

        for(int i = 0; i < numComponents; ++i) {
            if(data[indexA++] != data[indexB++])
                return false;
        }

        return true;
    }


    @Override
    protected T[] alloc(int size) {
        return allocator.alloc(size);
    }


    public static <E extends Element, T> ObjectTupleAttribute<E, T> get(String name, BMeshData<E> meshData, Class<T[]> arrayType) {
        return (ObjectTupleAttribute<E, T>) getAttribute(name, meshData, arrayType);
    }

    public static <E extends Element, T> ObjectTupleAttribute<E, T> getOrCreate(String name, int components, BMeshData<E> meshData, Class<T[]> arrayType, ObjectAttribute.ArrayAllocator<T> allocator) {
        ObjectTupleAttribute<E, T> attribute = get(name, meshData, arrayType);

        if(attribute == null) {
            attribute = new ObjectTupleAttribute<E, T>(name, components, allocator);
            meshData.addAttribute(attribute);
        }
        else if(attribute.numComponents != components)
            throw new IllegalStateException("Attribute with same name but different number of components already exists.");

        return attribute;
    }
}