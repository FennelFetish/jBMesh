package ch.alchemists.jbmesh.operator.bool;

import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.lookup.OptimizedGridDeduplication;
import ch.alchemists.jbmesh.lookup.VertexDeduplication;
import ch.alchemists.jbmesh.operator.VertexOps;
import ch.alchemists.jbmesh.operator.meshgen.DistanceFunction;
import ch.alchemists.jbmesh.operator.meshgen.MarchingCube;
import ch.alchemists.jbmesh.operator.meshgen.Tables;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Edge;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.HashGrid;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import java.util.*;

public class Subtract extends Cut {
    private final List<Face> insideFaces = new ArrayList<>();
    private final Set<Vertex> splitResultVertices = new HashSet<>();

    private final Set<Edge> borderEdges = new HashSet<>();
    private final Set<Vertex> borderVertices = new HashSet<>();

    //private final DFuncMeshBuilder meshBuilder;
    private final Vec3Property<Vertex> propPosition;


    public Subtract(BMesh bmesh, DistanceFunction dfunc) {
        super(bmesh, dfunc);
        //meshBuilder = new DFuncMeshBuilder(bmesh, dfunc);
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    @Override protected void accumulateInside(Face face) { insideFaces.add(face); }

    //@Override protected void accumulateOutside(Face face) { insideFaces.add(face); }



    @Override
    protected void accumulateCutVertex(Vertex vertex) {
        splitResultVertices.add(vertex);
    }

    @Override
    protected void accumulateCutEdge(Edge edge) {
        borderEdges.add(edge);
        borderVertices.add(edge.vertex0);
        borderVertices.add(edge.vertex1);
    }


    @Override
    protected void prepareCut() {}


    @Override
    protected void processCut() {
        // Need:
        // Inside faces
        // New edges (sorted loop?), all vertices on edges

        Set<Vertex> insideVertices = new HashSet<>(insideFaces.size() * 4);
        for(Face face : insideFaces) {
            face.loops().forEach(l -> insideVertices.add(l.vertex));
        }

        VertexOps vertOps = new VertexOps(bmesh);
        for(Vertex v : insideVertices) {
            if(!splitResultVertices.contains(v)) {
                bmesh.removeVertex(v);
                //vertOps.move(v, 0.3f, 0.3f, 0.3f);
            }
        }

        /*for(Vertex v : splitResultVertices) {
            //vertOps.move(v, 0.3f, 0.3f, 0.3f);
            //bmesh.removeVertex(v);
        }*/

        //meshBuilder.apply(borderEdges, borderVertices);



        // Build sorted edge loop of cut
        // And sorted edge loop of marched surface (using Loops, find edge of surface)
        // Find nearest inner vertex for each outer vertex
        // connect, build faces, can use structure traversal to find vertices of a face
        //marchingCubes();



        insideFaces.clear();
        splitResultVertices.clear();
        borderEdges.clear();
        borderVertices.clear();
    }



    private void marchingCubes() {
        final float cellSize = 0.2f;
        HashGrid<Vector3f> visitedCells = new HashGrid<>(cellSize);

        Vector3f p = new Vector3f();
        Queue<HashGrid.Index> queue = new ArrayDeque<>();

        {
            Vertex first = borderVertices.iterator().next();
            propPosition.get(first, p);
            p = alignToGrid(p, cellSize);
            HashGrid.Index gridIndex = visitedCells.getIndexForCoords(p);
            putQueue(queue, p, visitedCells, gridIndex, cellSize, 63); // Doesn't work, will make cubes on cut <<<<<<<
        }


        for(Vertex v : borderVertices) {
            propPosition.get(v, p);
            p = alignToGrid(p, cellSize);
            HashGrid.Index gridIndex = visitedCells.getIndexForCoords(p);
            visitedCells.set(gridIndex, p);

            for(byte[] dir : Tables.BOX_WALK_DIRECTIONS) {
                HashGrid.Index walkIndex = gridIndex.walk(dir[0], dir[1], dir[2]);
                Vector3f temp = p.add(dir[0]*cellSize, dir[1]*cellSize, dir[2]*cellSize);
                visitedCells.set(walkIndex, temp);
            }
        }


        VertexDeduplication dedup = new OptimizedGridDeduplication(bmesh, 0.0001f);
        MarchingCube cube = new MarchingCube(bmesh, dedup, cellSize, true);


        // Breadth first traversal along surface
        BoundingBox bounds = dfunc.getBounds();
        float maxBoundDist = cellSize*1.42f;

        while(!queue.isEmpty()) {
            HashGrid.Index gridIndex = queue.poll();
            p = visitedCells.get(gridIndex);
            if(bounds.distanceToEdge(p) > maxBoundDist)
                continue;

            cube.setPosition(p);
            int walkDirections = cube.process(dfunc);
            if(walkDirections != 0)
                putQueue(queue, p, visitedCells, gridIndex, cellSize, walkDirections);
        }
    }


    private static void putQueue(Queue<HashGrid.Index> queue, Vector3f p, HashGrid<Vector3f> visitedCells, HashGrid.Index gridIndex, float cellSize, int walkDirections) {
        for(int i=0; i<6; ++i, walkDirections >>>= 1) {
            if((walkDirections & 1) == 0)
                continue;

            byte[] dir = Tables.WALK_DIRECTIONS[i];
            HashGrid.Index walkIndex = gridIndex.walk(dir[0], dir[1], dir[2]);

            if(visitedCells.get(walkIndex) == null) {
                //System.out.println("queued: " + walkIndex + " (" + p + ")");
                Vector3f v = p.add(dir[0]*cellSize, dir[1]*cellSize, dir[2]*cellSize);
                visitedCells.set(walkIndex, v);
                queue.offer(walkIndex);
            }
        }
    }


    private Vector3f alignToGrid(Vector3f p, float cellSize) {
        p.divideLocal(cellSize);
        p.x = (float) Math.floor(p.x);
        p.y = (float) Math.floor(p.y);
        p.z = (float) Math.floor(p.z);
        p.multLocal(cellSize);
        return p;
    }
}
