// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.util;

import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.operator.normalgen.NewellNormal;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.Iterator;

public class PlanarCoordinateSystem implements Cloneable {
    private static final float AXIS_LENGTH_EPSILON = 0.001f;
    private static final float AXIS_LENGTH_EPSILON_SQUARED = AXIS_LENGTH_EPSILON * AXIS_LENGTH_EPSILON;

    private static final float MIN_VERTEX_DISTANCE = 0.00001f;
    private static final float MIN_VERTEX_DISTANCE_SQUARED = MIN_VERTEX_DISTANCE * MIN_VERTEX_DISTANCE;


    public final Vector3f p = new Vector3f();
    public final Vector3f x = new Vector3f(1, 0, 0);
    public final Vector3f y = new Vector3f(0, 1, 0);


    public PlanarCoordinateSystem() {}


    /**
     * Plane normal: [0, 0, 1]
     * @return A new coordinate system for the X/Y plane.
     */
    public static PlanarCoordinateSystem XY() {
        return new PlanarCoordinateSystem();
    }

    /**
     * Plane normal: [0, 1, 0]
     * @return A new coordinate system for the X/Z plane.
     */
    public static PlanarCoordinateSystem XZ() {
        PlanarCoordinateSystem coordSys = new PlanarCoordinateSystem();
        coordSys.x.set(0, 0, 1);
        coordSys.y.set(1, 0, 0);
        return coordSys;
    }

    /**
     * Plane normal: [1, 0, 0]
     * @return A new coordinate system for the Y/Z plane.
     */
    public static PlanarCoordinateSystem YZ() {
        PlanarCoordinateSystem coordSys = new PlanarCoordinateSystem();
        coordSys.x.set(0, 0, -1);
        coordSys.y.set(0, 1, 0);
        return coordSys;
    }


    private void validate() {
        if(Math.abs(1f - x.lengthSquared()) > AXIS_LENGTH_EPSILON_SQUARED)
            throw new IllegalArgumentException("Invalid X axis (normalized?)");
        if(Math.abs(1f - y.lengthSquared()) > AXIS_LENGTH_EPSILON_SQUARED)
            throw new IllegalArgumentException("Invalid Y axis (normalized?)");
    }


    public PlanarCoordinateSystem withX(Vector3f x, Vector3f n) {
        this.p.zero();
        this.x.set(x);
        this.y.set(n).crossLocal(x).normalizeLocal(); // TODO: Does it matter if x/y are not normalized?
        validate();
        return this;
    }

    public PlanarCoordinateSystem withXAt(Vector3f p, Vector3f x, Vector3f n) {
        this.p.set(p);
        this.x.set(x);
        this.y.set(n).crossLocal(x).normalizeLocal(); // TODO: Does it matter if x/y are not normalized?
        validate();
        return this;
    }

    public PlanarCoordinateSystem withXDifference(Vector3f xStart, Vector3f xEnd, Vector3f n) {
        if(xStart.isSimilar(xEnd, MIN_VERTEX_DISTANCE))
            throw new IllegalArgumentException("Distance between xStart and xEnd is too short");

        p.set(xStart);
        x.set(xEnd).subtractLocal(xStart).normalizeLocal();
        y.set(n).crossLocal(x).normalizeLocal();
        return this;
    }


    public PlanarCoordinateSystem withY(Vector3f y, Vector3f n) {
        this.p.zero();
        this.y.set(y);
        this.x.set(y).crossLocal(n).normalizeLocal(); // TODO: Does it matter if x/y are not normalized?
        validate();
        return this;
    }

    public PlanarCoordinateSystem withYAt(Vector3f p, Vector3f y, Vector3f n) {
        this.p.set(p);
        this.y.set(y);
        this.x.set(y).crossLocal(n).normalizeLocal(); // TODO: Does it matter if x/y are not normalized?
        validate();
        return this;
    }

    public PlanarCoordinateSystem withYDifference(Vector3f yStart, Vector3f yEnd, Vector3f n) {
        if(yStart.isSimilar(yEnd, MIN_VERTEX_DISTANCE))
            throw new IllegalArgumentException("Distance between yStart and yEnd is too short");

        p.set(yStart);
        y.set(yEnd).subtractLocal(yStart).normalizeLocal();
        x.set(y).crossLocal(n).normalizeLocal();
        return this;
    }


    public PlanarCoordinateSystem forFace(Face face, Vec3Attribute<Vertex> positions) {
        return forPolygon(face.vertices(), positions::get);
    }

