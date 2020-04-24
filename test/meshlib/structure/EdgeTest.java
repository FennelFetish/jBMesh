package meshlib.structure;

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

        try {
            edge.getNextEdge(v2);
            assert false;
        }
        catch(IllegalArgumentException ex) {}
        catch(Exception ex) { assert false; }

        try {
            v2.addEdge(edge);
            assert false;
        }
        catch(IllegalArgumentException ex) {}
        catch(Exception ex) { assert false; }
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
        edge.addLoop(loop1);

        assertThat(edge.loop, is(loop1));
        assertThat(edge.loop.nextEdgeLoop, is(loop1));

        Loop loop2 = new Loop();
        edge.addLoop(loop2);

        assertThat(edge.loop, is(loop1));
        assertThat(edge.loop.nextEdgeLoop, is(loop2));
        assertThat(edge.loop.nextEdgeLoop.nextEdgeLoop, is(loop1));
    }


    @Test
    public void testConnects() {
        Vertex v0 = new Vertex();
        Vertex v1 = new Vertex();
        Vertex v2 = new Vertex();
        Vertex v3 = new Vertex();

        Edge e1 = new Edge();
        e1.vertex0 = v0;
        e1.vertex1 = v1;

        assertTrue(e1.connects(v0, v1));
        assertTrue(e1.connects(v1, v0));
        assertFalse(e1.connects(v0, v2));
        assertFalse(e1.connects(v2, v0));
        assertFalse(e1.connects(v2, v3));
    }
}
