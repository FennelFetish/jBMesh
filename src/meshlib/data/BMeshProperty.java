package meshlib.data;

public abstract class BMeshProperty<TArray, E extends Element> {
    public static final class Vertex {
        private Vertex() {}

        public static final String POSITION = "VertexPosition";
        public static final String COLOR    = "VertexColor";
    }


    public final String name;
    public final int numComponents;

    protected TArray data = null;


    protected BMeshProperty(String name, BMeshData<E> meshData, int numComponents) {
        if(numComponents < 1)
            throw new IllegalArgumentException("Number of components cannot be less than 1");

        this.name = name;
        this.numComponents = numComponents;
        meshData.addProperty(this);
    }

    protected BMeshProperty(String name, BMeshData<E> meshData) {
        this(name, meshData, 1);
    }


    @SuppressWarnings("unchecked")
    protected static <TArray, E extends Element> BMeshProperty<TArray, E> getProperty(String name, BMeshData<E> meshData, Class<TArray> arrayType) {
        return (BMeshProperty<TArray, E>) meshData.getProperty(name);
    }


    public int indexOf(E element) {
        return element.getIndex() * numComponents;
    }

    public int indexOf(E element, int component) {
        return (element.getIndex() * numComponents) + component;
    }


    /**
     * Call compact first before passing to OpenGL.
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
        System.out.println("allocData '" + name + "': " + (size*numComponents));
        
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
