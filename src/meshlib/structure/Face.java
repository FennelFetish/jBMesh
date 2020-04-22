package meshlib.structure;

public class Face {
    private int index;

    // Never NULL
    public Loop loop;


    private Face() {}


    static final BMeshData.ElementAccessor<Face> ACCESSOR = new BMeshData.ElementAccessor<Face>() {
        @Override
        public Face create() {
            return new Face();
        }

        @Override
        public void release(Face element) {
            element.loop = null;
        }

        @Override
        public int getIndex(Face element) {
            return element.index;
        }

        @Override
        public void setIndex(Face element, int index) {
            element.index = index;
        }
    };
}
