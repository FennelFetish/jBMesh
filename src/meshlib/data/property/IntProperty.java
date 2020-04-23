package meshlib.data.property;

import meshlib.data.BMeshData;
import meshlib.data.BMeshProperty;
import meshlib.data.Element;

public class IntProperty<TElement extends Element> extends BMeshProperty<int[], TElement>{
    public IntProperty(String name, BMeshData<TElement> meshData) {
        super(name, meshData);
    }

    @Override
    protected int[] alloc(int size) {
        return new int[size];
    }


    public int get(TElement element) {
        return data[element.getIndex()];
    }

    public void set(TElement element, int i) {
        data[element.getIndex()] = i;
    }
}
