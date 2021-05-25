package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.Element;

public class FloatTupleAttribute<E extends Element> extends BMeshAttribute<E, float[]> {
    public FloatTupleAttribute(String name, int components) {
        super(name, components);
    }


    public float get(E element, int component) {
        return data[indexOf(element, component)];
    }


    public void set(E element, int component, float value) {
        data[indexOf(element, component)] = value;
    }

    public void setValues(E element, float... values) {
        if(values.length != numComponents)
            throw new IllegalArgumentException("Number of values does not match number of components.");

        int index = indexOf(element);
        for(int i=0; i<numComponents; ++i)
            data[index++] = values[i];
    }


    @Override
    public boolean equals(E a, E b) {
        int indexA = indexOf(a);
        int indexB = indexOf(b);

        for(int i=0; i<numComponents; ++i) {
            if(!FloatAttribute.floatEquals(data[indexA++], data[indexB++]))
                return false;
        }

        return true;
    }


    @Override
    protected float[] alloc(int size) {
        return new float[size];
    }


    public static <E extends Element> FloatTupleAttribute<E> get(String name, BMeshData<E> meshData) {
        return (FloatTupleAttribute<E>) getAttribute(name, meshData, float[].class);
    }

    public static <E extends Element> FloatTupleAttribute<E> getOrCreate(String name, int components, BMeshData<E> meshData) {
        FloatTupleAttribute<E> attribute = get(name, meshData);

        if(attribute == null) {
            attribute = new FloatTupleAttribute<>(name, components);
            meshData.addAttribute(attribute);
        }
        else if(attribute.numComponents != components)
            throw new IllegalStateException("Attribute with same name but different number of components already exists.");

        return attribute;
    }
}
