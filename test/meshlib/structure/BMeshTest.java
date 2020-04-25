package meshlib.structure;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
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

        try {
            edge1.getNextEdge(v2);
            assert false;
        }
        catch(IllegalArgumentException ex) {}
        catch(Exception ex) { assert false; }

        // Add second Edge
        Edge edge2 = bmesh.createEdge(v1, v2);
        assertThat(edge2.vertex0, is(v1));
        assertThat(edge2.vertex1, is(v2));
        assertNull(edge2.loop);

        assertThat(edge1.getNextEdge(v0), is(edge1));
        assertThat(edge1.getNextEdge(v1), is(edge2));
        assertThat(edge1.getPrevEdge(v0), is(edge1));
        assertThat(edge1.getPrevEdge(v1), is(edge2));

        assertThat(edge2.getNextEdge(v1), is(edge1));
        assertThat(edge2.getNextEdge(v2), is(edge2));
        assertThat(edge2.getPrevEdge(v1), is(edge1));
        assertThat(edge2.getPrevEdge(v2), is(edge2));

        try {
            edge2.getNextEdge(v0);
            assert false;
        }
        catch(IllegalArgumentException ex) {}
        catch(Exception ex) { assert false; }
    }


    @Test
    public void testCreateFace() {
        
    }
}
