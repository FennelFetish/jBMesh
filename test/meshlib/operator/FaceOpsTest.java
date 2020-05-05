package meshlib.operator;

import com.jme3.math.Vector3f;
import meshlib.TestUtil;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Vertex;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class FaceOpsTest {
    @Test
    public void testNormal() {
        BMesh bmesh = new BMesh();
        FaceOps faceOps = new FaceOps(bmesh);

        /**
         * Concave:
         *       v3
         *
         *       v1
         * v0          v2
         */
        Vertex v0 = bmesh.createVertex(0.0f, 0.0f, 0.0f);
        Vertex v1 = bmesh.createVertex(0.5f, 0.3f, 0.0f);
        Vertex v2 = bmesh.createVertex(1.0f, 0.0f, 0.0f);
        Vertex v3 = bmesh.createVertex(0.5f, 1.0f, 0.0f);

        Face face = bmesh.createFace(v0, v1, v2, v3);

        Vector3f normal = faceOps.normal(face);
        assertThat(normal.x, is(0.0f));
        assertThat(normal.y, is(0.0f));
        assertThat(normal.z, is(1.0f));
    }


    @Test
    public void testArea() {
        BMesh bmesh = new BMesh();
        FaceOps faceOps = new FaceOps(bmesh);

        testArea(bmesh, faceOps, 0.5f,
            0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f);

        testArea(bmesh, faceOps, 1.0f,
            0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f);

        testArea(bmesh, faceOps, 0.5f,
            0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            0.5f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f);

        // TODO: more...
    }


    private void testArea(BMesh bmesh, FaceOps faceOps, float expectedArea, float... vertices) {
        Vertex[] vs = new Vertex[vertices.length / 3];
        assert vs.length * 3 == vertices.length;

        int v=0;
        for(int i=2; i<vertices.length; i += 3)
            vs[v++] = bmesh.createVertex(vertices[i-2], vertices[i-1], vertices[i]);

        Face face = bmesh.createFace(vs);
        TestUtil.assertFloat(faceOps.area(face), expectedArea);

        if(vs.length == 3)
            TestUtil.assertFloat(faceOps.areaTriangle(face), expectedArea);
    }
}
