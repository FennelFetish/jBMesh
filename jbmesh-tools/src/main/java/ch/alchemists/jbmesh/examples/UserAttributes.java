// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.examples;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.Element;
import com.jme3.math.Vector2f;

public class UserAttributes {
    // Example user-defined attribute
    public static class LongAttribute<E extends Element> extends BMeshAttribute<E, long[]> {
        public LongAttribute(String name) {
            super(name);
        }

        public long get(E element) {
            return data[element.getIndex()];
        }

        public void set(E element, long l) {
            data[element.getIndex()] = l;
        }

        @Override
        public boolean equals(E a, E b) {
            return data[a.getIndex()] == data[b.getIndex()];
        }

        @Override
        protected long[] alloc(int size) {
            return new long[size];
        }

        public static <E extends Element> LongAttribute<E> get(String name, BMeshData<E> meshData) {
            return (LongAttribute<E>) getAttribute(name, meshData, long[].class);
        }
    }


    // Example of attribute for arbitrary objects
    public static class Vec2TupleAttribute<E extends Element> extends BMeshAttribute<E, Vector2f[]> {
        public Vec2TupleAttribute(String name) {
            super(name, 2);
        }


        public Vector2f getA(E element) {
            return data[indexOf(element)];
        }

        public Vector2f getB(E element) {
            return data[indexOf(element, 1)];
        }


        public void setA(E element, Vector2f vec) {
            data[indexOf(element)] = vec;
        }

        public void setB(E element, Vector2f vec) {
            data[indexOf(element, 1)] = vec;
        }


        @Override
        public boolean equals(E a, E b) {
            return data[indexOf(a, 0)].equals(data[indexOf(b, 0)])
                && data[indexOf(a, 1)].equals(data[indexOf(b, 1)]);
        }


        @Override
        protected Vector2f[] alloc(int size) {
            return new Vector2f[size];
        }

        @Override
        public void copy(E from, E to) {
            int iFrom = indexOf(from);
            int iTo = indexOf(to);

            for(int i=0; i<numComponents; i++) {
                if(data[iTo] == null)
                    data[iTo] = new Vector2f();

                data[iTo].set(data[iFrom]);

                iFrom++;
                iTo++;
            }
        }
    }
}
