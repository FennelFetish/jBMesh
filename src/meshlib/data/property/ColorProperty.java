package meshlib.data.property;

import com.jme3.math.ColorRGBA;
import meshlib.data.BMeshData;
import meshlib.data.BMeshProperty;
import meshlib.data.Element;

public class ColorProperty<TElement extends Element> extends BMeshProperty<float[], TElement> {
    public ColorProperty(String name, BMeshData<TElement> meshData) {
        super(name, meshData, 4);
    }

    @Override
    protected float[] alloc(int size) {
        return new float[size];
    }


    public ColorRGBA get(TElement element) {
        int eleIndex = element.getIndex() * numComponents;
        return new ColorRGBA(data[eleIndex], data[eleIndex+1], data[eleIndex+2], data[eleIndex+3]);
    }

    public void get(TElement element, ColorRGBA store) {
        int eleIndex = element.getIndex() * numComponents;
        store.r = data[eleIndex];
        store.g = data[eleIndex+1];
        store.b = data[eleIndex+2];
        store.a = data[eleIndex+3];
    }


    public void set(TElement element, ColorRGBA color) {
        set(element, color.r, color.g, color.b, color.a);
    }

    public void set(TElement element, float r, float g, float b, float a) {
        int eleIndex = element.getIndex() * numComponents;
        data[eleIndex]   = r;
        data[eleIndex+1] = g;
        data[eleIndex+2] = b;
        data[eleIndex+3] = a;
    }
}
