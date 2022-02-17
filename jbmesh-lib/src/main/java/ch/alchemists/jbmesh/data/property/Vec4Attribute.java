// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.util.Func;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

public class Vec4Attribute<E extends Element> extends FloatTupleAttribute<E> {
    public Vec4Attribute(String name) {
        super(name, 4);
    }


    public Vector4f get(E element) {
        int i = indexOf(element);
        return new Vector4f(data[i], data[i + 1], data[i + 2], data[i + 3]);
    }

    public Vector4f get(E element, Vector4f store) {
        int i = indexOf(element);
        store.x = data[i];
        store.y = data[i + 1];
        store.z = data[i + 2];
        store.w = data[i + 3];
        return store;
    }


    public void set(E element, Vector4f vec) {
        set(element, vec.x, vec.y, vec.z, vec.w);
    }

    public void set(E element, float x, float y, float z, float w) {
        int i = indexOf(element);
        data[i] = x;
        data[i + 1] = y;
        data[i + 2] = z;
        data[i + 3] = w;
    }


    public float getX(E element) {
        return getComponent(element, 0);
    }

    public void setX(E element, float x) {
        setComponent(element, 0, x);
    }

    public float getY(E element) {
        return getComponent(element, 1);
    }

    public void setY(E element, float y) {
        setComponent(element, 1, y);
    }

    public float getZ(E element) {
        return getComponent(element, 2);
    }

    public void setZ(E element, float z) {
        setComponent(element, 2, z);
    }

    public float getW(E element) {
        return getComponent(element, 3);
    }

    public void setW(E element, float w) {
        setComponent(element, 3, w);
    }


    /**
     * store = store + element
     */
    public void addLocal(Vector4f store, E element) {
        int i = indexOf(element);
        store.x += data[i];
        store.y += data[i + 1];
        store.z += data[i + 2];
        store.w += data[i + 3];
    }

    /**
     * element = element + v
     */
    public void addLocal(E element, Vector4f v) {
        int i = indexOf(element);
        data[i] += v.x;
        data[i + 1] += v.y;
        data[i + 2] += v.z;
        data[i + 3] += v.w;
    }


    /**
     * store = store - element
     */
    public void subtractLocal(Vector4f store, E element) {
        int i = indexOf(element);
        store.x -= data[i];
        store.y -= data[i + 1];
        store.z -= data[i + 2];
        store.w -= data[i + 3];
    }

    /**
     * element = element - v
     */
    public void subtractLocal(E element, Vector4f v) {
        int i = indexOf(element);
        data[i] -= v.x;
        data[i + 1] -= v.y;
        data[i + 2] -= v.z;
        data[i + 3] -= v.w;
    }


    public void execute(E element, Func.Unary<Vector4f> op) {
        Vector4f v = get(element);
        op.exec(v);
    }

    public void execute(E element1, E element2, Func.Binary<Vector4f> op) {
        Vector4f v1 = get(element1);
        Vector4f v2 = get(element2);
        op.exec(v1, v2);
    }

    public void forEach(Iterable<E> elements, Func.Unary<Vector4f> op) {
        Vector4f v = new Vector4f();
        for(E element : elements) {
            get(element, v);
            op.exec(v);
        }
    }


    public void modify(E element, Func.Unary<Vector4f> op) {
        Vector4f v = get(element);
        op.exec(v);
        set(element, v);
    }

    public void modify(E element1, E element2, Func.Binary<Vector4f> op) {
        Vector4f v1 = get(element1);
        Vector4f v2 = get(element2);
        op.exec(v1, v2);
        set(element1, v1);
        set(element2, v2);
    }

    public void forEachModify(Iterable<E> elements, Func.Unary<Vector4f> op) {
        Vector4f v = new Vector4f();
        for(E element : elements) {
            get(element, v);
            op.exec(v);
            set(element, v);
        }
    }


    public static <E extends Element> Vec4Attribute<E> get(String name, BMeshData<E> meshData) {
        return (Vec4Attribute<E>) getAttribute(name, meshData, float[].class);
    }

    public static <E extends Element> Vec4Attribute<E> getOrCreate(String name, BMeshData<E> meshData) {
        Vec4Attribute<E> attribute = get(name, meshData);
        if(attribute == null) {
            attribute = new Vec4Attribute<>(name);
            meshData.addAttribute(attribute);
        }
        return attribute;
    }
}
