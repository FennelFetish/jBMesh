package ch.alchemists.jbmesh.util;

import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;

public class Func {
    @FunctionalInterface
    public interface Unary<T> {
        void exec(T v);
    }

    @FunctionalInterface
    public interface Binary<T> {
        void exec(T a, T b);
    }


    @FunctionalInterface
    public interface MapVec3<T> {
        Vector3f get(T element, Vector3f store);
    }

    @FunctionalInterface
    public interface MapVertex<T> {
        Vertex get(T element);
    }
 }
