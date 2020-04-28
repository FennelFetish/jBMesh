package meshlib.structure;

import java.util.ArrayList;
import java.util.Iterator;
import meshlib.TestUtil;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BMeshTest {
    @Test
    public void testCreateEdge() {
        BMesh bmesh = new BMesh();

        Vertex v0 = bmesh.createVertex();
        Vertex v1 = bmesh.createVertex();
        Vertex v2 = bmesh.createVertex();

        // Add first Edge
        Edge edge1 = bmesh.createEdge(v0, v1);
        assertThat(edge1.vertex0, is(v0));
        assertThat(edge1.vertex1, is(v1));
        assertNull(edge1.loop);

        assertThat(edge1.getNextEdge(v0), is(edge1));
        assertThat(edge1.getNextEdge(v1), is(edge1));
        assertThat(edge1.getPrevEdge(v0), is(edge1));
        assertThat(edge1.getPrevEdge(v1), is(edge1));

        TestUtil.assertThrows(IllegalArgumentException.class, "Edge is not adjacent to Vertex", () -> {
            edge1.getNextEdge(v2);
        });

        // Add second Edge
        Edge edge2 = bmesh.createEdge(v1, v2);
        assertThat(edge2.vertex0, is(v1));
        assertThat(edge2.vertex1, is(v2));
        assertNull(edge2.loop);

        assertThat(edge1.getNextEdge(v0), is(edge1));
        assertThat(edge1.getNextEdge(v1), is(edge2));
        assertThat(edge1.getPrevEdge(v0), is(edge1));
        assertThat(edge1.getPrevEdge(v1), is(edge2));

        assertThat(edge1.getNextEdge(v1).getNextEdge(v1), is(edge1));
        assertThat(edge1.getPrevEdge(v1).getPrevEdge(v1), is(edge1));
        assertThat(edge1.getNextEdge(v1).getPrevEdge(v1), is(edge1));
        assertThat(edge1.getPrevEdge(v1).getNextEdge(v1), is(edge1));

        assertThat(edge2.getNextEdge(v1), is(edge1));
        assertThat(edge2.getNextEdge(v2), is(edge2));
        assertThat(edge2.getPrevEdge(v1), is(edge1));
        assertThat(edge2.getPrevEdge(v2), is(edge2));

        assertThat(edge2.getNextEdge(v1).getNextEdge(v1), is(edge2));
        assertThat(edge2.getPrevEdge(v1).getPrevEdge(v1), is(edge2));
        assertThat(edge2.getNextEdge(v1).getPrevEdge(v1), is(edge2));
        assertThat(edge2.getPrevEdge(v1).getNextEdge(v1), is(edge2));

        TestUtil.assertThrows(IllegalArgumentException.class, "Edge is not adjacent to Vertex", () -> {
            edge2.getNextEdge(v0);
        });
    }


    @Test
    public void testCreateFace() {
        BMesh bmesh = new BMesh();

        Vertex v0 = bmesh.createVertex();
        Vertex v1 = bmesh.createVertex();
        Vertex v2 = bmesh.createVertex();

        TestUtil.assertThrows(IllegalArgumentException.class, "A face needs at least 3 vertices", () -> {
            bmesh.createFace(v0, v1);
        });

        TestUtil.assertThrows(NullPointerException.class, () -> {
            bmesh.createFace(null);
        });

        TestUtil.assertThrows(NullPointerException.class, () -> {
            bmesh.createFace(v0, v1, null);
        });

        assertThat(bmesh.vertices().size(), is(3));
        assertThat(bmesh.edges().size(), is(0));
        assertThat(bmesh.faces().size(), is(0));
        assertThat(bmesh.loops().size(), is(0));

        Face face = bmesh.createFace(v0, v1, v2);

        assertThat(bmesh.vertices().size(), is(3));
        assertThat(bmesh.edges().size(), is(3));
        assertThat(bmesh.faces().size(), is(1));
        assertThat(bmesh.loops().size(), is(3));
        
        Edge e1 = v0.getEdgeTo(v1);
        Edge e2 = v1.getEdgeTo(v2);
        Edge e3 = v2.getEdgeTo(v0);

        assertTrue(e1.connects(v0, v1));
        assertTrue(e2.connects(v1, v2));
        assertTrue(e3.connects(v2, v0));

        Loop[] loops = getLoops(face);
        assertThat(loops.length, is(3));
        for(Loop loop : loops)
            assertThat(loop.face, is(face));

        assertThat(loops[0].nextFaceLoop, is(loops[1]));
        assertThat(loops[1].nextFaceLoop, is(loops[2]));
        assertThat(loops[2].nextFaceLoop, is(loops[0]));

        assertThat(loops[0].prevFaceLoop, is(loops[2]));
        assertThat(loops[1].prevFaceLoop, is(loops[0]));
        assertThat(loops[2].prevFaceLoop, is(loops[1]));

        assertThat(loops[0].edge, is(e1));
        assertThat(loops[1].edge, is(e2));
        assertThat(loops[2].edge, is(e3));

        assertThat(loops[0].vertex, is(v0));
        assertThat(loops[1].vertex, is(v1));
        assertThat(loops[2].vertex, is(v2));
    }


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

        Loop[] face1LoopsBefore = getLoops(face1);
        Loop[] face2LoopsBefore = getLoops(face2);
        Loop[] face3LoopsBefore = getLoops(face3);

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
        Loop[] face1LoopsAfter = getLoops(face1);
        Loop[] face2LoopsAfter = getLoops(face2);
        Loop[] face3LoopsAfter = getLoops(face3);

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
    public void testInvertFace() {
        BMesh bmesh = new BMesh();

        Vertex v0 = bmesh.createVertex();
        Vertex v1 = bmesh.createVertex();
        Vertex v2 = bmesh.createVertex();

        Face face = bmesh.createFace(v0, v1, v2);
        Loop[] originalLoops = getLoops(face);
        Loop[] expectedLoops = new Loop[] { originalLoops[0], originalLoops[2], originalLoops[1] };

        bmesh.invertFace(face);
        assertThat(face.loop, is(originalLoops[0]));
        Loop[] invertedLoops = getLoops(face);
        
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


    private static Loop[] getLoops(Face face) {
        ArrayList<Loop> loops = new ArrayList<>(3);
        for(Loop loop : face.loops())
            loops.add(loop);
        return loops.toArray(new Loop[loops.size()]);
    }
}
