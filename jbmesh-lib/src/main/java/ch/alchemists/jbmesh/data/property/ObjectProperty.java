package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.Element;

public class ObjectProperty<E extends Element, T> extends BMeshProperty<E, T[]> {
    public static interface ArrayAllocator<T> {
        T[] alloc(int size);
    }


    private final ArrayAllocator<T> allocator;


    public ObjectProperty(String name, ArrayAllocator<T> allocator) {
        super(name);
        this.allocator = allocator;
    }


    public void set(E element, T value) {
        data[indexOf(element)] = value;
    }

    public T get(E element) {
        return data[indexOf(element)];
    }


    @Override
    public boolean equals(E a, E b) {
        return a == b;
    }


    @Override
    protected T[] alloc(int size) {
        return allocator.alloc(size);
    }


    public static <E extends Element, T> ObjectProperty<E, T> get(String name, Class<T[]> arrayType, BMeshData<E> meshData) {
        return (ObjectProperty<E, T>) getProperty(name, meshData, arrayType);
    }

    /*public static <E extends Element, T> ObjectProperty<E, T> getOrCreate(String name, Class<T[]> arrayType, BMeshData<E> meshData) {
        ObjectProperty<E, T> prop = get(name, arrayType, meshData);
        if(prop == null) {
            prop = new ObjectProperty<E, T>(name, allocator);
            meshData.addProperty(prop);
        }
        return prop;
    }*/
}
