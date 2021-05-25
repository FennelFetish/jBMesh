package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.Element;

public class ShortTupleAttribute<E extends Element> extends BMeshAttribute<E, short[]> {
    public ShortTupleAttribute(String name, int components) {
        super(name, components);
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


    public static <E extends Element> ShortTupleAttribute<E> get(String name, BMeshData<E> meshData) {
        return (ShortTupleAttribute<E>) getAttribute(name, meshData, short[].class);
    }

    public static <E extends Element> ShortTupleAttribute<E> getOrCreate(String name, int components, BMeshData<E> meshData) {
        ShortTupleAttribute<E> attribute = get(name, meshData);

        if(attribute == null) {
            attribute = new ShortTupleAttribute<>(name, components);
            meshData.addAttribute(attribute);
        }
        else if(attribute.numComponents != components)
            throw new IllegalStateException("Attribute with same name but different number of components already exists.");

        return attribute;
    }
}