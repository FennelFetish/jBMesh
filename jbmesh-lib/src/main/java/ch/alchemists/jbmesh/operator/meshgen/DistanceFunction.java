package ch.alchemists.jbmesh.operator.meshgen;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;

public interface DistanceFunction {
    float dist(Vector3f v);


    default float getEpsilon() {
        return 0.0001f;
    }


    /**
     * Also "direction away from nearest point on surface".
     * @param p
     * @param store
     */
    default void normal(Vector3f p, Vector3f store) { // Pointing outwards, towards where dist() is positive
        float e = getEpsilon();
        float e2 = e * 2;

        store.set(p).x += e;
        float dx = dist(store);
        store.x -= e2;
        dx -= dist(store);

        store.set(p).y += e;
        float dy = dist(store);
        store.y -= e2;
        dy -= dist(store);

        store.set(p).z += e;
        float dz = dist(store);
        store.z -= e2;
        dz -= dist(store);

        store.set(dx, dy, dz).normalizeLocal();
    }

    default void tangent(Vector3f p, Vector3f reference, Vector3f store) {
        store.zero();
    }

    default BoundingBox getBounds() {
        return new BoundingBox(Vector3f.ZERO, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    }


    public static class Plane implements DistanceFunction {
        private final Vector3f p = new Vector3f();
        private final Vector3f n = new Vector3f();
        private final Vector3f proj = new Vector3f();

        public Plane(Vector3f p, Vector3f n) {
            this.p.set(p);
            this.n.set(n).normalizeLocal();
        }

        @Override
        public float dist(Vector3f v) {
            proj.set(v).subtractLocal(p);
            return proj.dot(n); // "Outside" is in direction of normal
        }

        @Override
        public void normal(Vector3f p, Vector3f store) {
            store.set(n);
        }

        @Override
        public void tangent(Vector3f p, Vector3f reference, Vector3f store) {
            store.set(p).subtractLocal(reference);
        }

        @Override
        public BoundingBox getBounds() {
            return new BoundingBox(p, 3.0f, 3.0f, 3.0f);
        }
    }


    public static class Sphere implements DistanceFunction {
        private final Vector3f center = new Vector3f();
        private final float radius;

        public Sphere(Vector3f center, float radius) {
            this.center.set(center);
            this.radius = radius;
        }

        @Override
        public float dist(Vector3f v) {
            return center.distance(v) - radius;
        }

        @Override
        public void normal(Vector3f p, Vector3f store) {
            store.set(p).subtractLocal(center).normalizeLocal();
        }

        @Override
        public void tangent(Vector3f p, Vector3f reference, Vector3f store) {
            store.set(reference).subtractLocal(center).normalizeLocal().multLocal(radius);
            //store.set(p).subtractLocal(reference);
            store.zero();
        }

        @Override
        public BoundingBox getBounds() {
            return new BoundingBox(center, radius, radius, radius);
        }
    }


    public static class Ellipsoid implements DistanceFunction {
        private final Vector3f center = new Vector3f();
        private final Vector3f radius = new Vector3f();
        private final Vector3f radiusSquared = new Vector3f();
        private final Vector3f tempA = new Vector3f();
        private final Vector3f tempB = new Vector3f();

        public Ellipsoid(Vector3f center, Vector3f radius) {
            this.center.set(center);
            this.radius.set(radius);
            radiusSquared.set(radius).multLocal(radius);
        }

        @Override
        public float dist(Vector3f v) {
            tempA.set(v).subtractLocal(center);
            tempB.set(tempA);
            float a = tempA.divideLocal(radius).length();
            float b = tempB.divideLocal(radiusSquared).length();
            return a * (a-1.0f) / b;
        }

        @Override
        public BoundingBox getBounds() {
            return new BoundingBox(center, radius.x, radius.y, radius.z);
        }
    }


    public static class Box implements DistanceFunction {
        private final Vector3f p = new Vector3f();
        private final Vector3f size = new Vector3f();
        private final Vector3f temp = new Vector3f();

        public Box(Vector3f p, Vector3f size) {
            this.p.set(p);
            this.size.set(size);
            //this.size.multLocal(0.5f);
        }

        @Override
        public float dist(Vector3f v) {
            temp.set(v).subtractLocal(p);
            temp.x = Math.abs(temp.x);
            temp.y = Math.abs(temp.y);
            temp.z = Math.abs(temp.z);
            temp.subtractLocal(size);

            float f = Math.max(temp.y, temp.z);
            f = Math.max(temp.x, f);
            f = Math.min(f, 0.0f);

            temp.x = Math.max(temp.x, 0.0f);
            temp.y = Math.max(temp.y, 0.0f);
            temp.z = Math.max(temp.z, 0.0f);

            return temp.length() + f;
        }

        @Override
        public BoundingBox getBounds() {
            return new BoundingBox(p, size.x, size.y, size.z);
        }
    }
}
