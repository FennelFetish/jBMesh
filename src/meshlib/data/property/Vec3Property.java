package meshlib.data.property;

import com.jme3.math.Vector3f;
import java.io.InvalidClassException;
import meshlib.data.BMeshData;
import meshlib.data.BMeshProperty;
import meshlib.data.Element;

public class Vec3Property<TElement extends Element> extends BMeshProperty<float[], TElement> {
    public Vec3Property(String name, BMeshData<TElement> meshData) {
        super(name, meshData, 3);
    }

    @Override
    protected float[] alloc(int size) {
        return new float[size];
    }


    // getX() setX() ?
    

    public Vector3f get(TElement element) {
        int eleIndex = element.getIndex() * numComponents;
        return new Vector3f(data[eleIndex], data[eleIndex+1], data[eleIndex+2]);
    }
    
    public void get(TElement element, Vector3f store) {
        int eleIndex = element.getIndex() * numComponents;
        store.x = data[eleIndex];
        store.y = data[eleIndex+1];
        store.z = data[eleIndex+2];
    }


    public void set(TElement element, Vector3f vec) {
        set(element, vec.x, vec.y, vec.z);
    }
    
    public void set(TElement element, float x, float y, float z) {
        int eleIndex = element.getIndex() * numComponents;
        data[eleIndex]   = x;
        data[eleIndex+1] = y;
        data[eleIndex+2] = z;
    }


    public static <TElement extends Element> Vec3Property<TElement> get(String name, BMeshData<TElement> meshData) {
        BMeshProperty<?, TElement> prop = meshData.getProperty(name);
        if(prop instanceof Vec3Property)
            return (Vec3Property<TElement>) prop;
        else
            throw new IllegalArgumentException("Property not of requested type");
    }

    
    public static final Class<float[]> TYPE = float[].class;
}
