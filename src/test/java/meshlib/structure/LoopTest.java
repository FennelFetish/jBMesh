package meshlib.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

public class LoopTest {
    @Test
    public void testCtor() {
        Loop loop = new Loop();

        assertNull(loop.edge);
        assertNull(loop.face);
        assertNull(loop.vertex);
        assertNull(loop.nextFaceLoop);

        assertEquals(loop, loop.nextEdgeLoop);
    }
}
