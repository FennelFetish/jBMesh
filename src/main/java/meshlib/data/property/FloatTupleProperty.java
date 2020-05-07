package meshlib.data.property;

import meshlib.data.BMeshData;
import meshlib.data.BMeshProperty;
import meshlib.data.Element;

public class FloatTupleProperty<E extends Element> extends BMeshProperty<E, float[]> {
    public FloatTupleProperty(String name, int size) {
        super(name, size);
    }


    public float get(E element, int component) {
        return data[indexOf(element, component)];
    }


    public void set(E element, int component, float value) {
        data[indexOf(element, component)] = value;
    }

    public void set(E element, float... values) {
        // throw?
        assert values.length == numComponents;

        int index = indexOf(element);
        for(int i=0; i<numComponents; ++i)
            data[index++] = values[i];
    }


    @Override
    public boolean equals(E a, E b) {
        int indexA = indexOf(a);
        int indexB = indexOf(b);

        for(int i=0; i<numComponents; ++i) {
            if(!FloatProperty.floatEquals(data[indexA++], data[indexB++]))
                return false;
        }

        return true;
    }


    @Override
    protected float[] alloc(int size) {
        return new float[size];
    }

    public static <E extends Element> FloatTupleProperty<E> get(String name, BMeshData<E> meshData) {
        return (FloatTupleProperty<E>) getProperty(name, meshData, float[].class);
    }
}
