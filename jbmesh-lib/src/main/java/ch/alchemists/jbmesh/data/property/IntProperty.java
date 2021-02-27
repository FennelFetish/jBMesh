package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.Element;

public class IntProperty<E extends Element> extends BMeshProperty<E, int[]> {
    public IntProperty(String name) {
        super(name);
    }


    public int get(E element) {
        return data[element.getIndex()];
    }

    public void set(E element, int value) {
        data[element.getIndex()] = value;
    }


    @Override
    public boolean equals(E a, E b) {
        return data[a.getIndex()] == data[b.getIndex()];
    }


    @Override
    protected int[] alloc(int size) {
        return new int[size];
    }

    public static <E extends Element> IntProperty<E> get(String name, BMeshData<E> meshData) {
        return (IntProperty<E>) getProperty(name, meshData, int[].class);
    }
}
