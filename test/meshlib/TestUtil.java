package meshlib;

import java.util.ArrayList;
import meshlib.structure.Face;
import meshlib.structure.Loop;
import meshlib.structure.Vertex;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestUtil {
    public static <T> void assertThrows(Class<T> exceptionType, Runnable runnable) {
        assertThrows(exceptionType, null, runnable);
    }


    public static <T> void assertThrows(Class<T> exceptionType, String message, Runnable runnable) {
        try {
            runnable.run();
            fail();
        }
        catch(Throwable t) {
            assertTrue("Throwable expected: " + exceptionType.getName() + ", Actual: " + t.getClass().getName(), exceptionType.isInstance(t));

            if(message != null)
                assertTrue("Message expected: \"" + message + "\", Actual: \"" + t.getMessage() + "\"", t.getMessage().equals(message));
        }
    }

    
    public static Loop[] getLoops(Face face) {
        ArrayList<Loop> loops = new ArrayList<>(3);
        for(Loop loop : face.loops())
            loops.add(loop);
        return loops.toArray(new Loop[loops.size()]);
    }


    public static void assertFace(Face face, Vertex... vertices) {
        Loop[] loops = getLoops(face);
        assertThat(loops.length, is(vertices.length));

        int prevIndex = loops.length-1;
        for(int i=0; i<loops.length; ++i) {
            int nextIndex = (i+1) % loops.length;
            Loop loop = loops[i];
            System.out.println("loop " + loop.vertex);

            assertThat(loop.nextFaceLoop, is(loops[nextIndex]));
            assertThat(loop.prevFaceLoop, is(loops[prevIndex]));
            
            assertThat(loop.face, is(face));
            assertThat(loop.vertex, is(vertices[i]));
            assertTrue(loop.edge.connects(vertices[i], vertices[nextIndex]));
            assertThat(vertices[i].getEdgeTo(vertices[nextIndex]), is(loop.edge));
            assertThat(vertices[nextIndex].getEdgeTo(vertices[i]), is(loop.edge));

            prevIndex = i;
        }
    }
}