    public <T> PlanarCoordinateSystem forPolygon(Iterable<T> elements, Func.MapVec3<T> positionMap) {
        Iterator<T> it = elements.iterator();
        if(!it.hasNext())
            throw new IllegalArgumentException("No elements.");

        Vector3f first = positionMap.get(it.next(), new Vector3f());
        Vector3f last  = first.clone();
        Vector3f valid = new Vector3f();
        Vector3f p     = new Vector3f();

        // Accumulate general direction of polygon
        Vector3f dirSum = new Vector3f();

        // Calculate face normal using Newell's Method
        Vector3f n = new Vector3f();

        int numVertices = 1;
        while(it.hasNext()) {
            positionMap.get(it.next(), p);
            NewellNormal.addToNormal(n, last, p);
            last.set(p);

            p.subtractLocal(first);
            dirSum.addLocal(p);

            // Count only vertices that are different from 'first'
            if(p.lengthSquared() > MIN_VERTEX_DISTANCE_SQUARED) {
                valid.set(p);
                numVertices++;
            }
        }

        if(numVertices < 3)
            throw new IllegalArgumentException("Cannot build PlanarCoordinateSystem with less than 3 valid positions.");

        // Add last segment from last to first
        NewellNormal.addToNormal(n, last, first);
        n.normalizeLocal();

        // Dir sum may be very near at 'first'. In this case use any valid vertex.
        if(dirSum.distanceSquared(first) <= MIN_VERTEX_DISTANCE_SQUARED)
            dirSum.set(valid).addLocal(first);

        withYDifference(first, dirSum, n);
        return this;
    }


    /**
     * Transforms the input 3D-point to local 2D-space by projecting it onto this plane.
     * @param vx
     * @param vy
     * @param vz
     * @param store
     * @return store
     */
    public Vector2f project(float vx, float vy, float vz, Vector2f store) {
        Vector3f diff = new Vector3f(vx, vy, vz);
        diff.subtractLocal(p);

        store.x = diff.dot(x);
        store.y = diff.dot(y);

        return store;
    }

    /**
     * @see #project(float, float, float, Vector2f)
     */
    public Vector2f project(float vx, float vy, float vz) {
        return project(vx, vy, vz, new Vector2f());
    }

    /**
     * @see #project(float, float, float, Vector2f)
     */
    public Vector2f project(Vector3f v, Vector2f store) {
        return project(v.x, v.y, v.z, store);
    }

    /**
     * @see #project(float, float, float, Vector2f)
     */
    public Vector2f project(Vector3f v) {
        return project(v.x, v.y, v.z, new Vector2f());
    }


    /**
     * Transforms the input 2D-point to global 3D-space.
     * @param vx
     * @param vy
     * @param store
     * @return store
     */
    public Vector3f unproject(float vx, float vy, Vector3f store) {
        // store = (v.x * x) + (v.y * y) + p
        store.x = (vx * x.x) + (vy * y.x);
        store.y = (vx * x.y) + (vy * y.y);
        store.z = (vx * x.z) + (vy * y.z);

        store.addLocal(p);
        return store;
    }

    /**
     * @see #unproject(float, float, Vector3f)
     */
    public Vector3f unproject(float vx, float vy) {
        return unproject(vx, vy, new Vector3f());
    }

    /**
     * @see #unproject(float, float, Vector3f)
     */
    public Vector3f unproject(Vector2f v, Vector3f store) {
        return unproject(v.x, v.y, store);
    }

    /**
     * @see #unproject(float, float, Vector3f)
     */
    public Vector3f unproject(Vector2f v) {
        return unproject(v.x, v.y, new Vector3f());
    }


    /**
     * Moves the origin of this coordinate system by the give offset value.
     * @param xOffset
     * @param yOffset
     * @param zOffset
     * @return this
     */
    public PlanarCoordinateSystem move(float xOffset, float yOffset, float zOffset) {
        p.x += xOffset;
        p.y += yOffset;
        p.z += zOffset;
        return this;
    }

    /**
     * @see #move(float, float, float)
     */
    public PlanarCoordinateSystem move(Vector3f offset) {
        return move(offset.x, offset.y, offset.z);
    }


    /**
     * Rotates the axes of this coordinate system around its origin by the given angle.
     * @param angleRad Rotation angle in Radians.
     * @return this
     */
    public PlanarCoordinateSystem rotate(float angleRad) {
        Vector3f normal = x.cross(y).normalizeLocal();
        Quaternion rot = new Quaternion();
        rot.fromAngleNormalAxis(angleRad, normal);
        rot.multLocal(x);
        rot.multLocal(y);
        return this;
    }


    /**
     * Scales the axes of this coordinate system by the given values.
     * @param xScale
     * @param yScale
     * @return this
     */
    public PlanarCoordinateSystem scale(float xScale, float yScale) {
        x.multLocal(xScale);
        y.multLocal(yScale);
        return this;
    }

    /**
     * @see #scale(float, float)
     */
    public PlanarCoordinateSystem scale(float scale) {
        return scale(scale, scale);
    }

    /**
     * @see #scale(float, float)
     */
    public PlanarCoordinateSystem scale(Vector2f scale) {
        return scale(scale.x, scale.y);
    }


    @Override
    public String toString() {
        return "PlanarCoordinateSystem{p: " + p + ", x: " + x + " (" + x.length() + "), y: " + y + " (" + y.length() + ")}";
    }


    @Override
    public PlanarCoordinateSystem clone() {
        PlanarCoordinateSystem copy = new PlanarCoordinateSystem();
        copy.p.set(p);
        copy.x.set(x);
        copy.y.set(y);
        return copy;
    }
}
