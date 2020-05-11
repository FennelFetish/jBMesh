package meshlib.structure;

import java.util.Iterator;
import meshlib.TestUtil;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class BMeshEulerTest {
    @Test
    public void testSplitEdge() {
        BMesh bmesh = new BMesh();

        assertThrows(NullPointerException.class, () -> {
            bmesh.splitEdge(null);
        });

        // Setup 3 faces along one edge
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

        assertEquals(5, bmesh.vertices().size());
        assertEquals(7, bmesh.edges().size());
        assertEquals(3, bmesh.faces().size());
        assertEquals(9, bmesh.loops().size());

        // Do split
        Edge edge = vEdge0.getEdgeTo(vEdge1);
        Vertex vSplit = bmesh.splitEdge(edge);

        TestUtil.assertFace(face1, vEdge0, vSplit, vEdge1, vFace1);
        TestUtil.assertFace(face2, vSplit, vEdge0, vFace2, vEdge1);
        TestUtil.assertFace(face3, vEdge0, vSplit, vEdge1, vFace3);

        assertEquals(6, bmesh.vertices().size());
        assertEquals(8, bmesh.edges().size());
        assertEquals(3, bmesh.faces().size());
        assertEquals(12, bmesh.loops().size());

        // Test edge connectivity
        assertEquals(edge, vSplit.getEdgeTo(vEdge0));
        Edge newEdge = vSplit.getEdgeTo(vEdge1);
        assertEquals(vSplit, newEdge.getCommonVertex(edge));

        {
            Iterator<Edge> it = vEdge1.edges().iterator();
            assertTrue(it.next().connects(vEdge1, vFace1));
            assertTrue(it.next().connects(vEdge1, vFace2));
            assertTrue(it.next().connects(vEdge1, vFace3));
            assertEquals(newEdge, it.next());
        }

        // Test face-loop connectivity
        Loop[] face1LoopsAfter = TestUtil.getLoops(face1);
        Loop[] face2LoopsAfter = TestUtil.getLoops(face2);
        Loop[] face3LoopsAfter = TestUtil.getLoops(face3);

        assertEquals(4, face1LoopsAfter.length);
        assertEquals(4, face2LoopsAfter.length);
        assertEquals(4, face3LoopsAfter.length);

        // Face.loop reference doesn't change
        assertEquals(face1LoopsBefore[0], face1LoopsAfter[0]);
        assertEquals(face2LoopsBefore[0], face2LoopsAfter[0]);
        assertEquals(face3LoopsBefore[0], face3LoopsAfter[0]);

        // [1] is the new Loop in Face 1
        assertEquals(face1LoopsAfter[1], face1LoopsAfter[0].nextFaceLoop);
        assertEquals(face1LoopsAfter[2], face1LoopsAfter[1].nextFaceLoop);
        assertEquals(face1LoopsAfter[0], face1LoopsAfter[1].prevFaceLoop);
        assertEquals(face1LoopsAfter[1], face1LoopsAfter[2].prevFaceLoop);
        assertEquals(face1LoopsBefore[1], face1LoopsAfter[2]);
        assertEquals(face1LoopsBefore[2], face1LoopsAfter[3]);

        // [3] is the new Loop in Face 2
        assertEquals(face2LoopsAfter[3], face2LoopsAfter[2].nextFaceLoop);
        assertEquals(face2LoopsAfter[0], face2LoopsAfter[3].nextFaceLoop);
        assertEquals(face2LoopsAfter[2], face2LoopsAfter[3].prevFaceLoop);
        assertEquals(face2LoopsAfter[3], face2LoopsAfter[0].prevFaceLoop);
        assertEquals(face2LoopsBefore[1], face2LoopsAfter[1]);
        assertEquals(face2LoopsBefore[2], face2LoopsAfter[2]);

        // [1] is the new Loop in Face 3
        assertEquals(face3LoopsAfter[1], face3LoopsAfter[0].nextFaceLoop);
        assertEquals(face3LoopsAfter[2], face3LoopsAfter[1].nextFaceLoop);
        assertEquals(face3LoopsAfter[0], face3LoopsAfter[1].prevFaceLoop);
        assertEquals(face3LoopsAfter[1], face3LoopsAfter[2].prevFaceLoop);
        assertEquals(face3LoopsBefore[1], face3LoopsAfter[2]);
        assertEquals(face3LoopsBefore[2], face3LoopsAfter[3]);
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

        TestUtil.assertFace(ft, v0, v2, vt);
        TestUtil.assertFace(fb, vb, v2, v0);

        assertEquals(4, bmesh.vertices().size());
        assertEquals(5, bmesh.edges().size());
        assertEquals(2, bmesh.faces().size());
        assertEquals(6, bmesh.loops().size());

        assertFalse(e0.isAlive());
        assertFalse(v1.isAlive());

        e0 = v0.getEdgeTo(v2);

        // Test radial and loop cycle
        Iterator<Loop> itLoop = e0.loops().iterator();
        Loop loop = itLoop.next();
        assertEquals(ft, loop.face);
        assertTrue(loop.prevFaceLoop.edge.connects(v0, vt));
        assertTrue(loop.nextFaceLoop.edge.connects(v2, vt));
        assertTrue(loop.nextEdgeLoop.nextEdgeLoop == loop);

        loop = itLoop.next();
        assertEquals(fb, loop.face);
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
    public void testSplitFace() {
        BMesh bmesh = new BMesh();

        // Setup rhombus
        Vertex v0 = bmesh.createVertex(); // Left
        Vertex v1 = bmesh.createVertex(); // Right
        Vertex vt = bmesh.createVertex(); // Top
        Vertex vb = bmesh.createVertex(); // Bottom

        Face face = bmesh.createFace(v0, vb, v1, vt);
        Edge edge = bmesh.splitFace(v0, v1);

        assertNotNull(edge);
        assertNotNull(edge.loop);
        assertEquals(edge, v0.getEdgeTo(v1));
        assertTrue(edge.connects(v0, v1));

        assertEquals(4, bmesh.vertices().size());
        assertEquals(5, bmesh.edges().size());
        assertEquals(2, bmesh.faces().size());
        assertEquals(6, bmesh.loops().size());

        assertEquals(face, edge.loop.face);
        assertEquals(edge, face.loop.edge);
        
        Face newFace = edge.loop.nextEdgeLoop.face;
        assertNotNull(newFace);
        assertNotEquals(face, newFace);
        assertEquals(edge, newFace.loop.edge);

        TestUtil.assertFace(newFace, v0, v1, vt);
        TestUtil.assertFace(face,    v1, v0, vb);
    }


    @Test
    public void testjoinFace() {
        BMesh bmesh = new BMesh();

        // Setup rhombus
        Vertex v0 = bmesh.createVertex(); // Left
        Vertex v1 = bmesh.createVertex(); // Right
        Vertex vt = bmesh.createVertex(); // Top
        Vertex vb = bmesh.createVertex(); // Bottom

        Face ft = bmesh.createFace(v0, v1, vt);
        Face fb = bmesh.createFace(v1, v0, vb);

        TestUtil.assertFace(ft, v0, v1, vt);
        TestUtil.assertFace(fb, v1, v0, vb);

        bmesh.joinFace(ft, fb);

        assertEquals(4, bmesh.vertices().size());
        assertEquals(4, bmesh.edges().size());
        assertEquals(1, bmesh.faces().size());
        assertEquals(4, bmesh.loops().size());

        assertTrue(ft.isAlive());
        assertFalse(fb.isAlive());
        TestUtil.assertFace(ft, v1, vt, v0, vb);
    }


    @Test
    public void testInvertFace() {
        BMesh bmesh = new BMesh();

        Vertex v0 = bmesh.createVertex();
        Vertex v1 = bmesh.createVertex();
        Vertex v2 = bmesh.createVertex();

        Face face = bmesh.createFace(v0, v1, v2);
        Loop[] originalLoops = TestUtil.getLoops(face);
        Loop[] expectedLoops = new Loop[] { originalLoops[2], originalLoops[1], originalLoops[0] };

        bmesh.invertFace(face);
        TestUtil.assertFace(face, v0, v2, v1);

        Loop[] invertedLoops = TestUtil.getLoops(face);

        assertEquals(expectedLoops[0], invertedLoops[0]);
        assertEquals(expectedLoops[1], invertedLoops[1]);
        assertEquals(expectedLoops[2], invertedLoops[2]);

        assertEquals(expectedLoops[1], invertedLoops[0].nextFaceLoop);
        assertEquals(expectedLoops[2], invertedLoops[1].nextFaceLoop);
        assertEquals(expectedLoops[0], invertedLoops[2].nextFaceLoop);

        assertEquals(expectedLoops[2], invertedLoops[0].prevFaceLoop);
        assertEquals(expectedLoops[0], invertedLoops[1].prevFaceLoop);
        assertEquals(expectedLoops[1], invertedLoops[2].prevFaceLoop);

        assertEquals(v0, invertedLoops[0].vertex);
        assertEquals(v2, invertedLoops[1].vertex);
        assertEquals(v1, invertedLoops[2].vertex);
    }
}
