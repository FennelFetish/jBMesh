package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.Element;

public class ObjectAttribute<E extends Element, T> extends BMeshAttribute<E, T[]> {
    public static interface ArrayAllocator<T> {
        T[] alloc(int size);
    }


    private final ArrayAllocator<T> allocator;


    public ObjectAttribute(String name, ArrayAllocator<T> allocator) {
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


    public static <E extends Element, T> ObjectAttribute<E, T> get(String name, BMeshData<E> meshData, Class<T[]> arrayType) {
        return (ObjectAttribute<E, T>) getAttribute(name, meshData, arrayType);
    }

    public static <E extends Element, T> ObjectAttribute<E, T> getOrCreate(String name, BMeshData<E> meshData, Class<T[]> arrayType, ArrayAllocator<T> allocator) {
        ObjectAttribute<E, T> attribute = get(name, meshData, arrayType);
        if(attribute == null) {
            attribute = new ObjectAttribute<E, T>(name, allocator);
            meshData.addAttribute(attribute);
        }
        return attribute;
    }
}
