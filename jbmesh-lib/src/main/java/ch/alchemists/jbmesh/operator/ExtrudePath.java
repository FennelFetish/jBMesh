// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.operator;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec2Attribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.PlanarCoordinateSystem;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ExtrudePath {
    private final BMesh bmesh;
    private final Vec3Attribute<Vertex> positions;
    private final ExtrudeFace extrude;

    private final ArrayList<Vector2f> shape = new ArrayList<>();
    private Face face;
    private int numSegments;


    public ExtrudePath(BMesh bmesh) {
        this.bmesh = bmesh;
        extrude = new ExtrudeFace(bmesh);
        positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
    }


    /**
     * Applies the extrusion of the <code>face</code> along the given <code>path</code>.<br>
     * @param face The Face to be extruded. Defines the shape.
     * @param faceCoordSys Defines how the orientation of the given <code>face</code> is recognized.<br><ul>
     *                     <li>The coordinate system should be coplanar with the face.</li>
     *                     <li>The X-axis defines the starting tangent, and therefore how the tangents of
     *                     the remaining path influence the rotation of extruded segments.</li></ul>
     * @param path The path along which the shape should be extruded.
     *             Each <code>PlanarCoordinateSystem</code> defines position and orientation of a segment.<br>
     *             The X-axis defines the tangent.
     */
    public void apply(Face face, PlanarCoordinateSystem faceCoordSys, Iterable<PlanarCoordinateSystem> path) {
        this.face = face;
        numSegments = 0;
        Vector3f p = new Vector3f();

        // Create projected shape
        shape.clear();
        for(Vertex vertex : face.vertices()) {
            positions.get(vertex, p);
            shape.add(faceCoordSys.project(p));
        }

        // Extrude along path
        for(PlanarCoordinateSystem coordSys : path) {
            numSegments++;

            extrude.apply(face);
            extrude.copyVertexAttributes();

            int i=0;
            for(Loop loop : face.loops()) {
                coordSys.unproject(shape.get(i), p);
                positions.set(loop.vertex, p);
                i++;
            }
        }
    }


    /**
     * Applies texture coordinates to all Loops of the extruded segments that
     * were previously created with {@link #apply(Face, PlanarCoordinateSystem, Iterable)}.<br><br>
     * <ul>
     * <li>The x texture coordinates range from 0.0 to 1.0 around the defined shape.</li>
     * <li>The y texture coordinates range from 0.0 to 1.0 along the extrusion path.</li>
     * </ul><br>
     * Since this method traverses the BMesh structure to find the Loops,
     * it should be called before any other structural changes are made,
     * e.g. right after calling the mentioned <code>apply()</code> method.
     */
    public void applyLoopTexCoords() {
        if(face == null)
            return;

        Vec2Attribute<Loop> attrTexCoords = Vec2Attribute.getOrCreate(BMeshAttribute.TexCoord, bmesh.loops());
        float[] texCoordsX = calcTexCoordsX();

        // Y coordinates decrease with segments
        float yFront = 1.0f;
        float yBack  = 1.0f;
        float yFeed  = 1f / numSegments;

        // We start at the loops inside the forward-facing Face at the end of extrusion
        // and move backwards from there, segment by segment (made of quads).
        ArrayList<Loop> loops = face.getLoops();

        for(int i=0; i<numSegments; ++i) {
            yBack -= yFeed;

            for(int side=0; side<loops.size(); ++side) {
                // Traverse edge to next face
                Loop loop = loops.get(side).nextEdgeLoop;

                applyLoopTexCoords(loop, side, texCoordsX, yFront, yBack, attrTexCoords);

                // Skip to other side of the quad
                loop = loop.nextFaceLoop.nextFaceLoop;
                loops.set(side, loop);
            }

            yFront = yBack;
        }
    }


    private float[] calcTexCoordsX() {
        // We need one extra coordinate to make a closed loop. Last tex coord = 1.0
        float[] texCoords = new float[shape.size() + 1];
        float perimeter = 0;

        // Calculate edge length and sum up perimeter length
        Vector2f lastPoint = shape.get(0);
        for(int i=1; i<shape.size(); ++i) {
            Vector2f p = shape.get(i);
            perimeter += p.distance(lastPoint);
            texCoords[i] = perimeter;

            lastPoint = p;
        }

        perimeter += shape.get(0).distance(lastPoint);

        // Divide values by perimeter length
        float perimeterInv = 1f / perimeter;
        for(int i=1; i<shape.size(); ++i)
            texCoords[i] *= perimeterInv;

        texCoords[shape.size()] = 1.0f;
        //texCoords[0] = 0;
        return texCoords;
    }


    private void applyLoopTexCoords(Loop loop, int side, float[] texCoordsX, float yFront, float yBack, Vec2Attribute<Loop> attrTexCoords) {
        Loop loop2 = loop.nextFaceLoop;
        Loop loop3 = loop2.nextFaceLoop;
        Loop loop4 = loop3.nextFaceLoop;
        assert loop4.nextFaceLoop == loop;

        attrTexCoords.set(loop,  texCoordsX[side+1], yFront);
        attrTexCoords.set(loop2, texCoordsX[side],   yFront);
        attrTexCoords.set(loop3, texCoordsX[side],   yBack);
        attrTexCoords.set(loop4, texCoordsX[side+1], yBack);
    }



    public static abstract class PointListPath implements Iterable<PlanarCoordinateSystem> {
        protected final ArrayList<Vector3f> points = new ArrayList<>();
        protected final Vector3f startPoint = new Vector3f();


        public PointListPath(Vector3f startPoint) {
            this.startPoint.set(startPoint);
        }

        public PointListPath(Vector3f startPoint, Collection<Vector3f> points) {
            this.startPoint.set(startPoint);
            this.points.addAll(points);
        }


        public void addPoint(Vector3f p) {
            points.add(p);
        }

        public void clearPoints() {
            points.clear();
        }


        protected abstract void setTangent(int i, Vector3f tangent, Vector3f normal);


        @Override
        public Iterator<PlanarCoordinateSystem> iterator() {
            if(points.size() < 2)
                throw new IllegalStateException("Need at least 2 points");

            return new Iterator<>() {
                private final PlanarCoordinateSystem coordSys = new PlanarCoordinateSystem();
                private final Vector3f normal  = new Vector3f();
                private final Vector3f tangent = new Vector3f();
                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < points.size();
                }

                @Override
                public PlanarCoordinateSystem next() {
                    Vector3f current = points.get(i);

                    if(i < points.size()-1) {
                        Vector3f prev = (i == 0) ? startPoint : points.get(i-1);
                        Vector3f next = points.get(i+1);

                        Vector3f nPrev = current.subtract(prev).normalizeLocal();
                        Vector3f nNext = next.subtract(current).normalizeLocal();
                        normal.set(nPrev).addLocal(nNext).normalizeLocal();
                    }
                    // Last point
                    else {
                        Vector3f prev = points.get(i-1);
                        normal.set(current).subtractLocal(prev).normalizeLocal();
                    }

                    setTangent(i, tangent, normal);
                    coordSys.withXAt(current, tangent, normal);

                    i++;
                    return coordSys;
                }
            };
        }
    }
}
