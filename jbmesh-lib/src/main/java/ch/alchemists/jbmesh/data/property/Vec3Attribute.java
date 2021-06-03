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

public class Vec3Attribute<E extends Element> extends FloatTupleAttribute<E> {
    public Vec3Attribute(String name) {
        super(name, 3);
    }


    public Vector3f get(E element) {
        int i = indexOf(element);
        return new Vector3f(data[i], data[i+1], data[i+2]);
    }
    
    public Vector3f get(E element, Vector3f store) {
        int i = indexOf(element);
        store.x = data[i];
        store.y = data[i+1];
        store.z = data[i+2];
        return store;
    }


    public void set(E element, Vector3f vec) {
        set(element, vec.x, vec.y, vec.z);
    }
    
    public void set(E element, float x, float y, float z) {
        int i = indexOf(element);
        data[i]   = x;
        data[i+1] = y;
        data[i+2] = z;
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


    /**
     * store = store + element
     */
    public void addLocal(Vector3f store, E element) {
        int i = indexOf(element);
        store.x += data[i];
        store.y += data[i+1];
        store.z += data[i+2];
    }

    /**
     * element = element + v
     */
    public void addLocal(E element, Vector3f v) {
        int i = indexOf(element);
        data[i]   += v.x;
        data[i+1] += v.y;
        data[i+2] += v.z;
    }


    /**
     * store = store - element
     */
    public void subtractLocal(Vector3f store, E element) {
        int i = indexOf(element);
        store.x -= data[i];
        store.y -= data[i+1];
        store.z -= data[i+2];
    }

    /**
     * element = element - v
     */
    public void subtractLocal(E element, Vector3f v) {
        int i = indexOf(element);
        data[i]   -= v.x;
        data[i+1] -= v.y;
        data[i+2] -= v.z;
    }


    public void execute(E element, Func.Unary<Vector3f> op) {
        Vector3f v = get(element);
        op.exec(v);
    }

    public void execute(E element1, E element2, Func.Binary<Vector3f> op) {
        Vector3f v1 = get(element1);
        Vector3f v2 = get(element2);
        op.exec(v1, v2);
    }

    public void forEach(Iterable<E> elements, Func.Unary<Vector3f> op) {
        Vector3f v = new Vector3f();
        for(E element : elements) {
            get(element, v);
            op.exec(v);
        }
    }


    public void modify(E element, Func.Unary<Vector3f> op) {
        Vector3f v = get(element);
        op.exec(v);
        set(element, v);
    }

    public void modify(E element1, E element2, Func.Binary<Vector3f> op) {
        Vector3f v1 = get(element1);
        Vector3f v2 = get(element2);
        op.exec(v1, v2);
        set(element1, v1);
        set(element2, v2);
    }

    public void forEachModify(Iterable<E> elements, Func.Unary<Vector3f> op) {
        Vector3f v = new Vector3f();
        for(E element : elements) {
            get(element, v);
            op.exec(v);
            set(element, v);
        }
    }


    public static <E extends Element> Vec3Attribute<E> get(String name, BMeshData<E> meshData) {
        return (Vec3Attribute<E>) getAttribute(name, meshData, float[].class);
    }

    public static <E extends Element> Vec3Attribute<E> getOrCreate(String name, BMeshData<E> meshData) {
        Vec3Attribute<E> attribute = get(name, meshData);
        if(attribute == null) {
            attribute = new Vec3Attribute<>(name);
            meshData.addAttribute(attribute);
        }
        return attribute;
    }
}
