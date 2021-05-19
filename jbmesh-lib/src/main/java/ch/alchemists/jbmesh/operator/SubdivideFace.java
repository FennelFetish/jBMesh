package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.structure.*;
import com.jme3.math.Vector3f;
import java.util.*;

public class SubdivideFace {
    private class FaceInfo {
        public final Vertex startVertex;
        public final int sides;

        public FaceInfo(List<Loop> loops) {
            startVertex = loops.get(0).vertex;
            sides = loops.size();
        }
    }


    private final BMesh bmesh;
    private final Map<Face, FaceInfo> faceInfo = new HashMap<>(); // TODO: This one too (as local variable in apply())
    private final Set<Edge> edges = new HashSet<>(); // TODO: Try and benchmark as local variable in apply() with initialCapacity

    private final Vec3Property<Vertex> propPosition;
    private int cuts = 1;

    private final List<Loop> tempLoops = new ArrayList<>(4);
    private final Vector3f tempP = new Vector3f();
    private final Vector3f tempStep = new Vector3f();


    public SubdivideFace(BMesh bmesh) {
        this(bmesh, 1);
    }

    public SubdivideFace(BMesh bmesh, int cuts) {
        this.bmesh = bmesh;
        setCuts(cuts);
        propPosition = Vec3Property.get(Vertex.Position, bmesh.vertices());
    }


    public void setCuts(int cuts) {
        if(cuts < 1)
            throw new IllegalArgumentException("Number of cuts must be at least 1");
        this.cuts = cuts;
    }


    public void apply(List<Face> faces) {
        try {
            for(Face f : faces)
                prepare(f);

             // If multiple faces are selected for subdivision, we can't treat each face separately:
             // Edges are split first for all faces.
            for(Edge edge : edges)
                splitEdge(edge, cuts);

            for(Map.Entry<Face, FaceInfo> entry : faceInfo.entrySet())
                subdivide(entry.getKey(), entry.getValue());
        }
        finally {
            faceInfo.clear();
            edges.clear();
        }
    }


    private void prepare(Face face) {
        try {
            face.getLoops(tempLoops);
            if(tempLoops.size() != 3 && tempLoops.size() != 4)
                return;

            faceInfo.put(face, new FaceInfo(tempLoops));
            for(Loop loop : tempLoops)
                edges.add(loop.edge);
        }
        finally {
            tempLoops.clear();
        }
    }


    private void splitEdge(Edge edge, final int cuts) {
        Vertex edgeV1 = edge.vertex1;
        propPosition.get(edge.vertex0, tempP);
        propPosition.get(edge.vertex1, tempStep);
        tempStep.subtractLocal(tempP);
        tempStep.divideLocal(cuts+1);

        for(int i=0; i<cuts; ++i) {
            Vertex v = bmesh.splitEdge(edge);
            tempP.addLocal(tempStep);
            propPosition.set(v, tempP);

            edge = v.getEdgeTo(edgeV1);
            assert edge != null;
        }
    }


    private void subdivide(Face face, FaceInfo info) {
        try {
            int first = 0;
            face.getLoops(tempLoops);
            for(Loop loop : tempLoops) {
                if(loop.vertex == info.startVertex)
                    break;
                first++;
            }

            if(info.sides == 3) {
                subdivideTriangle(face, first);
            } else {
                assert info.sides == 4;
                subdivideQuad(face, first);
            }
        }
        finally {
            tempLoops.clear();
        }
    }


    /**
     * Depends on BMesh.splitFace() implementation: Existing face is expected to lie on the right side.
     * @param face
     * @param first
     */
    private void subdivideTriangle(Face face, final int first) {
        final int sideStep = 1 + cuts;
        int cutStart = first + 1;
        int cutEnd   = first + 2*sideStep - 1;

        // Split face in one direction
        for(int i=0; i<cuts; ++i) {
            Vertex v0 = tempLoops.get(cutStart % tempLoops.size()).vertex;
            Vertex v1 = tempLoops.get(cutEnd % tempLoops.size()).vertex;
            Edge edge = bmesh.splitFace(face, v0, v1);

            int edgeCuts = cuts-i-1;
            if(edgeCuts > 0)
                splitEdge(edge, edgeCuts);

            cutStart++;
            cutEnd--;
        }

        // Make inner triangles
        cutStart = first;
        for(int i=0; i<=cuts; ++i) {
            Loop current   = tempLoops.get(cutStart % tempLoops.size());
            Loop loopStart = current.nextFaceLoop;
            Loop loopEnd   = current.prevFaceLoop;
            cutStart++;

            for(int k=i; k<cuts; ++k) {
                Vertex v0 = loopStart.vertex;
                Vertex v1 = loopEnd.vertex;

                loopStart = loopStart.nextFaceLoop;
                loopEnd   = loopEnd.prevFaceLoop;
                bmesh.splitFace(loopStart.face, v0, v1);

                v0 = loopStart.vertex;
                bmesh.splitFace(loopStart.face, v0, v1);
            }
        }
    }


    /**
     * Depends on BMesh.splitFace() implementation: Existing face is expected to lie on the right side.
     * @param face
     * @param first
     */
    private void subdivideQuad(Face face, final int first) {
        final int sideStep = 1 + cuts;
        int cutStart = first + 1;
        int cutEnd   = first + 3*sideStep - 1;

        // Split face in one direction
        for(int i=0; i<cuts; ++i) {
            Vertex v0 = tempLoops.get(cutStart % tempLoops.size()).vertex;
            Vertex v1 = tempLoops.get(cutEnd % tempLoops.size()).vertex;
            Edge edge = bmesh.splitFace(face, v0, v1);
            splitEdge(edge, cuts);

            cutStart++;
            cutEnd--;
        }

        // Split faces in the other direction
        cutStart = first;
        for(int i=0; i<sideStep; ++i) {
            Loop current   = tempLoops.get(cutStart % tempLoops.size());
            Loop loopStart = current.nextFaceLoop.nextFaceLoop;
            Loop loopEnd   = current.prevFaceLoop;
            cutStart++;

            for(int k=0; k<cuts; ++k) {
                Vertex v0 = loopStart.vertex;
                Vertex v1 = loopEnd.vertex;

                loopStart = loopStart.nextFaceLoop;
                loopEnd = loopEnd.prevFaceLoop;
                bmesh.splitFace(loopStart.face, v0, v1);
            }
        }
    }
}
