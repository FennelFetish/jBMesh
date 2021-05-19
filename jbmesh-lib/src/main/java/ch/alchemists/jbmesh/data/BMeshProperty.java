package ch.alchemists.jbmesh.data;

// TODO: Remember dirty state of elements for each property separately.
//       A normal generator could use the dirty state of the vertex-position property to determine which face normals have to be regenerated.
public abstract class BMeshProperty<E extends Element, TArray> {
    public final String name;
    public final int numComponents;

    protected TArray data = null;

    private boolean comparable = true;


    protected BMeshProperty(String name, int numComponents) {
        if(numComponents < 1)
            throw new IllegalArgumentException("Number of components must be at least 1");

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


    public final int indexOf(E element) {
        return element.getIndex() * numComponents;
    }

    public final int indexOf(E element, int component) {
        return (element.getIndex() * numComponents) + component;
    }


    public void copy(E from, E to) {
        int iFrom = indexOf(from);
        int iTo   = indexOf(to);
        System.arraycopy(data, iFrom, data, iTo, numComponents);
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
