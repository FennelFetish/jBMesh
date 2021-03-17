package ch.alchemists.jbmesh.util;

import ch.alchemists.jbmesh.conversion.LineExport;
import ch.alchemists.jbmesh.conversion.TriangleExport;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.FastMath;
import com.jme3.scene.Mesh;

public abstract class BasicShapes {
    private static BMesh createDisc(int numSamples, float radius) {
        BMesh bmesh = new BMesh();
        float angleBetweenSamples = FastMath.TWO_PI / numSamples;

        Vertex[] vertices = new Vertex[numSamples];
        for(int i=0; i<numSamples; ++i) {
            float angle = i * angleBetweenSamples;
            float x = FastMath.cos(angle) * radius;
            float y = FastMath.sin(angle) * radius;
            vertices[i] = bmesh.createVertex(x, y, 0);
        }

        bmesh.createFace(vertices);
        return bmesh;
    }


    public static Mesh disc(int numSamples, float radius) {
        BMesh bmesh = createDisc(numSamples, radius);
        return TriangleExport.apply(bmesh);
    }


    public static Mesh circle(int numSamples, float radius) {
        BMesh bmesh = createDisc(numSamples, radius);
        return LineExport.apply(bmesh);
    }
}
