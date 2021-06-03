// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.data.property;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.util.Func;
import com.jme3.math.Vector2f;

public class Vec2Attribute<E extends Element> extends FloatTupleAttribute<E> {
    public Vec2Attribute(String name) {
        super(name, 2);
    }


    public Vector2f get(E element) {
        int i = indexOf(element);
        return new Vector2f(data[i], data[i+1]);
    }

    public Vector2f get(E element, Vector2f store) {
        int i = indexOf(element);
        store.x = data[i];
        store.y = data[i+1];
        return store;
    }


    public void set(E element, Vector2f vec) {
        set(element, vec.x, vec.y);
    }

    public void set(E element, float x, float y) {
        int i = indexOf(element);
        data[i]   = x;
        data[i+1] = y;
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


    public void addLocal(Vector2f store, E element) {
        int i = indexOf(element);
        store.x += data[i];
        store.y += data[i+1];
    }

    public void addLocal(E element, Vector2f v) {
        int i = indexOf(element);
        data[i]   += v.x;
        data[i+1] += v.y;
    }


    public void subtractLocal(Vector2f store, E element) {
        int i = indexOf(element);
        store.x -= data[i];
        store.y -= data[i+1];
    }

    public void subtractLocal(E element, Vector2f v) {
        int i = indexOf(element);
        data[i]   -= v.x;
        data[i+1] -= v.y;
    }


    public void execute(E element, Func.Unary<Vector2f> op) {
        Vector2f v = get(element);
        op.exec(v);
    }

    public void execute(E element1, E element2, Func.Binary<Vector2f> op) {
        Vector2f v1 = get(element1);
        Vector2f v2 = get(element2);
        op.exec(v1, v2);
    }

    public void forEach(Iterable<E> elements, Func.Unary<Vector2f> op) {
        Vector2f v = new Vector2f();
        for(E element : elements) {
            get(element, v);
            op.exec(v);
        }
    }


    public void modify(E element, Func.Unary<Vector2f> op) {
        Vector2f v = get(element);
        op.exec(v);
        set(element, v);
    }

    public void modify(E element1, E element2, Func.Binary<Vector2f> op) {
        Vector2f v1 = get(element1);
        Vector2f v2 = get(element2);
        op.exec(v1, v2);
        set(element1, v1);
        set(element2, v2);
    }

    public void forEachModify(Iterable<E> elements, Func.Unary<Vector2f> op) {
        Vector2f v = new Vector2f();
        for(E element : elements) {
            get(element, v);
            op.exec(v);
            set(element, v);
        }
    }


    public static <E extends Element> Vec2Attribute<E> get(String name, BMeshData<E> meshData) {
        return (Vec2Attribute<E>) getAttribute(name, meshData, float[].class);
    }

    public static <E extends Element> Vec2Attribute<E> getOrCreate(String name, BMeshData<E> meshData) {
        Vec2Attribute<E> attribute = get(name, meshData);
        if(attribute == null) {
            attribute = new Vec2Attribute<>(name);
            meshData.addAttribute(attribute);
        }
        return attribute;
    }
}
