package meshlib.data;

public abstract class Element {
    public static final int FLAG_VIRTUAL = 1;


    private int index = -1;
    private int flags = 0;


    protected Element() {}


    public int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }


    public boolean isAlive() {
        return index >= 0;
    }

    boolean isListed() {
        return isAlive() && !checkFlags(FLAG_VIRTUAL);
    }


    final void release() {
        index = -1;
        flags = 0;
        releaseElement();
    }

    protected abstract void releaseElement();


    public void setFlags(int flags) {
        this.flags |= flags;
    }

    public void unsetFlags(int flags) {
        this.flags &= ~flags;
    }

    public boolean checkFlags(int flags) {
        return (this.flags & flags) == flags;
    }
}
