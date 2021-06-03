// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.DebugVisual;
import ch.alchemists.jbmesh.util.PlanarCoordinateSystem;
import com.jme3.math.Vector3f;
import java.util.TreeSet;

public class SweepTriangulation {
    public interface TriangleCallback {
        void handleTriangle(SweepVertex v1, SweepVertex v2, SweepVertex v3);
    }


    private final TreeSet<SweepVertex> sweepVertices = new TreeSet<>();
    private final EdgeSet edges = new EdgeSet();

    private final Preparation preparation = new Preparation(sweepVertices);
    private TriangleCallback cb;


    public SweepTriangulation() {}

    public SweepTriangulation(TriangleCallback triangleCallback) {
        this.cb = triangleCallback;
    }


    public void setTriangleCallback(TriangleCallback triangleCallback) {
        this.cb = triangleCallback;
    }

    public void setCoordinateSystem(PlanarCoordinateSystem coordSys) {
        preparation.setCoordinateSystem(coordSys);
    }


    public void addFace(BMesh bmesh, Face face) {
        addFaceWithLoops(bmesh, face.loops());
    }

    public void addFace(Vec3Attribute<Vertex> positions, Face face) {
        addFaceWithLoops(positions, face.loops());
    }

    public void addFaceWithLoops(BMesh bmesh, Iterable<Loop> face) {
        Vec3Attribute<Vertex> positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
        addFaceWithLoops(positions, face);
    }

    public void addFaceWithLoops(Vec3Attribute<Vertex> positions, Iterable<Loop> face) {
        preparation.addFace(face, (Loop loop, Vector3f store) -> positions.get(loop.vertex, store), loop -> loop.vertex);
    }

    public void addFaceWithPositions(Iterable<Vector3f> face) {
        preparation.addFace(face, (Vector3f v, Vector3f store) -> store.set(v), v -> null);
    }


    public void triangulate() {
        if(sweepVertices.size() < 3)
            throw new IllegalStateException("Triangulation needs a face with at least 3 valid vertices");

        if(cb == null)
            throw new IllegalStateException("Missing TriangleCallback");

        try {
            for(SweepVertex v : sweepVertices)
                handleSweepVertex(v);
        }
        finally {
            sweepVertices.clear();
            edges.clear();
            preparation.reset();
        }
    }


    public void triangulateDebug(float yLimit) {
        //System.out.println("SweepTriangulation.triangulateDebug ----------------------------------------------------");

        if(sweepVertices.size() < 3)
            throw new IllegalStateException("Triangulation needs a face with at least 3 valid vertices");

        if(cb == null)
            throw new IllegalStateException("Missing TriangleCallback");

        final float limit = yLimit + sweepVertices.first().p.y;

        try {
            for(SweepVertex v : sweepVertices) {
                if(v.p.y > limit)
                    break;

                //System.out.println("===[ handleSweepVertex " + (v.index+1) + ": " + v.p + " ]===");
                handleSweepVertex(v);
                //edges.printEdges(yLimit);
            }

            edges.drawSweepSegments(limit);
        }
        finally {
            sweepVertices.clear();
            edges.clear();
            preparation.reset();
        }
    }


    private void handleSweepVertex(SweepVertex v) {
        boolean prevUp = v.prev.isAbove(v);
        boolean nextUp = v.next.isAbove(v);

        if(prevUp != nextUp)
            handleContinuation(v);
        else if(v.leftTurn) {
            if(prevUp)
                handleStart(v);
            else
                handleEnd(v);
        } else {
            if(prevUp)
                handleSplit(v);
            else
                handleMerge(v);
        }
    }


    private void handleStart(SweepVertex v) {
        SweepEdge leftEdge = new SweepEdge(v, v.prev);
        leftEdge.monotoneSweep = new MonotoneSweep(v, cb);
        edges.addEdge(leftEdge);

        //leftEdge.rightEdge = new SweepEdge(v, v.next);
    }


