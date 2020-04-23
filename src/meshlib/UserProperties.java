package meshlib;

import com.jme3.math.Vector2f;
import meshlib.data.BMeshData;
import meshlib.data.BMeshProperty;
import meshlib.data.Element;
import meshlib.structure.Vertex;

public class UserProperties {
    // Example user-defined property
    public static class LongProperty<TElement extends Element> extends BMeshProperty<long[], TElement> {

        // Make it: public static class LongProperty extends BMeshProperty<long[], Vertex> {
        // How to make constructor?


        public LongProperty(String name, BMeshData<TElement> meshData) {
            super(name, meshData);
        }

        @Override
        protected long[] alloc(int size) {
            return new long[size];
        }

        public long get(TElement element) {
            return data[element.getIndex()];
        }

        public void set(TElement element, long l) {
            data[element.getIndex()] = l;
        }
    }


    // Properties for arbitrary objects
    public static class Vec2TupleProperty<TElement extends Element> extends BMeshProperty<Vector2f[], TElement> {
        public Vec2TupleProperty(String name, BMeshData<TElement> meshData) {
            super(name, meshData, 2);
        }

        @Override
        protected Vector2f[] alloc(int size) {
            return new Vector2f[size];
        }

        
        public Vector2f getA(TElement element) {
            int eleIndex = element.getIndex() * numComponents;
            return data[eleIndex];
        }

        public Vector2f getB(TElement element) {
            int eleIndex = element.getIndex() * numComponents;
            return data[eleIndex+1];
        }


        public void setA(TElement element, Vector2f vec) {
            int eleIndex = element.getIndex() * numComponents;
            data[eleIndex] = vec;
        }

        public void setB(TElement element, Vector2f vec) {
            int eleIndex = element.getIndex() * numComponents;
            data[eleIndex+1] = vec;
        }
    }
}
