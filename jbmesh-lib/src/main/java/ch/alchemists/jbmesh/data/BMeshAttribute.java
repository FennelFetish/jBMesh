package ch.alchemists.jbmesh.data;

// TODO: Remember dirty state of elements for each attribute separately.
//       A normal generator could use the dirty state of the vertex-position attribute to determine which face normals have to be regenerated.
public abstract class BMeshAttribute<E extends Element, TArray> {
    // Attribute names
    public static final String Position         = "Position";
    public static final String Normal           = "Normal";
    public static final String TexCoord         = "TexCoord";
    public static final String Color            = "Color";
    public static final String Index            = "Index";

    public static final String VertexMap        = "VertexMap";


    public final String name;
    public final int numComponents;

    protected TArray data = null;

    private boolean comparable = true;


    protected BMeshAttribute(String name, int numComponents) {
        if(numComponents < 1)
            throw new IllegalArgumentException("Number of components must be at least 1");

        this.name = name;
        this.numComponents = numComponents;
    }

    protected BMeshAttribute(String name) {
        this(name, 1);
    }


    @SuppressWarnings("unchecked")
    protected static <E extends Element, TArray> BMeshAttribute<E, TArray> getAttribute(String name, BMeshData<E> meshData, Class<TArray> arrayType) {
        // TODO: Check cast?
        return (BMeshAttribute<E, TArray>) meshData.getAttribute(name);
    }


    public final int indexOf(E element) {
        return element.getIndex() * numComponents;
    }

    public final int indexOf(E element, int component) {
        return (element.getIndex() * numComponents) + component;
    }


    // TODO: Benchmark... override in subclasses?
    public void copy(E from, E to) {
        int iFrom = indexOf(from);
        int iTo   = indexOf(to);
        System.arraycopy(data, iFrom, data, iTo, numComponents);
    }

    public <E2 extends Element> void copy(E from, BMeshAttribute<E2, ?> otherAttribute, E2 otherTo) {
        if(numComponents != otherAttribute.numComponents)
            throw new IllegalArgumentException("Number of components don't match.");

        int iFrom = indexOf(from);
        int iTo   = otherAttribute.indexOf(otherTo);
        System.arraycopy(data, iFrom, otherAttribute.data, iTo, numComponents);
    }


    public abstract boolean equals(E a, E b);

    public void setComparable(boolean comparable) {
        this.comparable = comparable;
    }

    public boolean isComparable() {
        return comparable;
    }


    public boolean isAttached() {
        return data != null;
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
