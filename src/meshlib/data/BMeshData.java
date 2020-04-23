package meshlib.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BMeshData<T extends Element> {
    public static interface ElementFactory<T> {
        T createElement();
    }


    private final ElementFactory<T> factory;
    private final ArrayList<T> elements = new ArrayList<>();
    private final List<T> readonlyElements = Collections.unmodifiableList(elements);
    private final Deque<Integer> freeList = new ArrayDeque<>();

    private static final int INITIAL_ARRAY_SIZE = 32;
    private static final float GROW_FACTOR = 1.25f;
    private int arraySize = INITIAL_ARRAY_SIZE;
    private int numElementsAlive = 0;
    
    private final Map<String, BMeshProperty<?, T>> properties = new HashMap<>();

    private final String name; // Debug


    public BMeshData(String name, ElementFactory<T> factory) {
        this.name = name;
        this.factory = factory;
    }


    public List<T> elements() {
        return readonlyElements;
    }


    public T add() {
        if(!freeList.isEmpty()) {
            int index = freeList.poll();
            T element = elements.get(index);
            element.setIndex(index);
            return element;
        }

        final int newIndex = elements.size();
        System.out.println(name + ": added element " + newIndex);
        if(newIndex >= arraySize) {
            int capacity = (int) Math.ceil(arraySize * GROW_FACTOR);
            ensureCapacity(capacity);
        }

        T element = factory.createElement();
        element.setIndex(newIndex);
        elements.add(element);

        numElementsAlive++;
        return element;
    }

    public void remove(T element) {
        element.release();

        freeList.add(element.getIndex());
        numElementsAlive--;
    }


    public void addProperty(BMeshProperty<?, T> property) {
        if(properties.containsKey(property.name))
            throw new IllegalStateException("Property '" + property.name + "' already exists");

        if(property.data != null)
            throw new IllegalStateException("Property '" + property.name + "' already associated with another data set");

        Object oldArray = property.allocReplace(arraySize);
        assert oldArray == null;
        properties.put(property.name, property);
    }

    // getProperty(name, Vec3Property.class) should return Vec3Property<T> ??
    BMeshProperty<?, T> getProperty(String name) {
        return properties.get(name);
    }

    public void removeProperty(BMeshProperty<?, T> property) {
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
        System.out.println("resize '" + name + "' from " + arraySize + " to " + size);

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

        int[] free = new int[freeList.size()];
        for(int i=0; !freeList.isEmpty(); ++i)
            free[i] = freeList.poll();
        
        Arrays.sort(free);
        compact(free);
        
        // Remove dead elements (index = -1).
        // TODO: Can be optimized since we have free list
        for(Iterator<T> it = elements.iterator(); it.hasNext(); ) {
            T ele = it.next();
            if(ele.getIndex() < 0)
                it.remove();
        }

        freeList.clear();
        arraySize = numElementsAlive;

        elements.trimToSize();
    }


    private void compact(int[] free) {
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
    }


    private static final class CompactOp {
        private int firstIndex;
        private int lastIndex;
        private int shift;

        public void compact(BMeshProperty property, Object oldArray) {
            int copyStartIndex = firstIndex * property.numComponents;
            int copyLength = (lastIndex-firstIndex + 1) * property.numComponents;
            System.arraycopy(oldArray, copyStartIndex, property.data, copyStartIndex-shift, copyLength);
        }
    }
}
