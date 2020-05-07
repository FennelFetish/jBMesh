package meshlib.data.property;

import meshlib.data.BMeshData;
import meshlib.data.BMeshProperty;
import meshlib.data.Element;

public class IntTupleProperty<E extends Element> extends BMeshProperty<E, int[]> {
    public IntTupleProperty(String name, int size) {
        super(name, size);
    }


    public int get(E element, int component) {
        return data[indexOf(element, component)];
    }

    
    public void set(E element, int component, int value) {
        data[indexOf(element, component)] = value;
    }

    public void set(E element, int... values) {
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
            if(data[indexA++] != data[indexB++])
                return false;
        }

        return true;
    }


    @Override
    protected int[] alloc(int size) {
        return new int[size];
    }

    public static <E extends Element> IntTupleProperty<E> get(String name, BMeshData<E> meshData) {
        return (IntTupleProperty<E>) getProperty(name, meshData, int[].class);
    }
}
