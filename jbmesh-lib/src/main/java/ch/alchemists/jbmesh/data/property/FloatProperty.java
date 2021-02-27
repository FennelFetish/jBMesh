package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.Element;

public class FloatProperty<E extends Element> extends BMeshProperty<E, float[]> {
    private static final float EPSILON = 0.001f;


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
    public boolean equals(E a, E b) {
        return floatEquals(data[a.getIndex()], data[b.getIndex()]);
    }

    
    @Override
    protected float[] alloc(int size) {
        return new float[size];
    }


    public static <E extends Element> FloatProperty<E> get(String name, BMeshData<E> meshData) {
        return (FloatProperty<E>) getProperty(name, meshData, float[].class);
    }

    public static <E extends Element> FloatProperty<E> getOrCreate(String name, BMeshData<E> meshData) {
        FloatProperty<E> prop = get(name, meshData);
        if(prop == null) {
            prop = new FloatProperty<>(name);
            meshData.addProperty(prop);
        }
        return prop;
    }


    public static boolean floatEquals(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b) || Math.abs(a - b) <= EPSILON;
    }
}
