// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.benchmarks;

import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;
import ch.alchemists.jbmesh.util.Profiler;

public class FloatBufferBenchmark {
    private static final long SEED = 12345678;

    private final int bytes;
    final int sparseSize;
    private final float[] data;
    private final int[] randomAccessPattern;


    public FloatBufferBenchmark(int bytes) {
        this.bytes = bytes;
        int size = bytes/4;
        sparseSize = size / 16;

        Random rnd = new Random(SEED);
        data = new float[size];
        for(int i=0; i<size; ++i) {
            data[i] = rnd.nextFloat();
        }

        // Inside-Out Fisher-Yates Shuffle for randomized permutations.
        randomAccessPattern = new int[size];
        for(int i=1; i<size; ++i) {
            int k = rnd.nextInt(i);
            randomAccessPattern[i] = randomAccessPattern[k];
            randomAccessPattern[k] = i;
        }

        /*System.out.println("randomAccessPattern: ");
        for(int i=0; i<size; ++i) {
            System.out.print(randomAccessPattern[i]);
            System.out.print(", ");

            if((i&31) == 31)
                System.out.println();
        }*/
    }


    public void heapBuffer() {
        FloatBuffer fb = ByteBuffer.allocate(bytes).asFloatBuffer();
        try(Profiler p = Profiler.start("Heap Float Buffer")) {
            buffer(fb);
            bufferMod(fb);
        }
    }

    public void directBuffer() {
        FloatBuffer fb = ByteBuffer.allocateDirect(bytes).asFloatBuffer();
        try(Profiler p = Profiler.start("Direct Float Buffer")) {
            buffer(fb);
            bufferMod(fb);
        }
    }

