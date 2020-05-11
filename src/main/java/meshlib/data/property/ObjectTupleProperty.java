package meshlib.data.property;

import meshlib.data.BMeshProperty;
import meshlib.data.Element;

public class ObjectTupleProperty<E extends Element, T> extends BMeshProperty<E, T[]> {
    private final ObjectProperty.ArrayAllocator<T> allocator;


    public ObjectTupleProperty(String name, int size, ObjectProperty.ArrayAllocator<T> allocator) {
        super(name, size);
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


    /*public static <E extends Element, T> ObjectTupleProperty<E, T> get(String name, Class<T> clazz, BMeshData<E> meshData) {
        return (ObjectTupleProperty<E, T>) getProperty(name, meshData, clazz);
    }*/
}