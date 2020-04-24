package meshlib.data;

import meshlib.data.property.FloatProperty;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class BMeshDataTest {
    private static class TestElement extends Element {
        @Override
        protected void releaseElement() {}
    }
    

    @Test
    public void testPropertyAddRemove() {
        final String propName = "Prop";

        BMeshData<TestElement> data = new BMeshData<>(() -> new TestElement());
        assertNull(data.getProperty(propName));

        FloatProperty<TestElement> prop = new FloatProperty<>(propName);
        assertNull(prop.data);

        data.addProperty(prop);
        assertThat(data.getProperty(propName), is(prop));
        assertNotNull(prop.data);

        try {
            data.addProperty(prop);
            assert false;
        }
        catch(IllegalStateException ex) {}
        catch(Exception ex) { assert false; }

        data.removeProperty(prop);
        assertNull(prop.data);
        assertNull(data.getProperty(propName));

        data.addProperty(prop);
        assertNotNull(prop.data);
    }
}
