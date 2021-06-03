// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.Element;

public class ByteAttribute<E extends Element> extends BMeshAttribute<E, byte[]> {
    public ByteAttribute(String name) {
        super(name);
    }


    public byte get(E element) {
        return data[element.getIndex()];
    }

    public void set(E element, byte value) {
        data[element.getIndex()] = value;
    }


    @Override
    public boolean equals(E a, E b) {
        return data[a.getIndex()] == data[b.getIndex()];
    }


    @Override
    protected byte[] alloc(int size) {
        return new byte[size];
    }


    public static <E extends Element> ByteAttribute<E> get(String name, BMeshData<E> meshData) {
        return (ByteAttribute<E>) getAttribute(name, meshData, byte[].class);
    }

    public static <E extends Element> ByteAttribute<E> getOrCreate(String name, BMeshData<E> meshData) {
        ByteAttribute<E> attribute = get(name, meshData);
        if(attribute == null) {
            attribute = new ByteAttribute<>(name);
            meshData.addAttribute(attribute);
        }
        return attribute;
    }
}
