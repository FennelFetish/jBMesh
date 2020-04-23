package meshlib.structure;

public abstract class BMeshProperty<TArr> {
    public static final class Vertex {
        private Vertex() {}

        public static final String POSITION = "Position";
        public static final String COLOR    = "Color";
    }



    static interface DataAllocator {
        Object alloc(int size);
    }

    public static enum Type {
        Float(s -> new float[s]),
        Double(s -> new double[s]),
        Integer(s -> new int[s]),
        Long(s -> new long[s]),
        Boolean(s -> new boolean[s]),
        String(s -> new String[s]);

        final DataAllocator allocator;

        private Type(DataAllocator allocator) {
            this.allocator = allocator;
        }
    }


    public final String name;
    public final int numComponents;
    public final Type type;

    protected TArr data;


    protected BMeshProperty(String name, Type type, int numComponents) {
        this.name = name;
        this.type = type;
        this.numComponents = numComponents;
    }
}
