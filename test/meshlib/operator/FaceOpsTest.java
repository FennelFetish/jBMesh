package meshlib.operator;

import com.jme3.math.Vector3f;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Vertex;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class FaceOpsTest {
    @Test
    public void testCalcNormal() {
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

        Vector3f normal = faceOps.calcNormal(face);
        assertThat(normal.x, is(0.0f));
        assertThat(normal.y, is(0.0f));
        assertThat(normal.z, is(1.0f));
    }
}
