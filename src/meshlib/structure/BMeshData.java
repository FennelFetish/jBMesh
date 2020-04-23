package meshlib.structure;

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
    interface ElementFactory<T> {
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
    
    private final Map<String, BMeshProperty<?>> properties = new HashMap<>();

    private final String name; // Debug


    BMeshData(String name, ElementFactory<T> factory) {
        this.name = name;
        this.factory = factory;
    }


    public List<T> elements() {
        return readonlyElements;
    }


    T add() {
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

    void remove(int index) {
        T element = elements.get(index);
        element.release();

        freeList.add(index);
        numElementsAlive--;
    }

    void remove(T element) {
        element.release();

        freeList.add(element.getIndex());
        numElementsAlive--;
    }


    public BMeshProperty createProperty(String name, BMeshProperty.Type type, int numComponents) {
        if(properties.containsKey(name))
            return null;

        if(numComponents < 1)
            throw new IllegalArgumentException("Number of components cannot be less than 1");

        BMeshProperty prop = new BMeshProperty(name, type, numComponents);
        System.out.println("alloc '" + name + "': " + (arraySize * numComponents));
        prop.data = type.allocator.alloc(arraySize * numComponents);
        properties.put(name, prop);
        return prop;
    }

    public BMeshProperty getProperty(String name) {
        return properties.get(name);
    }

    public void removeProperty(BMeshProperty property) {
        property.data = null;
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
            System.out.println("alloc resize '" + prop.name + "': " + (size * prop.numComponents));
            Object destArray = prop.type.allocator.alloc(size * prop.numComponents);
            System.arraycopy(prop.data, 0, destArray, 0, copyLength * prop.numComponents);
            prop.data = destArray;
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

            int copyFirstIndex = free[f] + 1;

            for(BMeshProperty property : properties.values())
                compact(property, copyFirstIndex, copyLastIndex, shift);

            for(int i=copyFirstIndex; i<=copyLastIndex; ++i)
                elements.get(i).setIndex(i-shift);
        }
    }


    private void compact(BMeshProperty property, int copyFirstIndex, int copyLastIndex, int shift) {
        final int newSize = numElementsAlive * property.numComponents;
        System.out.println("alloc compact '" + property.name + "': " + newSize);
        
        int copyStartIndex = copyFirstIndex * property.numComponents;
        int copyLength = (copyLastIndex-copyFirstIndex + 1) * property.numComponents;

        Object destArray = property.type.allocator.alloc(newSize);
        System.arraycopy(property.data, copyStartIndex, destArray, copyStartIndex-shift, copyLength);
        property.data = destArray;
    }
}
