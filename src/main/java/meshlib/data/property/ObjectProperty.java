package meshlib.data.property;

import meshlib.data.BMeshProperty;
import meshlib.data.Element;

public abstract class ObjectProperty<E extends Element, T> extends BMeshProperty<E, T[]> {
    protected ObjectProperty(String name) {
        super(name);
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
}
