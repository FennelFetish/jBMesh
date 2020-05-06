package meshlib.structure;

import meshlib.TestUtil;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class EdgeTest {
    @Test
    public void testCtor() {
        Vertex v0 = new Vertex();
        Vertex v1 = new Vertex();

        Edge edge = new Edge();
        edge.vertex0 = v0;
        edge.vertex1 = v1;
        
        assertNull(edge.loop);
        assertThat(edge.getNextEdge(v0), is(edge));
        assertThat(edge.getNextEdge(v1), is(edge));
    }


    @Test
    public void testNonAdjacent() {
        Vertex v0 = new Vertex();
        Vertex v1 = new Vertex();
        Vertex v2 = new Vertex();

        Edge edge = new Edge();
        edge.vertex0 = v0;
        edge.vertex1 = v1;

        TestUtil.assertThrows(IllegalArgumentException.class, "Edge is not adjacent to Vertex", () -> {
            edge.getNextEdge(v2);
        });

        TestUtil.assertThrows(IllegalArgumentException.class, "Edge is not adjacent to Vertex", () -> {
            v2.addEdge(edge);
        });
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

        assertThat(edge.loop, is(loop1));
        assertThat(edge.loop.nextEdgeLoop, is(loop1));
        assertThat(edge.loop.prevEdgeLoop, is(loop1));

        Loop loop2 = new Loop();
        loop2.edge = edge;
        edge.addLoop(loop2);

        assertThat(edge.loop, is(loop1));
        assertThat(edge.loop.nextEdgeLoop, is(loop2));
        assertThat(edge.loop.nextEdgeLoop.nextEdgeLoop, is(loop1));
        assertThat(edge.loop.prevEdgeLoop, is(loop2));
        assertThat(edge.loop.prevEdgeLoop.prevEdgeLoop, is(loop1));
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
        assertFalse(edge.isAdjacentTo(null));

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
        assertFalse(edge.isAdjacentTo(null));
    }
}
