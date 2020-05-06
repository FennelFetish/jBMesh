package meshlib.data.property;

import com.jme3.math.Vector3f;
import meshlib.data.BMeshData;
import meshlib.data.Element;
import meshlib.util.Func;

public class Vec3Property<E extends Element> extends FloatTupleProperty<E> {
    private final Vector3f tempA = new Vector3f();
    private final Vector3f tempB = new Vector3f();
    

    public Vec3Property(String name) {
        super(name, 3);
    }


    public Vector3f get(E element) {
        int i = indexOf(element);
        return new Vector3f(data[i], data[i+1], data[i+2]);
    }
    
    public void get(E element, Vector3f store) {
        int i = indexOf(element);
        store.x = data[i];
        store.y = data[i+1];
        store.z = data[i+2];
    }


    public void set(E element, Vector3f vec) {
        set(element, vec.x, vec.y, vec.z);
    }
    
    public void set(E element, float x, float y, float z) {
        int i = indexOf(element);
        data[i]   = x;
        data[i+1] = y;
        data[i+2] = z;
    }


    public float getX(E element) {
        return get(element, 0);
    }

    public void setX(E element, float x) {
        set(element, 0, x);
    }

    public float getY(E element) {
        return get(element, 1);
    }

    public void setY(E element, float y) {
        set(element, 1, y);
    }

    public float getZ(E element) {
        return get(element, 2);
    }

    public void setZ(E element, float z) {
        set(element, 2, z);
    }


    public void add(E element, Vector3f store) {
        int i = indexOf(element);
        store.x += data[i];
        store.y += data[i+1];
        store.z += data[i+2];
    }

    public void subtract(E element, Vector3f store) {
        int i = indexOf(element);
        store.x -= data[i];
        store.y -= data[i+1];
        store.z -= data[i+2];
    }


    public void execute(E element, Func.Unary<Vector3f> op) {
        get(element, tempA);
        op.exec(tempA);
        set(element, tempA);
    }


    public void execute(E element1, E element2, Func.Binary<Vector3f> op) {
        get(element1, tempA);
        get(element2, tempB);
        op.exec(tempA, tempB);
        set(element1, tempA);
        set(element2, tempB);
    }


    public static <E extends Element> Vec3Property<E> get(String name, BMeshData<E> meshData) {
        return (Vec3Property<E>) getProperty(name, meshData, float[].class);
    }
}
