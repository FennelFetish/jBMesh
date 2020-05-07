package meshlib.data.property;

import meshlib.data.BMeshData;
import meshlib.data.BMeshProperty;
import meshlib.data.Element;

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
}
