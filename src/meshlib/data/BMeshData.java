package meshlib.data;

import java.nio.Buffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BMeshData<E extends Element> {
    public static interface ElementFactory<T> {
        T createElement();
    }


    private final ElementFactory<E> factory;
    private final ArrayList<E> elements = new ArrayList<>();
    private final List<E> readonlyElements = Collections.unmodifiableList(elements);
    private final Deque<Integer> freeList = new ArrayDeque<>(); // PriorityQueue?

    private static final int INITIAL_ARRAY_SIZE = 32;
    private static final float GROW_FACTOR = 1.5f;
    private int arraySize = INITIAL_ARRAY_SIZE;
    private int numElementsAlive = 0;
    
    private final Map<String, BMeshProperty<E, ?>> properties = new HashMap<>();


    public BMeshData(ElementFactory<E> factory) {
        this.factory = factory;
    }


    public List<E> elements() {
        return readonlyElements;
    }


    public E create() {
        if(!freeList.isEmpty()) {
            int index = freeList.poll();
            E element = elements.get(index);
            element.setIndex(index);
            return element;
        }

        final int newIndex = elements.size();
        if(newIndex >= arraySize) {
            int capacity = (int) Math.ceil(arraySize * GROW_FACTOR);
            ensureCapacity(capacity);
        }

        E element = factory.createElement();
        element.setIndex(newIndex);
        elements.add(element);

        numElementsAlive++;
        return element;
    }

    public void destroy(E element) {
        element.release();

        freeList.add(element.getIndex());
        numElementsAlive--;
    }


    public void addProperty(BMeshProperty<E, ?> property) {
        if(properties.containsKey(property.name))
            throw new IllegalStateException("Property '" + property.name + "' already exists");

        if(property.data != null)
            throw new IllegalStateException("Property '" + property.name + "' already associated with another data set");

        Object oldArray = property.allocReplace(arraySize);
        assert oldArray == null;
        properties.put(property.name, property);
    }

    // getProperty(name, Vec3Property.class) should return Vec3Property<T> ??
    BMeshProperty<E, ?> getProperty(String name) {
        return properties.get(name);
    }

    public void removeProperty(BMeshProperty<E, ?> property) {
        property.release();
        properties.remove(property.name);
    }


    public void ensureCapacity(int minCapacity) {
        minCapacity -= freeList.size();
        if(arraySize < minCapacity)
            resize(minCapacity, arraySize);
    }

    public void reserve(int count) {
        ensureCapacity(numElementsAlive + count);
    }


    private void resize(int size, int copyLength) {
        System.out.println("resize from " + arraySize + " to " + size);

        for(BMeshProperty prop : properties.values()) {
            prop.realloc(size, copyLength);
        }

        arraySize = size;
    }


    public void compact() {
        if(arraySize == numElementsAlive)
            return;

        if(freeList.isEmpty()) {
            resize(numElementsAlive, numElementsAlive);
            return;
        }

        List<CompactOp> compactOps = buildCompactOps();
        for(BMeshProperty property : properties.values()) {
            Object oldArray = property.allocReplace(numElementsAlive);
            for(CompactOp op : compactOps)
                op.copy(property.numComponents, oldArray, property.data);
        }

        for(CompactOp op : compactOps) {
            for(int i=op.firstIndex; i<=op.lastIndex; ++i)
                elements.get(i).setIndex(i-op.shift);
        }

        // Remove dead elements (index = -1).
        // TODO: Can be optimized since we have free list
        for(Iterator<E> it = elements.iterator(); it.hasNext(); ) {
            E ele = it.next();
            if(ele.getIndex() < 0)
                it.remove();
        }

        elements.trimToSize();
        freeList.clear();
        arraySize = numElementsAlive;
    }


    public void compactWithoutResize() {
        // ...

        // Or make a compact() and trim() method?
        // compact: Process free list
        // trim: shrink arrays
    }


    private List<CompactOp> buildCompactOps() {
        int[] free = new int[freeList.size()];
        for(int i=0; !freeList.isEmpty(); ++i)
            free[i] = freeList.poll();
        Arrays.sort(free);

        List<CompactOp> ops = new ArrayList<>(free.length);

        int shift = 0;
        for(int f=0; f<free.length; ++f) {
            shift++;

            int copyLastIndex;
            if(f+1 >= free.length)
                copyLastIndex = arraySize-1;
            else if(free[f+1] == free[f]+1)
                continue;
            else
                copyLastIndex = free[f+1] - 1;

            CompactOp op = new CompactOp();
            op.firstIndex = free[f] + 1;
            op.lastIndex = copyLastIndex;
            op.shift = shift;
            ops.add(op);
        }

        return ops;
    }


    /*private void compact(int[] free) {
        List<CompactOp> ops = new ArrayList<>(free.length);

        int shift = 0;
        for(int f=0; f<free.length; ++f) {
            shift++;

            int copyLastIndex;
            if(f+1 >= free.length)
                copyLastIndex = arraySize-1;
            else if(free[f+1] == free[f]+1)
                continue;
            else
                copyLastIndex = free[f+1] - 1;

            CompactOp op = new CompactOp();
            op.firstIndex = free[f] + 1;
            op.lastIndex = copyLastIndex;
            op.shift = shift;
            ops.add(op);

            for(int i=op.firstIndex; i<=copyLastIndex; ++i)
                elements.get(i).setIndex(i-shift);
        }

        for(BMeshProperty property : properties.values()) {
            Object oldArray = property.allocReplace(numElementsAlive);
            for(CompactOp op : ops) {
                op.compact(property, oldArray);
            }
        }
    }*/


    private static final class CompactOp {
        private int firstIndex;
        private int lastIndex;
        private int shift;

        public void copy(int numComponents, Object srcArray, Object destArray) {
            int copyStartIndex = firstIndex * numComponents;
            int copyLength = (lastIndex-firstIndex + 1) * numComponents;
            System.arraycopy(srcArray, copyStartIndex, destArray, copyStartIndex-shift, copyLength);
        }
    }


    public <TArray> TArray getCompactData(BMeshProperty<E, TArray> property) {
        final int size = numElementsAlive * property.numComponents;
        TArray array = property.alloc(size);

        if(arraySize == numElementsAlive || freeList.isEmpty()) {
            System.arraycopy(property.data, 0, array, 0, size);
            return array;
        }

        List<CompactOp> compactOps = buildCompactOps();
        for(CompactOp op : compactOps) {
            op.copy(property.numComponents, property.data, array);
        }

        return array;
    }


    public <TArray> void putCompactData(BMeshProperty<E, TArray> property, Buffer buffer) {
        buffer.reset();
        // ...
    }


    public void sort(Comparator<E> comparator) {
        // Sort backing arrays
        // For optimizing OpenGL performance? Does this matter?
        // e.g. sort vertices by face for better cache utilisation, sort loops by face

        // Also provide back-to-front sorting for indices
    }


    public void equals(E element1, E element2) {
        // Compare all properties
    }
}
