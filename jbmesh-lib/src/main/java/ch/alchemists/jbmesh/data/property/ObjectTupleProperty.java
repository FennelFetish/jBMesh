package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.Element;

public class ObjectTupleProperty<E extends Element, T> extends BMeshProperty<E, T[]> {
    private final ObjectProperty.ArrayAllocator<T> allocator;


    public ObjectTupleProperty(String name, int components, ObjectProperty.ArrayAllocator<T> allocator) {
        super(name, components);
        this.allocator = allocator;
    }


    public T get(E element, int component) {
        return data[indexOf(element, component)];
    }


    public void set(E element, int component, T value) {
        data[indexOf(element, component)] = value;
    }

    public void setValues(E element, T... values) {
        // throw?
        assert values.length == numComponents;

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


    /*public static <E extends Element, T> ObjectTupleProperty<E, T> get(String name, BMeshData<E> meshData, Class<T[]> arrayType) {
        return (ObjectTupleProperty<E, T>) getProperty(name, meshData, arrayType);
    }

    public static <E extends Element, T> ObjectTupleProperty<E, T> getOrCreate(String name, BMeshData<E> meshData, int numComponents, Class<T[]> arrayType, ObjectProperty.ArrayAllocator<T> allocator) {
        ObjectTupleProperty<E, T> prop = get(name, meshData, arrayType);
        if(prop == null) {
            prop = new ObjectTupleProperty<E, T>(name, numComponents, arrayType, allocator);
            meshData.addProperty(prop);
        }
        return prop;
    }*/
}