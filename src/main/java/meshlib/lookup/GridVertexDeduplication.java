package meshlib.lookup;

import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;
import meshlib.util.HashGrid;

public class GridVertexDeduplication implements VertexDeduplication {

    // TODO: The mean of accumulated vertices will move - which location to use? What if the mean leaves a cell? Don't care?
    /*private static final class VertexAccumulator {
        public final Vertex vertex;
        public final List<Vector3f> locations = new ArrayList<>();

        public VertexAccumulator(Vertex vertex) {
            this.vertex = vertex;
        }
    }*/



    // TODO: store number of neighbors to avoid walks?
    private static final class Cell {
        private final List<Vertex> vertices = new ArrayList<>();
    }


    // 26 directions, 3x3x3 cube without center
    private static final int[][] WALK_DIRECTION = {
        // Two zeros
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},

        // One zero
        {1, 1, 0}, {1, -1, 0}, {-1, 1, 0}, {-1, -1, 0},
        {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
        {0, 1, 1}, {0, 1, -1}, {0, -1, 1}, {0, -1, -1},
        
        // No zero
        {-1, -1, -1}, {-1, -1, 1}, {-1, 1, -1}, {-1, 1, 1}, {1, -1, -1}, {1, -1, 1}, {1, 1, -1}, {1, 1, 1}
    };


    private final Vec3Property<Vertex> propPosition;
    private final HashGrid<Cell> grid;
    private final float epsilonSquared;
    private final Vector3f p = new Vector3f();


    public GridVertexDeduplication(BMesh bmesh) {
        this(bmesh, HashGrid.DEFAULT_CELLSIZE);
    }

    public GridVertexDeduplication(BMesh bmesh, float epsilon) {
        grid = new HashGrid<>(epsilon);
        epsilonSquared = epsilon * epsilon;
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    @Override
    public void addExisting(Vertex vertex) {
        propPosition.get(vertex, p);

        HashGrid.Index gridIndex = grid.getIndexForCoords(p);
        Cell cell = grid.get(gridIndex);
        if(cell == null) {
            cell = new Cell();
            grid.set(gridIndex, cell);
        }

        cell.vertices.add(vertex);
    }

    public void remove(Vertex vertex) {
        propPosition.get(vertex, p);

        HashGrid.Index gridIndex = grid.getIndexForCoords(p);
        Cell cell = grid.get(gridIndex);
        if(cell != null) {
            cell.vertices.remove(vertex);
            if(cell.vertices.isEmpty()) {
                grid.remove(gridIndex);
            }
        }
    }


    @Override
    public Vertex getVertex(Vector3f location) {
        HashGrid.Index gridIndex = grid.getIndexForCoords(location);
        Cell centerCell = grid.get(gridIndex);

        if(centerCell != null) {
            Vertex vertex = searchVertex(centerCell, location);
            if(vertex != null)
                return vertex;
        }

        return searchVertexWalk(gridIndex, location);
    }


    @Override
    public Vertex getOrCreateVertex(BMesh bmesh, Vector3f location) {
        HashGrid.Index gridIndex = grid.getIndexForCoords(location);
        Cell centerCell = grid.get(gridIndex);

        if(centerCell != null) {
            Vertex vertex = searchVertex(centerCell, location);
            if(vertex != null)
                return vertex;
        }

        Vertex vertex = searchVertexWalk(gridIndex, location);
        if(vertex != null)
            return vertex;

        if(centerCell == null) {
            centerCell = new Cell();
            grid.set(gridIndex, centerCell);
        }

        vertex = bmesh.createVertex(location);
        centerCell.vertices.add(vertex);
        return vertex;
    }


    private Vertex searchVertexWalk(HashGrid.Index gridIndex, Vector3f location) {
        for(int[] dir : WALK_DIRECTION) {
            HashGrid.Index walkIndex = gridIndex.walk(dir[0], dir[1], dir[2]);
            Cell cell = grid.get(walkIndex);
            if(cell == null)
                continue;

            Vertex vertex = searchVertex(cell, location);
            if(vertex != null)
                return vertex;
        }

        return null;
    }
    

    private Vertex searchVertex(Cell cell, Vector3f location) {
        for(Vertex vertex : cell.vertices) {
            propPosition.get(vertex, p);
            
            float dist = p.distanceSquared(location);
            if(dist <= epsilonSquared) {
                return vertex;
            }
        }

        return null;
    }
}
