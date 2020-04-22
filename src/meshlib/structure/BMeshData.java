package meshlib.structure;

import com.jme3.math.Vector3f;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BMeshData<T> {
    // Avoid adding public methods to elements
    interface ElementAccessor<T> {
        T create();
        void release(T element);
        int getIndex(T element);
        void setIndex(T element, int index);
    }


    private static interface DataAllocator {
        Object alloc(int size);
    }

    public static enum PropertyType {
        Float(s -> new float[s]),
        Double(s -> new double[s]),
        Integer(s -> new int[s]),
        Long(s -> new long[s]),
        Boolean(s -> new boolean[s]),
        String(s -> new String[s]);

        private final DataAllocator allocator;

        private PropertyType(DataAllocator allocator) {
            this.allocator = allocator;
        }
    }


    // Doesn't hold reference to data itself to prevent users of keeping references around
    public final class Property {
        public final String name;
        public final int numComponents;
        public final PropertyType type;

        private int index;

        private Property(String name, PropertyType type, int numComponents) {
            this.name = name;
            this.type = type;
            this.numComponents = numComponents;
        }

        public void getVec3(T element, Vector3f store) {
            float[] arr = (float[]) data.get(index);
            int eleIndex = accessor.getIndex(element) * numComponents;
            store.x = arr[eleIndex];
            store.y = arr[eleIndex+1];
            store.z = arr[eleIndex+2];
        }

        public void setVec3(T element, Vector3f vec) {
            setVec3(element, vec.x, vec.y, vec.z);
        }

        public void setVec3(T element, float x, float y, float z) {
            float[] arr = (float[]) data.get(index);
            int eleIndex = accessor.getIndex(element) * numComponents;
            arr[eleIndex]   = x;
            arr[eleIndex+1] = y;
            arr[eleIndex+2] = z;
        }


        public float getFloat(T element) {
            return ((float[])data.get(index))[accessor.getIndex(element)];
        }

        public void setFloat(T element, float f) {
            ((float[])data.get(index))[accessor.getIndex(element)] = f;
        }
    }


    private final ElementAccessor<T> accessor;

    private final ArrayList<T> elements = new ArrayList<>();
    private final List<T> readonlyElements = Collections.unmodifiableList(elements);
    private final Deque<Integer> freeList = new ArrayDeque<>();

    private static final int INITIAL_ARRAY_SIZE = 32;
    private static final float GROW_FACTOR = 1.25f;
    private int currentArraySize = INITIAL_ARRAY_SIZE;
    private int numElementsAlive = 0;
    
    private final Map<String, Property> properties = new HashMap<>();
    private final ArrayList<Object> data = new ArrayList<>(4);

    private final String name;


    BMeshData(String name, ElementAccessor<T> accessor) {
        this.name = name;
        this.accessor = accessor;
    }


    public List<T> elements() {
        return readonlyElements;
    }


    T add() {
        Integer index = freeList.poll();
        if(index != null) {
            T element = elements.get(index);
            accessor.setIndex(element, index);
            return element;
        }

        final int newIndex = elements.size();
        System.out.println(name + ": added element " + newIndex);
        if(newIndex >= currentArraySize) {
            int capacity = (int) Math.ceil(currentArraySize * GROW_FACTOR);
            ensureCapacity(capacity);
        }

        T element = accessor.create();
        accessor.setIndex(element, newIndex);
        elements.add(element);

        numElementsAlive++;
        return element;
    }

    void remove(int index) {
        T element = elements.get(index);
        accessor.release(element);

        freeList.add(index);
        numElementsAlive--;
    }

    void remove(T element) {
        accessor.release(element);

        freeList.add(accessor.getIndex(element));
        numElementsAlive--;
    }


    public Property createProperty(String name, PropertyType type, int numComponents) {
        if(properties.containsKey(name))
            return null;

        Property prop = new Property(name, type, numComponents);
        properties.put(name, prop);
        prop.index = data.size();
        System.out.println("alloc '" + name + "': " + (currentArraySize * numComponents));
        data.add(type.allocator.alloc(currentArraySize * numComponents));
        return prop;
    }

    public Property getProperty(String name) {
        return properties.get(name);
    }

    public void removeProperty(Property property) {
        properties.remove(property.name);
        data.remove(property.index);

        for(Property prop : properties.values()) {
            if(prop.index > property.index)
                prop.index--;
        }

        property.index = -1;
    }


    public void ensureCapacity(int capacity) {
        System.out.println("grow '" + name + "' from " + currentArraySize + " to " + capacity);

        for(Property prop : properties.values()) {
            System.out.println("alloc grow '" + prop.name + "': " + (capacity * prop.numComponents));
            Object destArray = prop.type.allocator.alloc(capacity * prop.numComponents);
            System.arraycopy(data.get(prop.index), 0, destArray, 0, currentArraySize * prop.numComponents);
            data.set(prop.index, destArray);
        }

        currentArraySize = capacity;
    }


    public void compact() {
        if(freeList.isEmpty())
            return;

        int[] free = new int[freeList.size()];
        for(int i=0; !freeList.isEmpty(); ++i)
            free[i] = freeList.poll();
        
        Arrays.sort(free);
        compact(free);
        
        // Remove dead elements (index = -1).
        // TODO: Can be optimized since we have free list
        for(Iterator<T> it = elements.iterator(); it.hasNext(); ) {
            T ele = it.next();
            if(accessor.getIndex(ele) < 0)
                it.remove();
        }

        freeList.clear();
        currentArraySize = numElementsAlive;
    }


    private void compact(int[] free) {
        int shift = 0;

        for(int f=0; f<free.length; ++f) {
            shift++;

            int copyLastIndex;
            if(f+1 >= free.length)
                copyLastIndex = currentArraySize-1;
            else if(free[f+1] == free[f]+1)
                continue;
            else
                copyLastIndex = free[f+1] - 1;

            int copyFirstIndex = free[f] + 1;

            for(Property property : properties.values())
                compact(property, copyFirstIndex, copyLastIndex, shift);

            for(int i=copyFirstIndex; i<=copyLastIndex; ++i)
                accessor.setIndex(elements.get(i), i-shift);
        }
    }


    private void compact(Property property, int copyFirstIndex, int copyLastIndex, int shift) {
        final int newSize = numElementsAlive * property.numComponents;
        System.out.println("alloc compact '" + property.name + "': " + newSize);
        Object destArray = property.type.allocator.alloc(newSize);

        int copyStartIndex = copyFirstIndex * property.numComponents;
        int copyLength = (copyLastIndex-copyFirstIndex + 1) * property.numComponents;
        System.arraycopy(data.get(property.index), copyStartIndex, destArray, copyStartIndex-shift, copyLength);
        data.set(property.index, destArray);
    }
}
