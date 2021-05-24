package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.Element;

public class BooleanAttribute<E extends Element> extends BMeshAttribute<E, boolean[]> {
    public BooleanAttribute(String name) {
        super(name);
    }


    public boolean get(E element) {
        return data[element.getIndex()];
    }

    public void set(E element, boolean value) {
        data[element.getIndex()] = value;
    }


    @Override
    public boolean equals(E a, E b) {
        return data[a.getIndex()] == data[b.getIndex()];
    }


    @Override
    protected boolean[] alloc(int size) {
        return new boolean[size];
    }

    public static <E extends Element> BooleanAttribute<E> get(String name, BMeshData<E> meshData) {
        return (BooleanAttribute<E>) getAttribute(name, meshData, boolean[].class);
    }

    public static <E extends Element> BooleanAttribute<E> getOrCreate(String name, BMeshData<E> meshData) {
        BooleanAttribute<E> attribute = get(name, meshData);
        if(attribute == null) {
            attribute = new BooleanAttribute<>(name);
            meshData.addAttribute(attribute);
        }
        return attribute;
    }
}
