package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.Element;

public class ShortTupleProperty<E extends Element> extends BMeshProperty<E, short[]> {
    public ShortTupleProperty(String name, int size) {
        super(name, size);
    }


    public short get(E element, int component) {
        return data[indexOf(element, component)];
    }


    public void set(E element, int component, short value) {
        data[indexOf(element, component)] = value;
    }

    public void setValues(E element, short... values) {
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
    protected short[] alloc(int size) {
        return new short[size];
    }

    public static <E extends Element> ShortTupleProperty<E> get(String name, BMeshData<E> meshData) {
        return (ShortTupleProperty<E>) getProperty(name, meshData, short[].class);
    }
}