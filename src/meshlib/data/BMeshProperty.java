package meshlib.data;

public abstract class BMeshProperty<E extends Element, TArray> {
    public static final class Vertex {
        private Vertex() {}

        public static final String POSITION = "VertexPosition";
        public static final String COLOR    = "VertexColor";
    }

    public static final class Face {
        private Face() {}

        public static final String INDICES_3 = "FaceIndicesTriangle";
        public static final String INDICES_4 = "FaceIndicesQuad";
    }


    public final String name;
    public final int numComponents;

    protected TArray data = null;


    protected BMeshProperty(String name, int numComponents) {
        if(numComponents < 1)
            throw new IllegalArgumentException("Number of components cannot be less than 1");

        this.name = name;
        this.numComponents = numComponents;
    }

    protected BMeshProperty(String name) {
        this(name, 1);
    }


    @SuppressWarnings("unchecked")
    protected static <E extends Element, TArray> BMeshProperty<E, TArray> getProperty(String name, BMeshData<E> meshData, Class<TArray> arrayType) {
        return (BMeshProperty<E, TArray>) meshData.getProperty(name);
    }


    public int indexOf(E element) {
        return element.getIndex() * numComponents;
    }

    public int indexOf(E element, int component) {
        return (element.getIndex() * numComponents) + component;
    }


    public void copy(E from, E to) {
        int iFrom = indexOf(from);
        int iTo = indexOf(to);
        System.arraycopy(data, iFrom, data, iTo, numComponents);
    }


    /**
     * Call BMeshData.compact() first before passing the array to OpenGL.
     * @return Underlying array.
     */
    public TArray array() {
        return data;
    }


    protected abstract TArray alloc(int size);


    /**
     * @param size
     * @return The old data array.
     */
    final TArray allocReplace(int size) {
        TArray oldArray = data;
        data = alloc(size * numComponents);
        return oldArray;
    }

    final void realloc(int size, int copyLength) {
        TArray oldArray = allocReplace(size);
        System.arraycopy(oldArray, 0, data, 0, copyLength * numComponents);
    }

    void release() {
        data = null;
    }
}
