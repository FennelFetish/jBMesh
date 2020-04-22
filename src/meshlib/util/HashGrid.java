package meshlib.util;

import com.jme3.math.Vector3f;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HashGrid<T> {
    public static final float DEFAULT_CELLSIZE = 0.01f;

    private final float cellSize;
    private final Map<Long, T> grid = new HashMap<>();


    public HashGrid() {
        this(DEFAULT_CELLSIZE);
    }

    public HashGrid(float cellSize) {
        this.cellSize = cellSize;
    }


    public Index getIndexForCoords(Vector3f coords) {
        return getIndexForCoords(coords.x, coords.y, coords.z);
    }

    public Index getIndexForCoords(float x, float y, float z) {
        return new Index(x*cellSize, y*cellSize, z*cellSize);
    }


    public T get(Index cellPos) {
        return grid.get(cellPos.key);
    }


    public T getAndSet(Index cellPos, T value) {
        if(value == null)
            return grid.remove(cellPos.key);
        else {
            return grid.put(cellPos.key, value);
        }
    }


    public int size() {
        return grid.size();
    }


    public void getAll(List<T> dest) {
        dest.addAll(grid.values());
    }


    public void clear() {
        grid.clear();
    }



    public static final class Index {
        private static final long KEY_OFFSET = 1048576;
        private static final long KEY_MASK   = 0x1FFFFF;

        public final int x, y, z;
        private final long key;


        private Index(float x, float y, float z) {
            this((int) Math.ceil(x), (int) Math.ceil(y), (int) Math.ceil(z));
        }

        private Index(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.key = key(x, y, z);
        }


        public Index walk(int x, int y, int z) {
            return new Index(this.x+x, this.y+y, this.z+z);
        }


        // 21 bits per component (2^21-1 = 2097151 = KEY_MASK). min value: -1048576 (KEY_OFFSET), max value: 1048575
        // Operate on long integers, therefore use long in parameter list.
        private static long key(long x, long y, long z) {
            x = (x + KEY_OFFSET) & KEY_MASK;
            y = (y + KEY_OFFSET) & KEY_MASK;
            z = (z + KEY_OFFSET) & KEY_MASK;
            return x | (y << 21) | (z << 42);
        }


        @Override
        public String toString() {
            return String.format("HashGrid.Index[x:%d, y:%d, z:%d]", x, y, z);
        }
    }
}
