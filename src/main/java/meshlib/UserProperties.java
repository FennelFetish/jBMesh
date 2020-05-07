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

        public long get(E element) {
            return data[element.getIndex()];
        }

        public void set(E element, long l) {
            data[element.getIndex()] = l;
        }

        @Override
        public boolean equals(E a, E b) {
            return data[a.getIndex()] == data[b.getIndex()];
        }

        @Override
        protected long[] alloc(int size) {
            return new long[size];
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


        public Vector2f getA(E element) {
            return data[indexOf(element)];
        }

        public Vector2f getB(E element) {
            return data[indexOf(element, 1)];
        }


        public void setA(E element, Vector2f vec) {
            data[indexOf(element)] = vec;
        }

        public void setB(E element, Vector2f vec) {
            data[indexOf(element, 1)] = vec;
        }


        @Override
        public boolean equals(E a, E b) {
            return data[indexOf(a, 0)].equals(data[indexOf(b, 0)])
                && data[indexOf(a, 1)].equals(data[indexOf(b, 1)]);
        }


        @Override
        protected Vector2f[] alloc(int size) {
            return new Vector2f[size];
        }

        @Override
        public void copy(E from, E to) {
            int iFrom = indexOf(from);
            int iTo = indexOf(to);

            for(int i=0; i<numComponents; ++i) {
                if(data[iTo] == null)
                    data[iTo] = new Vector2f();

                data[iTo].set(data[iFrom+i]);
                iTo++;
            }
        }
    }
}
