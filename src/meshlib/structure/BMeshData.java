package meshlib.structure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
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


    public static enum PropertyType {
        Float, Double, Integer, Long, Boolean
    }


    public final class Property {
        public final String name;
        public final int numComponents;
        public final PropertyType type;

        private int index;
        //private Object data;

        private Property(String name, PropertyType type, int numComponents) {
            this.name = name;
            this.type = type;
            this.numComponents = numComponents;
        }

        public float getFloat(T element) {
            return ((float[])data.get(index))[accessor.getIndex(element)];
        }
    }


    private final ElementAccessor<T> accessor;

    private final ArrayList<T> elements = new ArrayList<>();
    private final List<T> readonlyElements = Collections.unmodifiableList(elements);

    private int numElementsAlive = 0;
    private int currentArraySize = 0;
    
    private final Map<String, Property> properties = new HashMap<>();
    private final List<Object> data = new ArrayList<>(4);

    private final Deque<Integer> freeList = new ArrayDeque<>();


    BMeshData(ElementAccessor<T> accessor) {
        this.accessor = accessor;
    }


    public List<T> elements() {
        return readonlyElements;
    }


    /**
     * @return Index
     */
    public T add() {
        Integer index = freeList.poll();
        if(index != null) {
            T element = elements.get(index);
            accessor.setIndex(element, index);
            return element;
        }

        T element = accessor.create();
        accessor.setIndex(element, elements.size());
        elements.add(element);

        // TODO: Resize data arrays
        numElementsAlive++;
        return element;
    }

    
    public void remove(int index) {
        T element = elements.get(index);
        accessor.release(element);

        freeList.add(index);
        numElementsAlive--;
    }


    public Property createProperty(String name, PropertyType type, int numComponents) {
        if(properties.containsKey(name))
            return null;

        Property prop = new Property(name, type, numComponents);
        properties.put(name, prop);
        // TODO: create array
        return prop;
    }

    public Property getProperty(String name) {
        return properties.get(name);
    }

    public void removeProperty(Property property) {
        properties.remove(property.name);
    }


    public void compact() {
        if(freeList.isEmpty())
            return;

        int[] free = new int[freeList.size()];
        for(int i=0; !freeList.isEmpty(); ++i)
            free[i] = freeList.poll();
        Arrays.sort(free);

        /*for(Property property : properties.values())
            compact(property, free[]);*/
        
        // TODO: Remove dead elements
        freeList.clear();

        currentArraySize = numElementsAlive;
    }


    /*private void compact(Property property. int[] free) {
        final int newSize = numElementsAlive * property.numComponents;
        Object destArray;
        switch(property.type) {
            case Float:
                destArray = new float[newSize];
                break;
        }

        float[] positionDest = new float[numElementsAlive * 3];
        compact(data.get(property.index), positionDest, currentArraySize*property.numComponents, property.numComponents, free);
        position = positionDest;
    }*/


    /*private void compact(int srcLength, int components, int[] free, CopyFunctor copyFunc) {
        int shift = 0;
        
        for(int f=0; f<free.length; ++f) {
            shift += components;
            
            int lastToMove;
            if(f+1 >= free.length)
                lastToMove = srcLength-1;
            else if(free[f+1] == free[f]+1)
                continue;
            else
                lastToMove = (free[f+1] * components) - 1;

            int firstToMove = (free[f]+1) * components;
            copyFunc.copy(firstToMove, lastToMove-firstToMove, shift);
        }
    }*/


    private void compact(Object srcArray, Object destArray, int srcLength, int components, int[] free) {
        final int srcNumElements = srcLength/components;
        int shift = 0;

        for(int f=0; f<free.length; ++f) {
            shift++;

            int lastToMove;
            if(f+1 >= free.length)
                lastToMove = srcNumElements-1;
            else if(free[f+1] == free[f]+1)
                continue;
            else
                lastToMove = free[f+1] - 1;

            int firstToMove = free[f] + 1;
            int copyStartIndex = firstToMove * components;
            int copyLength = (lastToMove-firstToMove + 1) * components;
            System.arraycopy(srcArray, copyStartIndex, destArray, copyStartIndex-shift, copyLength);

            for(int i=firstToMove; i<=lastToMove; ++i)
                accessor.setIndex(elements.get(i), i-shift);
        }
    }


    /*private void compact(Property property, int copyStartIndex, int shift, int copyLength) {

    }*/
}
