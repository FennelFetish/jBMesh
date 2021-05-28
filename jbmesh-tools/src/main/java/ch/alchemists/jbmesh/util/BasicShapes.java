package ch.alchemists.jbmesh.util;

import ch.alchemists.jbmesh.conversion.LineExport;
import ch.alchemists.jbmesh.conversion.TriangleExport;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;

public abstract class BasicShapes {
    public static Face createDisc(BMesh bmesh, PlanarCoordinateSystem coordSys, int numSamples, float radius) {
        float angleBetweenSamples = FastMath.TWO_PI / numSamples;
        Vector2f p = new Vector2f();
        Vector3f temp = new Vector3f();

        Vertex[] vertices = new Vertex[numSamples];
        for(int i=0; i<numSamples; ++i) {
            float angle = i * angleBetweenSamples;
            p.x = FastMath.cos(angle) * radius;
            p.y = FastMath.sin(angle) * radius;
            coordSys.unproject(p, temp);
            vertices[i] = bmesh.createVertex(temp);
        }

        return bmesh.createFace(vertices);
    }


    public static Face createDiscXY(BMesh bmesh, Vector3f center, int numSamples, float radius) {
        float angleBetweenSamples = FastMath.TWO_PI / numSamples;

        Vertex[] vertices = new Vertex[numSamples];
        for(int i=0; i<numSamples; ++i) {
            float angle = i * angleBetweenSamples;
            float x = FastMath.cos(angle) * radius;
            float y = FastMath.sin(angle) * radius;
            vertices[i] = bmesh.createVertex(center.x + x, center.y + y, center.z);
        }

        return bmesh.createFace(vertices);
    }


    public static Mesh disc(int numSamples, float radius) {
        BMesh bmesh = new BMesh();
        createDiscXY(bmesh, Vector3f.ZERO, numSamples, radius);
        return TriangleExport.apply(bmesh);
    }


    public static Mesh circle(int numSamples, float radius) {
        BMesh bmesh = new BMesh();
        createDiscXY(bmesh, Vector3f.ZERO, numSamples, radius);
        return LineExport.apply(bmesh);
    }
}
