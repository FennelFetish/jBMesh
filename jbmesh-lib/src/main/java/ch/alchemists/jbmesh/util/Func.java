package ch.alchemists.jbmesh.util;

public class Func {
    public static interface Unary<T> {
        void exec(T v);
    }

    public static interface Binary<T> {
        void exec(T a, T b);
    }
}
