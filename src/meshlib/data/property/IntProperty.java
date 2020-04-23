package meshlib.data.property;

import meshlib.data.BMeshData;
import meshlib.data.BMeshProperty;
import meshlib.data.Element;

public class IntProperty<E extends Element> extends BMeshProperty<int[], E>{
    public IntProperty(String name) {
        super(name);
    }


    public int get(E element) {
        return data[element.getIndex()];
    }

    public void set(E element, int i) {
        data[element.getIndex()] = i;
    }


    @Override
    protected int[] alloc(int size) {
        return new int[size];
    }

    public static <E extends Element> IntProperty<E> get(String name, BMeshData<E> meshData) {
        return (IntProperty<E>) getProperty(name, meshData, int[].class);
    }
}
