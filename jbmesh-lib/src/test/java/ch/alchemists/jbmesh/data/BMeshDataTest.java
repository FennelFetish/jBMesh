package ch.alchemists.jbmesh.data;

import ch.alchemists.jbmesh.data.property.FloatAttribute;
import ch.alchemists.jbmesh.data.property.IntTupleAttribute;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class BMeshDataTest {
    private static class TestElement extends Element {
        @Override
        protected void releaseElement() {}
    }


    @Test
    public void testElementReference() {
        BMeshData<TestElement> data = new BMeshData<>(TestElement::new);

        TestElement e1 = data.create();
        data.destroy(e1);
        TestElement e2 = data.create();

        assertFalse(e1.isAlive());
        assertNotEquals(e1, e2);

        data.destroy(e1);
        assertTrue(e2.isAlive());
    }
    

    @Test
    public void testAttributeAddRemove() {
        final String attrName = "Attr";

        BMeshData<TestElement> data = new BMeshData<>(TestElement::new);
        assertNull(data.getAttribute(attrName));

        FloatAttribute<TestElement> attr = new FloatAttribute<>(attrName);
        assertNull(attr.data);

        data.addAttribute(attr);
        assertEquals(attr, data.getAttribute(attrName));
        assertNotNull(attr.data);

        assertThrows(IllegalStateException.class, () -> {
            data.addAttribute(attr);
        });

        data.removeAttribute(attr);
        assertNull(attr.data);
        assertNull(data.getAttribute(attrName));

        data.addAttribute(attr);
        assertNotNull(attr.data);
    }


    @Test
    public void testCompact() {
        BMeshData<TestElement> data = new BMeshData<>(TestElement::new);
        IntTupleAttribute<TestElement> attr = new IntTupleAttribute<>("Attr", 3);
        data.addAttribute(attr);

        TestElement[] elements = new TestElement[13];
        for(int i=0; i<elements.length; ++i) {
            elements[i] = data.create();
            attr.setValues(elements[i], i, i, i);
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
        assertEquals(6*3, attr.data.length);

        assertValues(attr, elements);
    }


    /**
     * Tests compacting/copying of values up to the first free slot.
     */
    @Test
    public void testCompactFirstSegment() {
        BMeshData<TestElement> data = new BMeshData<>(TestElement::new);
        IntTupleAttribute<TestElement> attr = new IntTupleAttribute<>("Attr", 3);
        data.addAttribute(attr);

        TestElement[] elements = new TestElement[6];
        for(int i=0; i<elements.length; ++i) {
            elements[i] = data.create();
            attr.setValues(elements[i], i, i, i);
        }

        assertEquals(6, data.size());
        data.destroy(elements[2]);
        assertEquals(5, data.size());
        data.compactData();

        assertValues(attr, elements);
    }


    private void assertValues(IntTupleAttribute<TestElement> attr, TestElement[] elements) {
        for(int i=0; i<elements.length; ++i) {
            if(!elements[i].isAlive())
                continue;

            assertEquals(i, attr.get(elements[i], 0));
            assertEquals(i, attr.get(elements[i], 1));
            assertEquals(i, attr.get(elements[i], 2));
        }
    }
}
