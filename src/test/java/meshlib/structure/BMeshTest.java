package meshlib.structure;

import meshlib.TestUtil;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BMeshTest {
    @Test
    public void testRemoveVertex() {
        BMesh bmesh = new BMesh();

        // Setup 3 faces around a vertex
        // (triangle where vertices are connected to a 4th vertex at the center)
        Vertex center = bmesh.createVertex();
        Vertex v0 = bmesh.createVertex();
        Vertex v1 = bmesh.createVertex();
        Vertex v2 = bmesh.createVertex();

        Face f0 = bmesh.createFace(v0, v1, center);
        Face f1 = bmesh.createFace(v1, v2, center);
        Face f2 = bmesh.createFace(v2, v0, center);

        Edge e0 = center.getEdgeTo(v0);
        Edge e1 = center.getEdgeTo(v1);
        Edge e2 = center.getEdgeTo(v2);
        assertThat(bmesh.edges().size(), is(6));

        bmesh.removeVertex(center);

        assertFalse(center.isAlive());
        assertTrue(v0.isAlive());
        assertTrue(v1.isAlive());
        assertTrue(v2.isAlive());
        assertThat(bmesh.vertices().size(), is(3));

        assertFalse(e0.isAlive());
        assertFalse(e1.isAlive());
        assertFalse(e2.isAlive());
        assertThat(bmesh.edges().size(), is(3));

        assertFalse(f0.isAlive());
        assertFalse(f1.isAlive());
        assertFalse(f2.isAlive());
        assertThat(bmesh.faces().size(), is(0));

        Edge e0_1 = v0.getEdgeTo(v1);
        Edge e1_2 = v1.getEdgeTo(v2);
        Edge e2_0 = v2.getEdgeTo(v0);

        assertNotNull(e0_1);
        assertNotNull(e1_2);
        assertNotNull(e2_0);

        assertTrue(e0_1.getNextEdge(v1).isAdjacentTo(v2));
        assertThat(e0_1.getNextEdge(v1).getNextEdge(v1), is(e0_1));
        assertTrue(e1_2.getNextEdge(v2).isAdjacentTo(v0));
        assertThat(e1_2.getNextEdge(v2).getNextEdge(v2), is(e1_2));
        assertTrue(e2_0.getNextEdge(v0).isAdjacentTo(v1));
        assertThat(e2_0.getNextEdge(v0).getNextEdge(v0), is(e2_0));

        assertNull(e0_1.loop);
        assertNull(e1_2.loop);
        assertNull(e2_0.loop);
        assertThat(bmesh.loops().size(), is(0));
    }


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
    public void testRemoveEdge() {
        BMesh bmesh = new BMesh();

        // Setup rhombus with horizontal edge in the middle
        Vertex v0 = bmesh.createVertex(); // Left
        Vertex v1 = bmesh.createVertex(); // Right
        Vertex vt = bmesh.createVertex(); // Top
        Vertex vb = bmesh.createVertex(); // Bottom

        Face ft = bmesh.createFace(v0, v1, vt);
        Face fb = bmesh.createFace(v1, v0, vb);

        Edge edge = v0.getEdgeTo(v1);
        bmesh.removeEdge(edge);

        assertFalse(edge.isAlive());
        assertFalse(ft.isAlive());
        assertFalse(fb.isAlive());

        assertThat(bmesh.vertices().size(), is(4));
        assertThat(bmesh.edges().size(), is(4));
        assertThat(bmesh.faces().size(), is(0));
        assertThat(bmesh.loops().size(), is(0));

        assertNull(v0.getEdgeTo(v1));
        Edge e0_t = v0.getEdgeTo(vt);
        Edge e0_b = v0.getEdgeTo(vb);
        Edge e1_t = v1.getEdgeTo(vt);
        Edge e1_b = v1.getEdgeTo(vb);

        assertNull(e0_t.loop);
        assertNull(e0_b.loop);
        assertNull(e1_t.loop);
        assertNull(e1_b.loop);
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
        TestUtil.assertFace(face, v0, v1, v2);

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


    @Test
    public void testRemoveFace() {
        BMesh bmesh = new BMesh();

        // Setup triangle
        Vertex v0 = bmesh.createVertex();
        Vertex v1 = bmesh.createVertex();
        Vertex v2 = bmesh.createVertex();

        Face face = bmesh.createFace(v0, v1, v2);
        bmesh.removeFace(face);

        assertFalse(face.isAlive());

        assertThat(bmesh.vertices().size(), is(3));
        assertThat(bmesh.edges().size(), is(3));
        assertThat(bmesh.faces().size(), is(0));
        assertThat(bmesh.loops().size(), is(0));

        Edge e0 = v0.getEdgeTo(v1);
        Edge e1 = v1.getEdgeTo(v2);
        Edge e2 = v2.getEdgeTo(v0);

        assertNull(e0.loop);
        assertNull(e1.loop);
        assertNull(e2.loop);
    }
}
