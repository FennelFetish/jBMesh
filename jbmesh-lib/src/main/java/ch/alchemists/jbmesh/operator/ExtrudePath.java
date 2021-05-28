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
import java.util.List;

public class ExtrudePath {
    // TODO: Change scale? Give more access to intermediate faces? Create List<Vertex[]> and process afterwards?
    public interface PathIterator {
        boolean getNextPoint(Vector3f position, Vector3f tangent, Vector3f normal);
        int getNumPoints();
    }


    private final BMesh bmesh;
    private final Vec3Attribute<Vertex> positions;
    private final ExtrudeFace extrude;


    public ExtrudePath(BMesh bmesh) {
        this.bmesh = bmesh;
        extrude = new ExtrudeFace(bmesh);
        positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
    }


    public void apply(Face face, PathIterator pathIterator) {
        Vector3f pos = new Vector3f();
        Vector3f tan = new Vector3f();
        Vector3f nor = new Vector3f();

        // Initial point, no extrusion. This defines starting orientation.
        if(!pathIterator.getNextPoint(pos, tan, nor))
            return;

        PlanarCoordinateSystem coordSys = new PlanarCoordinateSystem().withXAt(pos, tan, nor);
        ArrayList<Vertex> vertices = face.getVertices();
        Vector2f[] points = createPoints(vertices, coordSys);

        Vec2Attribute<Vertex> attrTexCoords = Vec2Attribute.getOrCreate(BMeshAttribute.TexCoord, bmesh.vertices());
        float[] texCoords = createTextureCoordinates(points);

        // TODO: Set texCoords to loops instead of vertices

        // Set texCoords to loops of original face, TODO: Does it work?
        for(int i=0; i<vertices.size(); ++i) {
            attrTexCoords.set(vertices.get(i), texCoords[i], 0);
        }

        int faceNr = 1;
        while(pathIterator.getNextPoint(pos, tan, nor)) {
            coordSys.withXAt(pos, tan, nor);

            extrude.apply(face);
            extrude.copyVertexAttributes();

            int i=0;
            for(Loop loop : face.loops()) {
                positions.set(loop.vertex, coordSys.unproject(points[i]));

                float texCoordsY = (float) faceNr / (pathIterator.getNumPoints()-1);
                //attrTexCoords.set(loop, texCoords[i], texCoordsY);
                attrTexCoords.set(loop.vertex, texCoords[i], texCoordsY);

                i++;
            }

            ++faceNr;
        }
    }


    private Vector2f[] createPoints(List<Vertex> vertices, PlanarCoordinateSystem coordSys) {
        Vector2f[] points = new Vector2f[vertices.size()];
        Vector3f p = new Vector3f();

        for(int i=0; i<points.length; ++i) {
            positions.get(vertices.get(i), p);
            points[i] = coordSys.project(p);
        }

        return points;
    }


    private float[] createTextureCoordinates(Vector2f[] points) {
        float[] texCoords = new float[points.length];
        float perimeter = 0;

        Vector2f lastPoint = points[points.length-1];
        for(int i=0; i<points.length; ++i) {
            texCoords[i] = points[i].distance(lastPoint);
            perimeter += texCoords[i];

            lastPoint = points[i];
        }

        texCoords[0] /= perimeter;
        for(int i=1; i<texCoords.length; ++i)
            texCoords[i] = texCoords[i-1] + (texCoords[i]/perimeter);

        return texCoords;
    }



    public static abstract class PointPathIterator implements PathIterator {
        protected final List<Vector3f> points = new ArrayList<>();
        private int i = 0;

        public PointPathIterator() {}

        public PointPathIterator(Collection<Vector3f> points) {
            this.points.addAll(points);
        }

        public void addPoint(Vector3f p) {
            points.add(p);
        }

        public void clearPoints() {
            points.clear();
        }

        @Override
        public int getNumPoints() {
            return points.size();
        }

        protected abstract void setTangent(int i, Vector3f tangent, Vector3f normal);

        @Override
        public boolean getNextPoint(Vector3f position, Vector3f tangent, Vector3f normal) {
            // Finish
            if(i == points.size())
                return false;

            Vector3f current = points.get(i);
            position.set(current);

            // First point
            if(i == 0) {
                Vector3f next = points.get(i+1);
                normal.set(next).subtractLocal(current).normalizeLocal();
            }
            // Last point
            else if(i == points.size()-1) {
                Vector3f prev = points.get(i-1);
                normal.set(current).subtractLocal(prev).normalizeLocal();
            }
            // All middle points
            else {
                Vector3f next = points.get(i+1);
                Vector3f prev = points.get(i-1);

                Vector3f nPrev = current.subtract(prev).normalizeLocal();
                Vector3f nNext = next.subtract(current).normalizeLocal();
                normal.set(nPrev).addLocal(nNext).normalizeLocal();
            }

            setTangent(i, tangent, normal);
            i++;
            return true;
        }
    }
}
