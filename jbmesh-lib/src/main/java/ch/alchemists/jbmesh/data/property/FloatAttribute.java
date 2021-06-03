// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.Element;

public class FloatAttribute<E extends Element> extends BMeshAttribute<E, float[]> {
    private static final float EPSILON = 0.001f;


    public FloatAttribute(String name) {
        super(name);
    }


    public float get(E element) {
        return data[element.getIndex()];
    }

    public void set(E element, float value) {
        data[element.getIndex()] = value;
    }


    @Override
    public boolean equals(E a, E b) {
        return floatEquals(data[a.getIndex()], data[b.getIndex()]);
    }

    
    @Override
    protected float[] alloc(int size) {
        return new float[size];
    }


    public static <E extends Element> FloatAttribute<E> get(String name, BMeshData<E> meshData) {
        return (FloatAttribute<E>) getAttribute(name, meshData, float[].class);
    }

    public static <E extends Element> FloatAttribute<E> getOrCreate(String name, BMeshData<E> meshData) {
        FloatAttribute<E> attribute = get(name, meshData);
        if(attribute == null) {
            attribute = new FloatAttribute<>(name);
            meshData.addAttribute(attribute);
        }
        return attribute;
    }


    public static boolean floatEquals(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b) || Math.abs(a - b) <= EPSILON;
    }
}
