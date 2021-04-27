package ch.alchemists.jbmesh.lookup;

import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.HashGrid;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class OptimizedGridDeduplication implements VertexDeduplication {
    // 3x3x3 cube without center, 26 directions total, 7 directions for 8 subcells
    private static final int[][][] WALK_DIRECTION = { // [8][7][3]
        {{-1, 0, 0}, {0, -1, 0}, {0, 0, -1},    {-1, -1, 0}, {-1, 0, -1}, {0, -1, -1},  {-1, -1, -1}},  // -X, -Y, -Z
        {{-1, 0, 0}, {0, -1, 0}, {0, 0, 1},     {-1, -1, 0}, {-1, 0, 1}, {0, -1, 1},    {-1, -1, 1}},   // -X, -Y, +Z
        {{-1, 0, 0}, {0, 1, 0}, {0, 0, -1},     {-1, 1, 0}, {-1, 0, -1}, {0, 1, -1},    {-1, 1, -1}},   // -X, +Y, -Z
        {{-1, 0, 0}, {0, 1, 0}, {0, 0, 1},      {-1, 1, 0}, {-1, 0, 1}, {0, 1, 1},      {-1, 1, 1}},    // -X, +Y, +Z
        {{1, 0, 0}, {0, -1, 0}, {0, 0, -1},     {1, -1, 0}, {1, 0, -1}, {0, -1, -1},    {1, -1, -1}},   // +X, -Y, -Z
        {{1, 0, 0}, {0, -1, 0}, {0, 0, 1},      {1, -1, 0}, {1, 0, 1}, {0, -1, 1},      {1, -1, 1}},    // +X, -Y, +Z
        {{1, 0, 0}, {0, 1, 0}, {0, 0, -1},      {1, 1, 0}, {1, 0, -1}, {0, 1, -1},      {1, 1, -1}},    // +X, +Y, -Z
        {{1, 0, 0}, {0, 1, 0}, {0, 0, 1},       {1, 1, 0}, {1, 0, 1}, {0, 1, 1},        {1, 1, 1}},     // +X, +Y, +Z
    };


    private final float epsilon;
    private final float epsilonSquared;
    private final float cellSize;

    private final BMesh bmesh;
    private final Vec3Property<Vertex> propPosition;
    private final HashGrid<List<Vertex>> grid;
    private final Vector3f p = new Vector3f();


    public OptimizedGridDeduplication(BMesh bmesh) {
        this(bmesh, HashGrid.DEFAULT_CELLSIZE);
    }

    public OptimizedGridDeduplication(BMesh bmesh, float epsilon) {
        this.bmesh = bmesh;
        this.epsilon = epsilon;
        epsilonSquared = epsilon * epsilon;
        cellSize = epsilon * 2.0f;

        grid = new HashGrid<>(cellSize);
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    @Override
    public void addExisting(Vertex vertex) {
        propPosition.get(vertex, p);

        HashGrid.Index gridIndex = grid.getIndexForCoords(p);
        List<Vertex> vertices = grid.get(gridIndex);
        if(vertices == null) {
            vertices = new ArrayList<>(1);
            grid.set(gridIndex, vertices);
        }

        vertices.add(vertex);
    }

    public void remove(Vertex vertex) {
        propPosition.get(vertex, p);

        HashGrid.Index gridIndex = grid.getIndexForCoords(p);
        List<Vertex> vertices = grid.get(gridIndex);
        if(vertices != null) {
            vertices.remove(vertex);
            if(vertices.isEmpty()) {
                grid.remove(gridIndex);
            }
        }
    }


    @Override
    public void clear() {
        grid.clear();
    }


    @Override
    public Vertex getVertex(Vector3f location) {
        HashGrid.Index gridIndex = grid.getIndexForCoords(location);
        List<Vertex> vertices = grid.get(gridIndex);

        if(vertices != null) {
            Vertex vertex = searchVertex(vertices, location);
            if(vertex != null)
                return vertex;
        }

        return searchVertexWalk(gridIndex, location);
    }


    @Override
    public Vertex getOrCreateVertex(Vector3f location) {
        HashGrid.Index gridIndex = grid.getIndexForCoords(location);
        List<Vertex> centerVertices = grid.get(gridIndex);

        if(centerVertices != null) {
            Vertex vertex = searchVertex(centerVertices, location);
            if(vertex != null)
                return vertex;
        }

        Vertex vertex = searchVertexWalk(gridIndex, location);
        if(vertex != null)
            return vertex;

        if(centerVertices == null) {
            centerVertices = new ArrayList<>(1);
            grid.set(gridIndex, centerVertices);
        }

        vertex = bmesh.createVertex(location);
        centerVertices.add(vertex);
        return vertex;
    }


    private int[][] getWalkDirections(HashGrid.Index gridIndex, Vector3f location) {
        float pivotX = (gridIndex.x * cellSize) - epsilon;
        float pivotY = (gridIndex.y * cellSize) - epsilon;
        float pivotZ = (gridIndex.z * cellSize) - epsilon;

        int index = 0;
        if(location.z > pivotZ) index |= 1;
        if(location.y > pivotY) index |= 2;
        if(location.x > pivotX) index |= 4;

        return WALK_DIRECTION[index];
    }


    private Vertex searchVertexWalk(HashGrid.Index gridIndex, Vector3f location) {
        int[][] directions = getWalkDirections(gridIndex, location);

        for(int[] dir : directions) {
            List<Vertex> vertices = grid.getNeighbor(gridIndex, dir[0], dir[1], dir[2]);
            if(vertices == null)
                continue;

            Vertex vertex = searchVertex(vertices, location);
            if(vertex != null)
                return vertex;
        }

        return null;
    }


    private Vertex searchVertex(List<Vertex> vertices, Vector3f location) {
        for(Vertex vertex : vertices) {
            propPosition.get(vertex, p);
            if(p.distanceSquared(location) <= epsilonSquared)
                return vertex;
        }

        return null;
    }
}
