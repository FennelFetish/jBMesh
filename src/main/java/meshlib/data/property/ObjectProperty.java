package meshlib.data.property;

import meshlib.data.BMeshProperty;
import meshlib.data.Element;

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
}
