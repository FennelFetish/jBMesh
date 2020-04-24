package meshlib.structure;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.junit.Test;


public class VertexTest {
    @Test
    public void testCtor() {
        Vertex v = new Vertex();
        assertNull(v.edge);
    }

    @Test
    public void testDiskCycle() {
        Vertex center = new Vertex();
        Vertex v1 = new Vertex();
        Vertex v2 = new Vertex();
        Vertex v3 = new Vertex();

        Edge e1 = new Edge();
        e1.vertex0 = center;
        e1.vertex1 = v1;

        center.addEdge(e1);
        assertThat(center.edge, is(e1));

        Edge e2 = new Edge();
        e2.vertex0 = center;
        e2.vertex1 = v2;

        center.addEdge(e2);
        assertThat(center.edge, is(e1));
        assertThat(center.edge.getNextEdge(center), is(e2));
        assertThat(center.edge.getNextEdge(center).getNextEdge(center), is(e1));

        Edge e3 = new Edge();
        e3.vertex0 = center;
        e3.vertex1 = v3;

        center.addEdge(e3);
        assertThat(center.edge, is(e1));
        assertThat(center.edge.getNextEdge(center), is(e2));
        assertThat(center.edge.getNextEdge(center).getNextEdge(center), is(e3));
        assertThat(center.edge.getNextEdge(center).getNextEdge(center).getNextEdge(center), is(e1));

        // Test Vertex.getEdgeTo(Vertex)
        assertThat(center.getEdgeTo(v1), is(e1));
        assertThat(center.getEdgeTo(v2), is(e2));
        assertThat(center.getEdgeTo(v3), is(e3));

        Vertex v4 = new Vertex();
        assertNull(center.getEdgeTo(v4));
    }
}
