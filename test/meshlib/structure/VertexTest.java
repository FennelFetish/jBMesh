package meshlib.structure;

import meshlib.TestUtil;
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
    public void testDiskCycleAdd() {
        Vertex center = new Vertex();
        Vertex v1 = new Vertex();
        Vertex v2 = new Vertex();
        Vertex v3 = new Vertex();

        Edge e1 = new Edge();

        TestUtil.assertThrows(IllegalArgumentException.class, "Edge is not adjacent to Vertex", () -> {
            center.addEdge(e1);
        });

        e1.vertex1 = v1;

        TestUtil.assertThrows(IllegalArgumentException.class, "Edge is not adjacent to Vertex", () -> {
            center.addEdge(e1);
        });

        e1.vertex0 = center;
        center.addEdge(e1);
        assertThat(center.edge, is(e1));
        assertDiskCycleNext(center, e1);
        assertDiskCyclePrev(center, e1);

        Edge e2 = new Edge();
        e2.vertex0 = center;
        e2.vertex1 = v2;
        center.addEdge(e2);
        assertDiskCycleNext(center, e1, e2, e1);
        assertDiskCyclePrev(center, e1, e2, e1);

        Edge e3 = new Edge();
        e3.vertex0 = center;
        e3.vertex1 = v3;
        center.addEdge(e3);
        assertDiskCycleNext(center, e1, e2, e3, e1);
        assertDiskCyclePrev(center, e1, e3, e2, e1);

        center.removeEdge(e2);
        assertThat(e2.getNextEdge(center), is(e2));
        assertThat(e2.getNextEdge(v2), is(e2));
        assertDiskCycleNext(center, e1, e3, e1);
        assertDiskCyclePrev(center, e1, e3, e1);

        TestUtil.assertThrows(IllegalArgumentException.class, "Edge does not exists in disk cycle for Vertex", () -> {
            center.removeEdge(e2);
        });

        center.addEdge(e2);
        assertDiskCycleNext(center, e1, e3, e2, e1);
        assertDiskCyclePrev(center, e1, e2, e3, e1);

        TestUtil.assertThrows(IllegalArgumentException.class, "Edge already associated with a disk cycle for this Vertex", () -> {
            center.addEdge(e2);
        });

        // Test Vertex.getEdgeTo(Vertex)
        assertThat(center.getEdgeTo(v1), is(e1));
        assertThat(center.getEdgeTo(v2), is(e2));
        assertThat(center.getEdgeTo(v3), is(e3));

        Vertex v4 = new Vertex();
        assertNull(center.getEdgeTo(v4));
    }


    @Test
    public void testDiskCycleRemove() {
        Vertex center = new Vertex();
        Vertex v1 = new Vertex();
        Vertex v2 = new Vertex();
        Vertex v3 = new Vertex();

        Edge e1 = new Edge();
        e1.vertex0 = center;
        e1.vertex1 = v1;
        center.addEdge(e1);

        Edge e2 = new Edge();
        e2.vertex0 = center;
        e2.vertex1 = v2;
        center.addEdge(e2);

        Edge e3 = new Edge();
        e3.vertex0 = center;
        e3.vertex1 = v3;
        center.addEdge(e3);

        // Test removal one-by-one
        center.removeEdge(e1);
        assertDiskCycleNext(center, e2, e3, e2);
        assertDiskCyclePrev(center, e2, e3, e2);

        TestUtil.assertThrows(IllegalArgumentException.class, "Edge does not exists in disk cycle for Vertex", () -> {
            center.removeEdge(e1);
        });

        center.removeEdge(e3);
        assertDiskCycleNext(center, e2, e2);
        assertDiskCyclePrev(center, e2, e2);

        center.removeEdge(e2);
        assertNull(center.edge);

        // Throw same IllegalArgumentException and not NPE when center.edge == null
        TestUtil.assertThrows(IllegalArgumentException.class, "Edge does not exists in disk cycle for Vertex", () -> {
            center.removeEdge(e1);
        });
    }


    @Test
    public void testEdgeNull() {
        Vertex v = new Vertex();

        TestUtil.assertThrows(NullPointerException.class, () -> {
            v.addEdge(null);
        });

        TestUtil.assertThrows(NullPointerException.class, () -> {
            v.removeEdge(null);
        });
    }


    private static void assertDiskCycleNext(Vertex vert, Edge... expectedEdges) {
        Edge[] actual = new Edge[expectedEdges.length];

        Edge edge = vert.edge;
        for(int i=0; i<expectedEdges.length; ++i) {
            actual[i] = edge;
            edge = edge.getNextEdge(vert);
        }

        assertThat(actual, is(expectedEdges));
    }

    private static void assertDiskCyclePrev(Vertex vert, Edge... expectedEdges) {
        Edge[] actual = new Edge[expectedEdges.length];

        Edge edge = vert.edge;
        for(int i=0; i<expectedEdges.length; ++i) {
            actual[i] = edge;
            edge = edge.getPrevEdge(vert);
        }

        assertThat(actual, is(expectedEdges));
    }
}
