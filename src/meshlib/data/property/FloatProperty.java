package meshlib.data.property;

import meshlib.data.BMeshData;
import meshlib.data.BMeshProperty;
import meshlib.data.Element;

public class FloatProperty<TElement extends Element> extends BMeshProperty<float[], TElement> {
    public FloatProperty(String name, BMeshData<TElement> meshData) {
        super(name, meshData);
    }

    @Override
    protected float[] alloc(int size) {
        return new float[size];
    }


    public float get(TElement element) {
        return data[element.getIndex()];
    }

    public void set(TElement element, float f) {
        data[element.getIndex()] = f;
    }
}
