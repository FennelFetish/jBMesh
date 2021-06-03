// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.util;

import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;

public class Func {
    @FunctionalInterface
    public interface Unary<T> {
        void exec(T v);
    }

    @FunctionalInterface
    public interface Binary<T> {
        void exec(T a, T b);
    }


    @FunctionalInterface
    public interface MapVec3<T> {
        Vector3f get(T element, Vector3f store);
    }

    @FunctionalInterface
    public interface MapVertex<T> {
        Vertex get(T element);
    }
 }
