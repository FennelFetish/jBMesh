package meshlib.structure;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class EdgeTest {
    @Test
    public void testCtor() {
        Vertex v0 = new Vertex();
        Vertex v1 = new Vertex();

        Edge edge = new Edge();
        edge.vertex0 = v0;
        edge.vertex1 = v1;
        
        assertNull(edge.loop);
        assertEquals(edge, edge.getNextEdge(v0));
        assertEquals(edge, edge.getNextEdge(v1));
    }


    @Test
    public void testNonAdjacent() {
        Vertex v0 = new Vertex();
        Vertex v1 = new Vertex();
        Vertex v2 = new Vertex();

        Edge edge = new Edge();
        edge.vertex0 = v0;
        edge.vertex1 = v1;

        assertThrows(IllegalArgumentException.class, () -> {
            edge.getNextEdge(v2);
        }, "Edge is not adjacent to Vertex");

        assertThrows(IllegalArgumentException.class, () -> {
            v2.addEdge(edge);
        }, "Edge is not adjacent to Vertex");
    }


    @Test
    public void testRadialCycle() {
        Vertex v0 = new Vertex();
        Vertex v1 = new Vertex();

        Edge edge = new Edge();
        edge.vertex0 = v0;
        edge.vertex1 = v1;

        assertNull(edge.loop);

        Loop loop1 = new Loop();
        loop1.edge = edge;
        edge.addLoop(loop1);

        assertEquals(loop1, edge.loop);
        assertEquals(loop1, edge.loop.nextEdgeLoop);
        assertEquals(loop1, edge.loop.prevEdgeLoop);

        Loop loop2 = new Loop();
        loop2.edge = edge;
        edge.addLoop(loop2);

        assertEquals(loop1, edge.loop);
        assertEquals(loop2, edge.loop.nextEdgeLoop);
        assertEquals(loop1, edge.loop.nextEdgeLoop.nextEdgeLoop);
        assertEquals(loop2, edge.loop.prevEdgeLoop);
        assertEquals(loop1, edge.loop.prevEdgeLoop.prevEdgeLoop);
    }


    @Test
    public void testAdjacency() {
        Vertex v0 = new Vertex();
        Vertex v1 = new Vertex();
        Vertex v2 = new Vertex();
        Vertex v3 = new Vertex();

        Edge edge = new Edge();
        assertFalse(edge.connects(null, null));
        assertFalse(edge.connects(v0, null));
        assertFalse(edge.isAdjacentTo(v0));
        assertFalse(edge.isAdjacentTo((Vertex)null));

        edge.vertex0 = v0;
        assertFalse(edge.connects(v0, null));
        assertFalse(edge.connects(null, v0));
        assertTrue(edge.isAdjacentTo(v0));

        edge.vertex1 = v1;
        assertTrue(edge.connects(v0, v1));
        assertTrue(edge.connects(v1, v0));
        assertFalse(edge.connects(v0, v2));
        assertFalse(edge.connects(v2, v0));
        assertFalse(edge.connects(v2, v3));

        assertTrue(edge.isAdjacentTo(v0));
        assertTrue(edge.isAdjacentTo(v1));
        assertFalse(edge.isAdjacentTo(v2));
        assertFalse(edge.isAdjacentTo((Vertex)null));
    }
}
