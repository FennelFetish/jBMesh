package meshlib;

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
}
