// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.data;

public abstract class Element {
    // This library will start using bits on the left side (MSB).
    // User code should use bits on the right side (LSB).
    public static final int FLAG_VIRTUAL = 1 << 31;


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
        return !checkFlags(FLAG_VIRTUAL);
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
