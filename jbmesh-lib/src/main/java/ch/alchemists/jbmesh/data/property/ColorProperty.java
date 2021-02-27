package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.Element;
import com.jme3.math.ColorRGBA;

public class ColorProperty<E extends Element> extends FloatTupleProperty<E> {
    public ColorProperty(String name) {
        super(name, 4);
    }


    public ColorRGBA get(E element) {
        int i = indexOf(element);
        return new ColorRGBA(data[i], data[i+1], data[i+2], data[i+3]);
    }

    public void get(E element, ColorRGBA store) {
        int i = indexOf(element);
        store.r = data[i];
        store.g = data[i+1];
        store.b = data[i+2];
        store.a = data[i+3];
    }


    public void set(E element, ColorRGBA color) {
        set(element, color.r, color.g, color.b, color.a);
    }

    public void set(E element, float r, float g, float b, float a) {
        int i = indexOf(element);
        data[i]   = r;
        data[i+1] = g;
        data[i+2] = b;
        data[i+3] = a;
    }


    public static <E extends Element> ColorProperty<E> get(String name, BMeshData<E> meshData) {
        return (ColorProperty<E>) getProperty(name, meshData, float[].class);
    }
}
