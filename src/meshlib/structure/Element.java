package meshlib.structure;

public abstract class Element {
    private int index;


    protected Element() {}


    public int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }


    void release() {
        index = -1;
        releaseElement();
    }

    protected abstract void releaseElement();
}
