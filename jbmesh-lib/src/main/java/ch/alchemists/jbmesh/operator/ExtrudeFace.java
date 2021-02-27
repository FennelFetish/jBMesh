package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import java.util.ArrayList;
import java.util.List;

// extrudeVertex        -> new edge
// extrudeEdgeQuad      -> new face
// extrudeEdgeTriangle  -> new triangle-face from edge with 1 additional vertex
// extrudeFace          -> new volume
public class ExtrudeFace {
    private final BMesh bmesh;

    private final transient List<Loop> tempLoops = new ArrayList<>(4);

    // Results
    private Face face = null;
    private final List<Vertex> originalVertices = new ArrayList<>(4);
    private final List<Face> resultFaces = new ArrayList<>(4);


    public ExtrudeFace(BMesh bmesh) {
        this.bmesh = bmesh;
    }


    public void apply(Face face) {
        // Disconnect face
        // Keep loops, but disconnect
        // Leave vertices, create new Vertices (without properties)
        // (---> no, also new loops, because the original loops have attributes) ???
        //    -> no, keep loops because they belong to the face - attributes are for this face

        // n = num vertices
        // n new Faces -> quads
        // insert new faces

        this.face = face;

        try {
            resultFaces.clear();
            originalVertices.clear();

            // Gather loops and create new vertices for selected Face
            for(Loop loop : face.loops()) {
                tempLoops.add(loop);
                originalVertices.add(loop.vertex);

                loop.vertex = bmesh.createVertex();
                loop.edge.removeLoop(loop);
            }

            for(int i=0; i<tempLoops.size(); ++i) {
                int nextIndex = (i+1) % tempLoops.size();

                Loop loop = tempLoops.get(i);
                Loop nextLoop = tempLoops.get(nextIndex);

                Face newFace = bmesh.createFace(nextLoop.vertex, loop.vertex, originalVertices.get(i), originalVertices.get(nextIndex));
                resultFaces.add(newFace);

                loop.edge = loop.vertex.getEdgeTo(nextLoop.vertex);
                loop.edge.addLoop(loop);
            }
        }
        finally {
            tempLoops.clear();
        }
    }


    /**
     * Copy properties from old vertices to the new ones.
     */
    public void copyVertexProperties() {
        Loop loop = face.loop;
        for(int i=0; i<originalVertices.size(); ++i) {
            bmesh.vertices().copyProperties(originalVertices.get(i), loop.vertex);
            loop = loop.nextFaceLoop;
        }
    }


    public List<Face> getResultFaces() {
        return resultFaces;
    }
}
