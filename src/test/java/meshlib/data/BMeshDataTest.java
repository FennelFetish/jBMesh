package meshlib.data;

import meshlib.data.property.FloatProperty;
import meshlib.data.property.IntTupleProperty;
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


    @Test
    public void testCompact() {
        BMeshData<TestElement> data = new BMeshData<>(() -> new TestElement());
        IntTupleProperty<TestElement> prop = new IntTupleProperty<>("Prop", 3);
        data.addProperty(prop);

        TestElement[] elements = new TestElement[13];
        for(int i=0; i<elements.length; ++i) {
            elements[i] = data.create();
            prop.set(elements[i], i, i, i);
        }

        assertThat(data.size(), is(13));

        data.destroy(elements[0]);
        // 1
        // 2
        data.destroy(elements[3]);
        // 4
        data.destroy(elements[5]);
        data.destroy(elements[6]);
        // 7
        // 8
        data.destroy(elements[9]);
        data.destroy(elements[10]);
        data.destroy(elements[11]);
        // 12

        assertThat(data.size(), is(6));

        data.compactData();

        assertThat(data.size(), is(6));
        assertThat(prop.data.length, is(6*3));

        for(int i=0; i<elements.length; ++i) {
            if(!elements[i].isAlive())
                continue;

            assertThat(prop.get(elements[i], 0), is(i));
            assertThat(prop.get(elements[i], 1), is(i));
            assertThat(prop.get(elements[i], 2), is(i));
        }
    }
}