    private void handleSplit(SweepVertex v) {
        SweepEdge leftEdge  = edges.getEdge(v);
        assert leftEdge != null : "Intersections?"; // If this happens, some edges were crossing?

        SweepEdge rightEdge = new SweepEdge(v, v.prev);

        SweepVertex lastVertex = leftEdge.monotoneSweep.getLastVertex();

        // Connection to left chain
        if(lastVertex == leftEdge.start) {
            //drawMonotonePath(lastVertex, v);
            rightEdge.monotoneSweep = leftEdge.monotoneSweep;
            leftEdge.monotoneSweep = new MonotoneSweep(lastVertex, cb);
        }
        // Connection to mergeVertex
        else if(leftEdge.lastMerge != null) {
            //drawMonotonePath(leftEdge.lastMerge.getLastVertex(), v);
            rightEdge.monotoneSweep = leftEdge.lastMerge;
            leftEdge.lastMerge = null;
        }
        // Connection to right chain
        else {
            //drawMonotonePath(lastVertex, v);
            rightEdge.monotoneSweep = new MonotoneSweep(lastVertex, cb);
        }

        //rightEdge.rightEdge = leftEdge.rightEdge;
        //leftEdge.rightEdge = new SweepEdge(v, v.next);

        leftEdge.monotoneSweep.processRight(v);
        rightEdge.monotoneSweep.processLeft(v);
        edges.addEdge(rightEdge);
    }


    private void handleMerge(SweepVertex v) {
        // Remove and handle edge to the right
        SweepEdge rightEdge = edges.removeEdge(v);
        if(rightEdge.lastMerge != null) {
            //drawMonotonePath(rightEdge.lastMerge.getLastVertex(), v);
            rightEdge.monotoneSweep.processEnd(v);
            rightEdge.monotoneSweep = rightEdge.lastMerge;
        }

        rightEdge.monotoneSweep.processLeft(v);

        SweepEdge leftEdge = edges.getEdge(v);
        if(leftEdge.lastMerge != null) {
            //drawMonotonePath(leftEdge.lastMerge.getLastVertex(), v);
            leftEdge.lastMerge.processEnd(v);
        }

        //leftEdge.rightEdge = rightEdge.rightEdge;

        leftEdge.monotoneSweep.processRight(v);
        leftEdge.lastMerge = rightEdge.monotoneSweep; // Left edge will remember this merge
    }


    private void handleEnd(SweepVertex v) {
        SweepEdge removedEdge = edges.removeEdge(v);
        removedEdge.monotoneSweep.processEnd(v);

        if(removedEdge.lastMerge != null) {
            //drawMonotonePath(removedEdge.lastMerge.getLastVertex(), v);
            removedEdge.lastMerge.processEnd(v);
        }
    }


    private void handleContinuation(SweepVertex v) {
        SweepEdge edge = edges.getEdge(v);
        assert edge != null : "Intersections?"; // If this happens, some edges were crossing?

        // Left edge continues
        if(edge.end == v) {
            SweepVertex next = getContinuationVertex(edge);
            edge.reset(v, next);

            if(edge.lastMerge != null) {
                //drawMonotonePath(edge.lastMerge.getLastVertex(), v);
                edge.monotoneSweep.processEnd(v);
                edge.monotoneSweep = edge.lastMerge;
                edge.lastMerge = null;
            }

            edge.monotoneSweep.processLeft(v);
        }
        // Right edge continues
        else {
            //assert edge.rightEdge.end == v;
            //SweepVertex next = getContinuationVertex(edge.rightEdge);
            //edge.rightEdge.reset(v, next);

            if(edge.lastMerge != null) {
                //drawMonotonePath(edge.lastMerge.getLastVertex(), v);
                edge.lastMerge.processEnd(v);
                edge.lastMerge = null;
            }

            edge.monotoneSweep.processRight(v);
        }
    }

    private SweepVertex getContinuationVertex(SweepEdge edge) {
        if(edge.end.prev == edge.start)
            return edge.end.next;
        else {
            assert edge.end.next == edge.start;
            return edge.end.prev;
        }
    }


    private void drawMonotonePath(SweepVertex src, SweepVertex dest) {
        //System.out.println("Connecting monotone path from " + src + " to " + dest);
        Vector3f start = new Vector3f(src.p.x, src.p.y, 0);
        Vector3f end = new Vector3f(dest.p.x, dest.p.y, 0);
        DebugVisual.get("SweepTriangulation").addArrow(start, end);
    }
}
