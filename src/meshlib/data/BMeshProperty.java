package meshlib.data;

public abstract class BMeshProperty<TArray, TElement extends Element> {
    public static final class Vertex {
        private Vertex() {}

        public static final String POSITION = "VertexPosition";
        public static final String COLOR    = "VertexColor";
    }


    public final String name;
    public final int numComponents;

    protected TArray data = null;

    public final Class arrClass = float[].class;


    protected BMeshProperty(String name, BMeshData<TElement> meshData, int numComponents) {
        if(numComponents < 1)
            throw new IllegalArgumentException("Number of components cannot be less than 1");

        this.name = name;
        this.numComponents = numComponents;
        meshData.addProperty(this);
    }

    protected BMeshProperty(String name, BMeshData<TElement> meshData) {
        this(name, meshData, 1);
    }


    /**
     * Call compact first before passing to OpenGL.
     * @return Underlying array.
     */
    public TArray array() {
        return data;
    }


    protected abstract TArray alloc(int size);


    final TArray allocData(int size) {
        System.out.println("allocData '" + name + "': " + (size*numComponents));
        
        TArray oldArray = data;
        data = alloc(size * numComponents);
        return oldArray;
    }

    final void realloc(int size, int copyLength) {
        TArray oldArray = allocData(size);
        System.arraycopy(oldArray, 0, data, 0, copyLength * numComponents);
    }

    void release() {
        data = null;
    }
}
