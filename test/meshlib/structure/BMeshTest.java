package meshlib.structure;

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

        Loop[] loops = TestUtil.getLoops(face);
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
}
