package meshlib.data;

import meshlib.data.property.FloatProperty;
import meshlib.data.property.IntTupleProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

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
        assertEquals(prop, data.getProperty(propName));
        assertNotNull(prop.data);

        assertThrows(IllegalStateException.class, () -> {
            data.addProperty(prop);
        });

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
            prop.setValues(elements[i], i, i, i);
        }

        assertEquals(13, data.size());

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

        assertEquals(6, data.size());

        data.compactData();

        assertEquals(6, data.size());
        assertEquals(6*3, prop.data.length);

        for(int i=0; i<elements.length; ++i) {
            if(!elements[i].isAlive())
                continue;

            assertEquals(i, prop.get(elements[i], 0));
            assertEquals(i, prop.get(elements[i], 1));
            assertEquals(i, prop.get(elements[i], 2));
        }
    }
}
