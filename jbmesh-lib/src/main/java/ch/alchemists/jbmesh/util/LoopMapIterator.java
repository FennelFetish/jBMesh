// Copyright (c) 2020-2022 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.util;

import ch.alchemists.jbmesh.structure.Loop;
import java.util.Iterator;
import java.util.function.Function;

public final class LoopMapIterator<E> implements Iterator<E> {
    private final Iterator<Loop> it;
    private final Function<Loop, E> mapFunc;

    public LoopMapIterator(Iterator<Loop> it, Function<Loop, E> mapFunc) {
        this.it = it;
        this.mapFunc = mapFunc;
    }

    @Override
    public final boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public final E next() {
        return mapFunc.apply(it.next());
    }
}
