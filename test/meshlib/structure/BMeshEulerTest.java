package meshlib.structure;

import java.util.Iterator;
import meshlib.TestUtil;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BMeshEulerTest {
    @Test
    public void testSplitEdge() {
        BMesh bmesh = new BMesh();

        TestUtil.assertThrows(NullPointerException.class, () -> {
            bmesh.splitEdge(null);
        });

        // Setup
        Vertex vEdge0 = bmesh.createVertex();
        Vertex vEdge1 = bmesh.createVertex();

        Vertex vFace1 = bmesh.createVertex();
        Vertex vFace2 = bmesh.createVertex();
        Vertex vFace3 = bmesh.createVertex();

        Face face1 = bmesh.createFace(vEdge0, vEdge1, vFace1);
        Face face2 = bmesh.createFace(vEdge1, vEdge0, vFace2);
        Face face3 = bmesh.createFace(vEdge0, vEdge1, vFace3);

        Loop[] face1LoopsBefore = TestUtil.getLoops(face1);
        Loop[] face2LoopsBefore = TestUtil.getLoops(face2);
        Loop[] face3LoopsBefore = TestUtil.getLoops(face3);

        assertThat(bmesh.vertices().size(), is(5));
        assertThat(bmesh.edges().size(), is(7));
        assertThat(bmesh.faces().size(), is(3));
        assertThat(bmesh.loops().size(), is(9));

        // Do split
        Edge edge = vEdge0.getEdgeTo(vEdge1);
        Vertex vSplit = bmesh.splitEdge(edge);

        assertThat(bmesh.vertices().size(), is(6));
        assertThat(bmesh.edges().size(), is(8));
        assertThat(bmesh.faces().size(), is(3));
        assertThat(bmesh.loops().size(), is(12));

        // Test edge connectivity
        assertThat(vSplit.getEdgeTo(vEdge0), is(edge));
        Edge newEdge = vSplit.getEdgeTo(vEdge1);
        assertThat(newEdge.getCommonVertex(edge), is(vSplit));

        {
            Iterator<Edge> it = vEdge1.edges().iterator();
            assertTrue(it.next().connects(vEdge1, vFace1));
            assertTrue(it.next().connects(vEdge1, vFace2));
            assertTrue(it.next().connects(vEdge1, vFace3));
            assertThat(it.next(), is(newEdge));
        }

        // Test face-loop connectivity
        Loop[] face1LoopsAfter = TestUtil.getLoops(face1);
        Loop[] face2LoopsAfter = TestUtil.getLoops(face2);
        Loop[] face3LoopsAfter = TestUtil.getLoops(face3);

        assertThat(face1LoopsAfter.length, is(4));
        assertThat(face2LoopsAfter.length, is(4));
        assertThat(face3LoopsAfter.length, is(4));

        // Face.loop reference doesn't change
        assertThat(face1LoopsAfter[0], is(face1LoopsBefore[0]));
        assertThat(face2LoopsAfter[0], is(face2LoopsBefore[0]));
        assertThat(face3LoopsAfter[0], is(face3LoopsBefore[0]));

        // [1] is the new Loop in Face 1
        assertThat(face1LoopsAfter[0].nextFaceLoop, is(face1LoopsAfter[1]));
        assertThat(face1LoopsAfter[1].nextFaceLoop, is(face1LoopsAfter[2]));
        assertThat(face1LoopsAfter[1].prevFaceLoop, is(face1LoopsAfter[0]));
        assertThat(face1LoopsAfter[2].prevFaceLoop, is(face1LoopsAfter[1]));
        assertThat(face1LoopsAfter[2], is(face1LoopsBefore[1]));
        assertThat(face1LoopsAfter[3], is(face1LoopsBefore[2]));

        // [3] is the new Loop in Face 2
        assertThat(face2LoopsAfter[2].nextFaceLoop, is(face2LoopsAfter[3]));
        assertThat(face2LoopsAfter[3].nextFaceLoop, is(face2LoopsAfter[0]));
        assertThat(face2LoopsAfter[3].prevFaceLoop, is(face2LoopsAfter[2]));
        assertThat(face2LoopsAfter[0].prevFaceLoop, is(face2LoopsAfter[3]));
        assertThat(face2LoopsAfter[1], is(face2LoopsBefore[1]));
        assertThat(face2LoopsAfter[2], is(face2LoopsBefore[2]));

        // [1] is the new Loop in Face 3
        assertThat(face3LoopsAfter[0].nextFaceLoop, is(face3LoopsAfter[1]));
        assertThat(face3LoopsAfter[1].nextFaceLoop, is(face3LoopsAfter[2]));
        assertThat(face3LoopsAfter[1].prevFaceLoop, is(face3LoopsAfter[0]));
        assertThat(face3LoopsAfter[2].prevFaceLoop, is(face3LoopsAfter[1]));
        assertThat(face3LoopsAfter[2], is(face3LoopsBefore[1]));
        assertThat(face3LoopsAfter[3], is(face3LoopsBefore[2]));
    }


    @Test
    public void testJoinEdge() {
        BMesh bmesh = new BMesh();

        /**
         *       vt
         *     /    \
         *   /        \
         * v0===(v1)---v2
         *   \        /
         *     \    /
         *       vb
         */

        Vertex v0 = bmesh.createVertex();
        Vertex v1 = bmesh.createVertex();
        Vertex v2 = bmesh.createVertex();

        Vertex vt = bmesh.createVertex();
        Vertex vb = bmesh.createVertex();

        Face ft = bmesh.createFace(v0, v1, v2, vt);
        Face fb = bmesh.createFace(vb, v2, v1, v0);

        Edge e0 = v0.getEdgeTo(v1);
        bmesh.joinEdge(e0, v1);

        assertThat(bmesh.verts().size(), is(4));
        assertThat(bmesh.edges().size(), is(5));
        assertThat(bmesh.faces().size(), is(2));
        assertThat(bmesh.loops().size(), is(6));

        assertFalse(e0.isAlive());
        assertFalse(v1.isAlive());

        e0 = v0.getEdgeTo(v2);

        // Test radial and loop cycle
        Iterator<Loop> itLoop = e0.loops().iterator();
        Loop loop = itLoop.next();
        assertThat(loop.face, is(ft));
        assertTrue(loop.prevFaceLoop.edge.connects(v0, vt));
        assertTrue(loop.nextFaceLoop.edge.connects(v2, vt));
        assertTrue(loop.nextEdgeLoop.nextEdgeLoop == loop);

        loop = itLoop.next();
        assertThat(loop.face, is(fb));
        assertTrue(loop.prevFaceLoop.edge.connects(v2, vb));
        assertTrue(loop.nextFaceLoop.edge.connects(v0, vb));
        assertTrue(loop.nextEdgeLoop.nextEdgeLoop == loop);

        assertFalse(itLoop.hasNext());

        // Test disk cycle
        Iterator<Edge> itEdge = v0.edges().iterator();
        assertTrue(itEdge.next().connects(v0, vt));
        assertTrue(itEdge.next().connects(v0, vb));
        assertTrue(itEdge.next().connects(v0, v2));
        assertFalse(itEdge.hasNext());

        itEdge = v2.edges().iterator();
        assertTrue(itEdge.next().connects(v2, v0));
        assertTrue(itEdge.next().connects(v2, vt));
        assertTrue(itEdge.next().connects(v2, vb));
        assertFalse(itEdge.hasNext());
    }


    @Test
    public void testInvertFace() {
        BMesh bmesh = new BMesh();

        Vertex v0 = bmesh.createVertex();
        Vertex v1 = bmesh.createVertex();
        Vertex v2 = bmesh.createVertex();

        Face face = bmesh.createFace(v0, v1, v2);
        Loop[] originalLoops = TestUtil.getLoops(face);
        Loop[] expectedLoops = new Loop[] { originalLoops[0], originalLoops[2], originalLoops[1] };

        bmesh.invertFace(face);
        assertThat(face.loop, is(originalLoops[0]));
        Loop[] invertedLoops = TestUtil.getLoops(face);

        assertThat(invertedLoops[0], is(expectedLoops[0]));
        assertThat(invertedLoops[1], is(expectedLoops[1]));
        assertThat(invertedLoops[2], is(expectedLoops[2]));

        assertThat(invertedLoops[0].nextFaceLoop, is(expectedLoops[1]));
        assertThat(invertedLoops[1].nextFaceLoop, is(expectedLoops[2]));
        assertThat(invertedLoops[2].nextFaceLoop, is(expectedLoops[0]));

        assertThat(invertedLoops[0].prevFaceLoop, is(expectedLoops[2]));
        assertThat(invertedLoops[1].prevFaceLoop, is(expectedLoops[0]));
        assertThat(invertedLoops[2].prevFaceLoop, is(expectedLoops[1]));

        assertThat(invertedLoops[0].vertex, is(v0));
        assertThat(invertedLoops[1].vertex, is(v2));
        assertThat(invertedLoops[2].vertex, is(v1));
    }
}
