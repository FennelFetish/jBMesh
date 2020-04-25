package meshlib;

import com.jme3.math.Vector2f;
import meshlib.data.BMeshData;
import meshlib.data.BMeshProperty;
import meshlib.data.Element;

public class UserProperties {
    // Example user-defined property
    public static class LongProperty<E extends Element> extends BMeshProperty<E, long[]> {
        public LongProperty(String name) {
            super(name);
        }

        @Override
        protected long[] alloc(int size) {
            return new long[size];
        }

        public long get(E element) {
            return data[element.getIndex()];
        }

        public void set(E element, long l) {
            data[element.getIndex()] = l;
        }

        public static <E extends Element> LongProperty<E> get(String name, BMeshData<E> meshData) {
            return (LongProperty<E>) getProperty(name, meshData, long[].class);
        }
    }


    // Example of property for arbitrary objects
    public static class Vec2TupleProperty<E extends Element> extends BMeshProperty<E, Vector2f[]> {
        public Vec2TupleProperty(String name) {
            super(name, 2);
        }

        @Override
        protected Vector2f[] alloc(int size) {
            return new Vector2f[size];
        }

        
        public Vector2f getA(E element) {
            int eleIndex = element.getIndex() * numComponents;
            return data[eleIndex];
        }

        public Vector2f getB(E element) {
            int eleIndex = element.getIndex() * numComponents;
            return data[eleIndex+1];
        }


        public void setA(E element, Vector2f vec) {
            int eleIndex = element.getIndex() * numComponents;
            data[eleIndex] = vec;
        }

        public void setB(E element, Vector2f vec) {
            int eleIndex = element.getIndex() * numComponents;
            data[eleIndex+1] = vec;
        }
    }
}
