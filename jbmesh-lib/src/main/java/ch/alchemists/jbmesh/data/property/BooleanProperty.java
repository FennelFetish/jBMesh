package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.Element;

public class BooleanProperty<E extends Element> extends BMeshProperty<E, boolean[]> {
    public BooleanProperty(String name) {
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

    public static <E extends Element> BooleanProperty<E> get(String name, BMeshData<E> meshData) {
        return (BooleanProperty<E>) getProperty(name, meshData, boolean[].class);
    }

    public static <E extends Element> BooleanProperty<E> getOrCreate(String name, BMeshData<E> meshData) {
        BooleanProperty<E> prop = get(name, meshData);
        if(prop == null) {
            prop = new BooleanProperty<>(name);
            meshData.addProperty(prop);
        }
        return prop;
    }
}
