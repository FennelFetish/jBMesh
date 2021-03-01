package ch.alchemists.jbmesh.data;

public abstract class Element {
    public static final int FLAG_VIRTUAL = 1;


    private int index = -1;
    private int flags = 0;


    protected Element() {}


    public final int getIndex() {
        return index;
    }

    final void setIndex(int index) {
        this.index = index;
    }


    public final boolean isAlive() {
        return index >= 0;
    }

    final boolean isListed() {
        return isAlive() && !checkFlags(FLAG_VIRTUAL); // TODO: Remove check for isAlive()
    }


    final void release() {
        index = -1;
        flags = 0;
        releaseElement();
    }

    protected abstract void releaseElement();


    final void setFlags(int flags) {
        this.flags |= flags;
    }

    final void unsetFlags(int flags) {
        this.flags &= ~flags;
    }

    final boolean checkFlags(int flags) {
        return (this.flags & flags) == flags;
    }
}