    public void directBufferNativeOrder() {
        FloatBuffer fb = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()).asFloatBuffer();
        try(Profiler p = Profiler.start("Direct Float Buffer, Native Order")) {
            buffer(fb);
            bufferMod(fb);
        }
    }

    public void jmeBuffer() {
        FloatBuffer fb = BufferUtils.createByteBuffer(bytes).asFloatBuffer(); // ReflectionAllocator
        try(Profiler p = Profiler.start("JME Float Buffer")) {
            buffer(fb);
            bufferMod(fb);
        }
    }


    private void buffer(FloatBuffer fb) {
        fb.rewind();
        try(Profiler p = Profiler.start("Loop Write Sequential")) {
            for(int i=0; i<data.length; ++i)
                fb.put(data[i]);
        }

        fb.rewind();
        try(Profiler p = Profiler.start("Loop Read Sequential")) {
            for(int i=0; i<data.length; ++i)
                data[i] = fb.get();
        }


        fb.rewind();
        try(Profiler p = Profiler.start("Loop Write Random")) {
            for(int i=0; i<data.length; ++i)
                fb.put(randomAccessPattern[i], data[i]);
        }

        fb.rewind();
        try(Profiler p = Profiler.start("Loop Read Random")) {
            for(int i=0; i<data.length; ++i)
                data[i] = fb.get(randomAccessPattern[i]);
        }


        fb.rewind();
        try(Profiler p = Profiler.start("Array Write")) {
            fb.put(data);
        }

        fb.rewind();
        try(Profiler p = Profiler.start("Array Read")) {
            fb.get(data);
        }
    }


    private void bufferMod(FloatBuffer fb) {
        fb.rewind();
        try(Profiler p = Profiler.start("Loop Read-Modify-Write Sequential")) {
            for(int i=0; i<data.length; ++i) {
                float f = fb.get(i);
                f += 1.0f;
                fb.put(i, f);
            }
        }

        fb.rewind();
        try(Profiler p = Profiler.start("Loop Read-Modify-Write Random")) {
            for(int i : randomAccessPattern) {
                float f = fb.get(i);
                f += 1.0f;
                fb.put(i, f);
            }
        }

        fb.rewind();
        try(Profiler p = Profiler.start("Loop Read-Modify-Write Random Sparse")) {
            for(int i=0; i<sparseSize; ++i) {
                float f = fb.get(randomAccessPattern[i]);
                f += 1.0f;
                fb.put(randomAccessPattern[i], f);
            }
        }

        fb.rewind();
        try(Profiler p = Profiler.start("Array Read-Modify-Write Sequential")) {
            float[] arr = BufferUtils.getFloatArray(fb);
            for(int i=0; i<data.length; ++i) {
                arr[i] += 1.0f;
            }
            fb.rewind();
            fb.put(arr);
        }

        fb.rewind();
        try(Profiler p = Profiler.start("Array Read-Modify-Write Random")) {
            float[] arr = BufferUtils.getFloatArray(fb);
            for(int i : randomAccessPattern) {
                arr[i] += 1.0f;
            }
            fb.rewind();
            fb.put(arr);
        }

        fb.rewind();
        try(Profiler p = Profiler.start("Array Read-Modify-Write Random Sparse")) {
            float[] arr = BufferUtils.getFloatArray(fb);
            for(int i=0; i<sparseSize; ++i) {
                arr[randomAccessPattern[i]] += 1.0f;
            }
            fb.rewind();
            fb.put(arr);
        }
    }


    private void array() {
        float[] dest = new float[data.length];

        try(Profiler p0 = Profiler.start("float[]")) {
            try(Profiler p = Profiler.start("Loop Write Sequential")) {
                for(int i=0; i<data.length; ++i)
                    dest[i] = data[i];
            }

            try(Profiler p = Profiler.start("Loop Read Sequential")) {
                for(int i=0; i<data.length; ++i)
                    data[i] = dest[i];
            }


            try(Profiler p = Profiler.start("Loop Write Random")) {
                for(int i=0; i<data.length; ++i)
                    dest[randomAccessPattern[i]] = data[i];
            }

            try(Profiler p = Profiler.start("Loop Read Random")) {
                for(int i=0; i<data.length; ++i)
                    data[i] = dest[randomAccessPattern[i]];
            }

            
            try(Profiler p = Profiler.start("System.arraycopy Write")) {
                System.arraycopy(data, 0, dest, 0, data.length);
            }

            try(Profiler p = Profiler.start("System.arraycopy Read")) {
                System.arraycopy(dest, 0, data, 0, data.length);
            }


            try(Profiler p = Profiler.start("Modify Sequential")) {
                for(int i=0; i<data.length; ++i) {
                    data[i] += 1.0f;
                }
            }

            try(Profiler p = Profiler.start("Modify Random")) {
                for(int i : randomAccessPattern) {
                    data[i] += 1.0f;
                }
            }

            try(Profiler p = Profiler.start("Modify Random Sparse")) {
                for(int i=0; i<sparseSize; ++i) {
                    data[randomAccessPattern[i]] += 1.0f;
                }
            }
        }
    }



    public static void main(String[] args) {
        int MB = 1024*1024;

        /*Profiler.setEnabled(false);
        runWithSize("Warmup 16KB", 1024*16);
        Profiler.setEnabled(true);*/

        try(Profiler p = Profiler.start("Total")) {
            runWithSize("64 bytes", 64);
            runWithSize("1 KB", 1024);
            runWithSize("16 KB", 1024*16);
            runWithSize("64 KB", 1024*64);
            runWithSize("512 KB", 1024*512);
            runWithSize("1 MB", MB);
            runWithSize("4 MB", MB * 4);
            runWithSize("16 MB", MB * 16);
        }
    }

    
    private static void runWithSize(String name, int size) {
        FloatBufferBenchmark bench = new FloatBufferBenchmark(size);

        try(Profiler p = Profiler.start(name)) {
            for(int i=0; i<10; ++i) {
                bench.heapBuffer();
                bench.directBuffer();
                bench.directBufferNativeOrder();
                bench.jmeBuffer();
                bench.array();
                System.gc();
            }
        }
    }
}
