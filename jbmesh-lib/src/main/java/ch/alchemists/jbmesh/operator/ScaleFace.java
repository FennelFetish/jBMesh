// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

public class ScaleFace {
    private final Vec3Attribute<Vertex> positions;

    private float scale;
    private Function<Face, Vector3f> pivotFunc;


    public ScaleFace(BMesh bmesh, float scale, Function<Face, Vector3f> pivotFunction) {
        setPivotFunction(pivotFunction);
        positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
        this.scale = scale;
    }

    public ScaleFace(BMesh bmesh, float scale) {
        this(bmesh, scale, new CentroidPivot(bmesh));
    }

    public ScaleFace(BMesh bmesh) {
        this(bmesh, 1.0f, new CentroidPivot(bmesh));
    }


    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getScale() {
        return scale;
    }


    public void setPivotFunction(Function<Face, Vector3f> pivotFunction) {
        Objects.requireNonNull(pivotFunction);
        this.pivotFunc = pivotFunction;
    }


    public void apply(Face face) {
        Vector3f pivot = pivotFunc.apply(face);
        Vector3f p = new Vector3f();

        for(Vertex vertex : face.vertices()) {
            positions.get(vertex, p);
            p.subtractLocal(pivot);
            p.multLocal(scale);
            p.addLocal(pivot);
            positions.set(vertex, p);
        }
    }


    public static class CentroidPivot implements Function<Face, Vector3f> {
        private final FaceOps faceOps;
        private final Vector3f store = new Vector3f();

        public CentroidPivot(BMesh bmesh) {
            faceOps = new FaceOps(bmesh);
        }

        @Override
        public Vector3f apply(Face face) {
            return faceOps.centroid(face, store);
        }
    }


    public static class FirstVertexPivot implements Function<Face, Vector3f> {
        private final Vec3Attribute<Vertex> positions;
        private final Vector3f store = new Vector3f();

        public FirstVertexPivot(BMesh bmesh) {
            positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
        }

        @Override
        public Vector3f apply(Face face) {
            Iterator<Vertex> it = face.vertices().iterator();
            if(it.hasNext())
                return positions.get(it.next(), store);

            return store.zero();
        }
    }


    public static class PointPivot implements Function<Face, Vector3f> {
        private final Vector3f pivotPoint = new Vector3f();

        public PointPivot() {}

        public PointPivot(Vector3f pivotPoint) {
            setPivotPoint(pivotPoint);
        }

        public PointPivot(float xPivot, float yPivot, float zPivot) {
            setPivotPoint(xPivot, yPivot, zPivot);
        }


        public void setPivotPoint(Vector3f pivotPoint) {
            this.pivotPoint.set(pivotPoint);
        }

        public void setPivotPoint(float xPivot, float yPivot, float zPivot) {
            pivotPoint.set(xPivot, yPivot, zPivot);
        }

        public Vector3f getPivotPoint() {
            return pivotPoint;
        }

        @Override
        public Vector3f apply(Face face) {
            return pivotPoint;
        }
    }
}
