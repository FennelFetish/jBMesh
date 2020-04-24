package meshlib.data.property;

import meshlib.data.BMeshData;
import meshlib.data.BMeshProperty;
import meshlib.data.Element;

public class FloatProperty<E extends Element> extends BMeshProperty<float[], E> {
    public FloatProperty(String name) {
        super(name);
    }


    public float get(E element) {
        return data[element.getIndex()];
    }

    public void set(E element, float value) {
        data[element.getIndex()] = value;
    }

    
    @Override
    protected float[] alloc(int size) {
        return new float[size];
    }

    public static <E extends Element> FloatProperty<E> get(String name, BMeshData<E> meshData) {
        return (FloatProperty<E>) getProperty(name, meshData, float[].class);
    }
}
