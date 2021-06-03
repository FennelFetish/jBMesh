// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.operator.meshgen;

import ch.alchemists.jbmesh.lookup.HashGridDeduplication;
import ch.alchemists.jbmesh.lookup.VertexDeduplication;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.util.HashGrid;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class MarchingCubeBuilder {
    private static final float DEDUP_EPSILON = 0.0001f;
    private static final float BOUND_EPSILON = 0.001f;
    private static final boolean DEFAULT_SET_NORMALS = true;


    public static BMesh build(BMesh bmesh, DistanceFunction dfunc, float cellSize) {
        return build(bmesh, dfunc, cellSize, DEFAULT_SET_NORMALS);
    }

    public static BMesh build(BMesh bmesh, DistanceFunction dfunc, float cellSize, boolean setNormals) {
        if(bmesh == null)
            bmesh = new BMesh();

        VertexDeduplication dedup = new HashGridDeduplication(bmesh, DEDUP_EPSILON);
        MarchingCube cube = new MarchingCube(bmesh, dedup, cellSize, setNormals);

        BoundingBox bounds = dfunc.getBounds();
        Vector3f start = bounds.getMin(null);
        Vector3f end   = bounds.getMax(null);
        Vector3f p     = new Vector3f();

        // Prevent building outside of bounds
        float boundary = cellSize - BOUND_EPSILON;
        end.subtractLocal(boundary, boundary, boundary);

        for(p.x = start.x; p.x <= end.x; p.x += cellSize) {
            for(p.y = start.y; p.y <= end.y; p.y += cellSize) {
                for(p.z = start.z; p.z <= end.z; p.z += cellSize) {
                    cube.setPosition(p);
                    cube.process(dfunc);
                }
            }
        }

        return bmesh;
    }


    public static BMesh buildFollowSurface(BMesh bmesh, DistanceFunction dfunc, float cellSize) {
        return buildFollowSurface(bmesh, dfunc, cellSize, DEFAULT_SET_NORMALS);
    }

    public static BMesh buildFollowSurface(BMesh bmesh, DistanceFunction dfunc, float cellSize, boolean setNormals) {
        Iterator<Vector3f> cellPosIterator = new DefaultCellPosIterator(dfunc.getBounds(), cellSize);
        return buildFollowSurface(bmesh, dfunc, cellSize, setNormals, cellPosIterator);
    }

    public static BMesh buildFollowSurface(BMesh bmesh, DistanceFunction dfunc, float cellSize, boolean setNormals, Iterator<Vector3f> cellPosIterator) {
        if(bmesh == null)
            bmesh = new BMesh();

        VertexDeduplication dedup = new HashGridDeduplication(bmesh, DEDUP_EPSILON);
        HashGrid<Vector3f> visitedCells = new HashGrid<>(cellSize);
        MarchingCube cube = new MarchingCube(bmesh, dedup, cellSize, setNormals);
        Queue<HashGrid.Index> queue = new ArrayDeque<>();
        Vector3f p;

        // Find first intersecting cell
        while(cellPosIterator.hasNext()) {
            p = cellPosIterator.next();
            cube.setPosition(p);
            int walkDirections = cube.process(dfunc);

            if(walkDirections != 0) {
                HashGrid.Index gridIndex = visitedCells.getIndexForCoords(p);
                visitedCells.set(gridIndex, p.clone());
                putQueue(queue, p, visitedCells, gridIndex, cellSize, walkDirections);
                break;
            }
        }

        // Move BoundingBox so we can use it to ensure that the cells are all completely contained inside the bounds
        BoundingBox bounds = dfunc.getBounds();
        float boundary = cellSize - BOUND_EPSILON;
        Vector3f min = bounds.getMin(null).subtractLocal(boundary, boundary, boundary);
        Vector3f max = bounds.getMax(null).subtractLocal(boundary, boundary, boundary);
        bounds = new BoundingBox(min, max);

        // Breadth first traversal along surface
        while(!queue.isEmpty()) {
            HashGrid.Index gridIndex = queue.poll();
            p = visitedCells.get(gridIndex);
            if(!bounds.contains(p))
                continue;

            cube.setPosition(p);
            int walkDirections = cube.process(dfunc);
            if(walkDirections != 0)
                putQueue(queue, p, visitedCells, gridIndex, cellSize, walkDirections);
        }

        return bmesh;
    }


    private static void putQueue(Queue<HashGrid.Index> queue, Vector3f p, HashGrid<Vector3f> visitedCells, HashGrid.Index gridIndex, float cellSize, int walkDirections) {
        for(int i=0; i<6; ++i, walkDirections >>>= 1) {
            if((walkDirections & 1) == 0)
                continue;

            byte[] dir = Tables.WALK_DIRECTIONS[i];
            //HashGrid.Index walkIndex = gridIndex.walk(dir[0], dir[1], dir[2]);

            //if(visitedCells.get(walkIndex) == null) {
            if(visitedCells.getNeighbor(gridIndex, dir[0], dir[1], dir[2]) == null) {
                Vector3f v = p.add(dir[0]*cellSize, dir[1]*cellSize, dir[2]*cellSize);

                HashGrid.Index walkIndex = gridIndex.walk(dir[0], dir[1], dir[2]);
                visitedCells.set(walkIndex, v);
                queue.offer(walkIndex);
            }
        }
    }


    public static class DefaultCellPosIterator implements Iterator<Vector3f> {
        private final Vector3f start;
        private final Vector3f end;
        private final Vector3f p = new Vector3f();
        private final float cellSize;

        public DefaultCellPosIterator(BoundingBox bounds, float cellSize) {
            start = bounds.getMin(null);
            end   = bounds.getMax(null);

            float boundary = cellSize - BOUND_EPSILON;
            end.subtractLocal(boundary, boundary, boundary);

            p.set(start);
            p.y -= cellSize;
            this.cellSize = cellSize;
        }

        @Override
        public boolean hasNext() {
            return (p.y <= end.y) || (p.z <= end.z) || (p.x <= end.x);
        }

        @Override
        public Vector3f next() {
            p.y += cellSize;
            if(p.y > end.y) {
                p.y = start.y;

                p.z += cellSize;
                if(p.z > end.z) {
                    p.z = start.z;

                    p.x += cellSize;
                }
            }

            return p;
        }
    }
}
