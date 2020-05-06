package meshlib.structure;

import meshlib.TestUtil;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

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
        assertEquals(6, bmesh.edges().size());

        bmesh.removeVertex(center);

        assertFalse(center.isAlive());
        assertTrue(v0.isAlive());
        assertTrue(v1.isAlive());
        assertTrue(v2.isAlive());
        assertEquals(3, bmesh.vertices().size());

        assertFalse(e0.isAlive());
        assertFalse(e1.isAlive());
        assertFalse(e2.isAlive());
        assertEquals(3, bmesh.edges().size());

        assertFalse(f0.isAlive());
        assertFalse(f1.isAlive());
        assertFalse(f2.isAlive());
        assertEquals(0, bmesh.faces().size());

        Edge e0_1 = v0.getEdgeTo(v1);
        Edge e1_2 = v1.getEdgeTo(v2);
        Edge e2_0 = v2.getEdgeTo(v0);

        assertNotNull(e0_1);
        assertNotNull(e1_2);
        assertNotNull(e2_0);

        assertTrue(e0_1.getNextEdge(v1).isAdjacentTo(v2));
        assertEquals(e0_1, e0_1.getNextEdge(v1).getNextEdge(v1));
        assertTrue(e1_2.getNextEdge(v2).isAdjacentTo(v0));
        assertEquals(e1_2, e1_2.getNextEdge(v2).getNextEdge(v2));
        assertTrue(e2_0.getNextEdge(v0).isAdjacentTo(v1));
        assertEquals(e2_0, e2_0.getNextEdge(v0).getNextEdge(v0));

        assertNull(e0_1.loop);
        assertNull(e1_2.loop);
        assertNull(e2_0.loop);
        assertEquals(0, bmesh.loops().size());
    }


    @Test
    public void testCreateEdge() {
        BMesh bmesh = new BMesh();

        Vertex v0 = bmesh.createVertex();
        Vertex v1 = bmesh.createVertex();
        Vertex v2 = bmesh.createVertex();

        // Add first Edge
        Edge edge1 = bmesh.createEdge(v0, v1);
        assertEquals(v0, edge1.vertex0);
        assertEquals(v1, edge1.vertex1);
        assertNull(edge1.loop);

        assertEquals(edge1, edge1.getNextEdge(v0));
        assertEquals(edge1, edge1.getNextEdge(v1));
        assertEquals(edge1, edge1.getPrevEdge(v0));
        assertEquals(edge1, edge1.getPrevEdge(v1));

        assertThrows(IllegalArgumentException.class, () -> {
            edge1.getNextEdge(v2);
        }, "Edge is not adjacent to Vertex");

        // Add second Edge
        Edge edge2 = bmesh.createEdge(v1, v2);
        assertEquals(v1, edge2.vertex0);
        assertEquals(v2, edge2.vertex1);
        assertNull(edge2.loop);

        assertEquals(edge1, edge1.getNextEdge(v0));
        assertEquals(edge2, edge1.getNextEdge(v1));
        assertEquals(edge1, edge1.getPrevEdge(v0));
        assertEquals(edge2, edge1.getPrevEdge(v1));

        assertEquals(edge1, edge1.getNextEdge(v1).getNextEdge(v1));
        assertEquals(edge1, edge1.getPrevEdge(v1).getPrevEdge(v1));
        assertEquals(edge1, edge1.getNextEdge(v1).getPrevEdge(v1));
        assertEquals(edge1, edge1.getPrevEdge(v1).getNextEdge(v1));

        assertEquals(edge1, edge2.getNextEdge(v1));
        assertEquals(edge2, edge2.getNextEdge(v2));
        assertEquals(edge1, edge2.getPrevEdge(v1));
        assertEquals(edge2, edge2.getPrevEdge(v2));

        assertEquals(edge2, edge2.getNextEdge(v1).getNextEdge(v1));
        assertEquals(edge2, edge2.getPrevEdge(v1).getPrevEdge(v1));
        assertEquals(edge2, edge2.getNextEdge(v1).getPrevEdge(v1));
        assertEquals(edge2, edge2.getPrevEdge(v1).getNextEdge(v1));

        assertThrows(IllegalArgumentException.class, () -> {
            edge2.getNextEdge(v0);
        }, "Edge is not adjacent to Vertex");
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

        assertEquals(4, bmesh.vertices().size());
        assertEquals(4, bmesh.edges().size());
        assertEquals(0, bmesh.faces().size());
        assertEquals(0, bmesh.loops().size());

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

        assertThrows(IllegalArgumentException.class, () -> {
            bmesh.createFace(v0, v1);
        }, "A face needs at least 3 vertices");

        assertThrows(NullPointerException.class, () -> {
            bmesh.createFace((Vertex[]) null);
        });

        assertThrows(NullPointerException.class, () -> {
            bmesh.createFace(v0, v1, null);
        });

        assertEquals(3, bmesh.vertices().size());
        assertEquals(0, bmesh.edges().size());
        assertEquals(0, bmesh.faces().size());
        assertEquals(0, bmesh.loops().size());

        Face face = bmesh.createFace(v0, v1, v2);
        TestUtil.assertFace(face, v0, v1, v2);

        assertEquals(3, bmesh.vertices().size());
        assertEquals(3, bmesh.edges().size());
        assertEquals(1, bmesh.faces().size());
        assertEquals(3, bmesh.loops().size());
        
        Edge e1 = v0.getEdgeTo(v1);
        Edge e2 = v1.getEdgeTo(v2);
        Edge e3 = v2.getEdgeTo(v0);

        assertTrue(e1.connects(v0, v1));
        assertTrue(e2.connects(v1, v2));
        assertTrue(e3.connects(v2, v0));

        Loop[] loops = TestUtil.getLoops(face);
        assertEquals(3, loops.length);
        for(Loop loop : loops)
            assertEquals(face, loop.face);

        assertEquals(loops[1], loops[0].nextFaceLoop);
        assertEquals(loops[2], loops[1].nextFaceLoop);
        assertEquals(loops[0], loops[2].nextFaceLoop);

        assertEquals(loops[2], loops[0].prevFaceLoop);
        assertEquals(loops[0], loops[1].prevFaceLoop);
        assertEquals(loops[1], loops[2].prevFaceLoop);

        assertEquals(e1, loops[0].edge);
        assertEquals(e2, loops[1].edge);
        assertEquals(e3, loops[2].edge);

        assertEquals(v0, loops[0].vertex);
        assertEquals(v1, loops[1].vertex);
        assertEquals(v2, loops[2].vertex);
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

        assertEquals(3, bmesh.vertices().size());
        assertEquals(3, bmesh.edges().size());
        assertEquals(0, bmesh.faces().size());
        assertEquals(0, bmesh.loops().size());

        Edge e0 = v0.getEdgeTo(v1);
        Edge e1 = v1.getEdgeTo(v2);
        Edge e2 = v2.getEdgeTo(v0);

        assertNull(e0.loop);
        assertNull(e1.loop);
        assertNull(e2.loop);
    }
}
