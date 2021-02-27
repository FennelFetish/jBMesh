package ch.alchemists.jbmesh.util;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

public class PlanarCoordinateSystem {
    private final Vector3f p = new Vector3f();
    private final Vector3f x = new Vector3f();
    private final Vector3f y = new Vector3f();


    public PlanarCoordinateSystem(Vector3f p0, Vector3f p1, Vector3f n) {
        this.p.set(p0);

        x.set(p1).subtractLocal(p0).normalizeLocal();
        y.set(n).crossLocal(x).normalizeLocal();
    }


    public Vector2f project(Vector3f v) {
        return project(v, new Vector2f());
    }

    public Vector2f project(Vector3f v, Vector2f store) {
        Vector3f temp = v.subtract(p);

        store.x = temp.dot(x);
        store.y = temp.dot(y);

        return store;
    }


    public Vector3f unproject(Vector2f v) {
        return unproject(v, new Vector3f());
    }

    public Vector3f unproject(Vector2f v, Vector3f store) {
        // store = (v.x * x) + (v.y * y) + p
        store.x = (v.x * x.x) + (v.y * y.x);
        store.y = (v.x * x.y) + (v.y * y.y);
        store.z = (v.x * x.z) + (v.y * y.z);

        store.addLocal(p);
        return store;
    }
}
