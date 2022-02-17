// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

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


    /**
     * Extrudes the input Face by creating <i>n</i> quads on its periphery,
     * where <i>n</i> is the number of sides of the input Face.<br><br>
     * Note that the input Face <i>F</i> will keep its loops (and their attributes), but
     * will receive new vertices and edges with undefined attributes.
     * To apply the attributes of the original vertices (<i>v0-3</i>) to the new vertices (<i>v0'-3')</i>,
     * {@link #copyVertexAttributes()} can be called.<br><br>
     *
     * <pre>
     *     Before:  v3-----v2
     *              |   F   |
     *              v0-----v1
     *
     *     After:   v3----------v2
     *              | \        / |
     *              |  v3'---v2' |
     *              |  |   F  |  |
     *              |  v0'---v1' |
     *              | /        \ |
     *              v0----------v1
     * </pre>
     * @param face The Face to be extruded.
     */
    public void apply(Face face) {
        // Disconnect face
        // Keep loops, but disconnect
        // Leave vertices, create new Vertices (without attributes)
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

                Face sideFace = bmesh.createFace(nextLoop.vertex, loop.vertex, originalVertices.get(i), originalVertices.get(nextIndex));
                resultFaces.add(sideFace);

                loop.edge = loop.vertex.getEdgeTo(nextLoop.vertex);
                loop.edge.addLoop(loop);
            }
        }
        finally {
            tempLoops.clear();
        }
    }


    /**
     * Copy attributes from old vertices to the new ones.
     */
    public void copyVertexAttributes() {
        Loop loop = face.loop;
        for(int i=0; i<originalVertices.size(); ++i) {
            bmesh.vertices().copyAttributes(originalVertices.get(i), loop.vertex);
            loop = loop.nextFaceLoop;
        }
    }


    /**
     * Recreates the Face that was formed by the original input vertices given to the last invocation of {@link #apply(Face)}.<br>
     * This will close the hole that is left behind after an extrude operation.<br>
     * Typically, the returned Face should be inverted with an additional call to {@link BMesh#invertFace(Face)} so it faces outwards.
     * @return A new Face consisting of the original input vertices in the same order.
     */
    public Face recreateOriginalFace() {
        return bmesh.createFace(originalVertices);
    }


    // TODO: Don't need to store them in 'resultFaces', can use data structure to traverse
    public List<Face> getResultFaces() {
        return resultFaces;
    }
}
