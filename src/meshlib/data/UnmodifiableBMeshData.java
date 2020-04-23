package meshlib.data;

import java.util.List;

public class UnmodifiableBMeshData<T extends Element> extends BMeshData<T> {
    // TODO: Must wrap, not extend

    public UnmodifiableBMeshData(String name, ElementFactory<T> factory) {
        super(name, factory);
    }


    @Override
    public List<T> elements() {
        // ?
        return null;
    }

    
    @Override
    public T add() {
        throw new UnsupportedOperationException("Unsupported");
    }

    /*@Override
    public void remove(int index) {
        throw new UnsupportedOperationException("Unsupported");
    }*/

    @Override
    public void remove(T element) {
        throw new UnsupportedOperationException("Unsupported");
    }
}
