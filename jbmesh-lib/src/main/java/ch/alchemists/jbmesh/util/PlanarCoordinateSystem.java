package ch.alchemists.jbmesh.util;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

public class PlanarCoordinateSystem {
    private final Vector3f p = new Vector3f();
    private final Vector3f x = new Vector3f();
    private final Vector3f y = new Vector3f();


    private PlanarCoordinateSystem() {}


    @Deprecated
    public PlanarCoordinateSystem(Vector3f p0, Vector3f p1, Vector3f n) {
        assert !p0.isSimilar(p1, 0.0001f);

        this.p.set(p0);

        x.set(p1).subtractLocal(p0).normalizeLocal();
        y.set(n).crossLocal(x).normalizeLocal();
    }


    public static PlanarCoordinateSystem withX(Vector3f x, Vector3f n) {
        PlanarCoordinateSystem coordSys = new PlanarCoordinateSystem();
        coordSys.x.set(x);
        coordSys.y.set(n).crossLocal(x);
        return coordSys;
    }

    public static PlanarCoordinateSystem withX(Vector3f p0, Vector3f p1, Vector3f n) {
        assert !p0.isSimilar(p1, 0.0001f);

        PlanarCoordinateSystem coordSys = new PlanarCoordinateSystem();
        coordSys.p.set(p0);
        coordSys.x.set(p1).subtractLocal(p0).normalizeLocal();
        coordSys.y.set(n).crossLocal(coordSys.x).normalizeLocal();
        return coordSys;
    }


    public static PlanarCoordinateSystem withY(Vector3f y, Vector3f n) {
        PlanarCoordinateSystem coordSys = new PlanarCoordinateSystem();
        coordSys.y.set(y);
        coordSys.x.set(y).crossLocal(n);
        return coordSys;
    }

    public static PlanarCoordinateSystem withY(Vector3f p0, Vector3f p1, Vector3f n) {
        assert !p0.isSimilar(p1, 0.0001f);

        PlanarCoordinateSystem coordSys = new PlanarCoordinateSystem();
        coordSys.p.set(p0);
        coordSys.y.set(p1).subtractLocal(p0).normalizeLocal();
        coordSys.x.set(coordSys.y).crossLocal(n).normalizeLocal();
        return coordSys;
    }


    public Vector2f project(Vector3f v) {
        return project(v, new Vector2f());
    }

    public Vector2f project(Vector3f v, Vector2f store) {
        Vector3f diff = v.subtract(p);

        store.x = diff.dot(x);
        store.y = diff.dot(y);

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


    public void rotate(float angle) {
        Quaternion rot = new Quaternion();
        rot.fromAngleAxis(angle, Vector3f.UNIT_Z);
        rot.multLocal(x);
        rot.multLocal(y);
    }


    @Override
    public String toString() {
        return "PlanarCoordinateSystem{x: " + x + " (" + x.length() + "), y: " + y + " (" + y.length() + ")}";
    }
}
