package meshlib.structure;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class LoopTest {
    @Test
    public void testCtor() {
        Loop loop = new Loop();

        assertNull(loop.edge);
        assertNull(loop.face);
        assertNull(loop.vertex);
        assertNull(loop.nextFaceLoop);

        assertThat(loop.nextEdgeLoop, is(loop));
    }
}
