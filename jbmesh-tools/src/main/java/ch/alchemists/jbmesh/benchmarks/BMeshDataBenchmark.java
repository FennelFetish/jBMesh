package ch.alchemists.jbmesh.benchmarks;

import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.util.Profiler;
import java.util.ArrayList;
import java.util.Random;

public class BMeshDataBenchmark {
    private static class BenchElement extends Element {
        @Override
        protected void releaseElement() {}
    }


    private static final long SEED = 76543;
    private static final float DESTROY_AMOUNT = 0.5f;

    private final int numElements;
    private final int numDestroy;

    private final ArrayList<BenchElement> elements = new ArrayList<>();
    private final int[] randomAccessPattern;


    private BMeshDataBenchmark(int numElements) {
        this.numElements = numElements;
        this.numDestroy = (int) Math.ceil(numElements * DESTROY_AMOUNT);

        System.out.println("numDestroy: " + numDestroy);
        elements.ensureCapacity(numElements);

        Random rnd = new Random(SEED);

        // Inside-Out Fisher-Yates Shuffle for randomized permutations.
        randomAccessPattern = new int[numElements];
        for(int i=1; i<numElements; ++i) {
            int k = rnd.nextInt(i);
            randomAccessPattern[i] = randomAccessPattern[k];
            randomAccessPattern[k] = i;
        }
    }



    private void run() {
        BMeshData<BenchElement> data = new BMeshData<>(BenchElement::new);

        // Create all elements
        try(Profiler p = Profiler.start("Create")) {
            for(int i=0; i<numElements; ++i) {
                elements.add(data.create());
            }
        }

        // Destroy elements in random pattern
        try(Profiler p = Profiler.start("Destroy")) {
            for(int i=0; i<numDestroy; ++i) {
                BenchElement ele = elements.get(randomAccessPattern[i]);
                elements.set(randomAccessPattern[i], null);
                data.destroy(ele);
            }
        }

        // Recreate elements
        try(Profiler p = Profiler.start("Recreate")) {
            for(int i=0; i<numDestroy; ++i) {
                assert elements.get(randomAccessPattern[i]) == null;
                elements.set(randomAccessPattern[i], data.create());
            }
        }

        // Destroy elements in random pattern
        try(Profiler p = Profiler.start("Destroy 2")) {
            for(int i=numDestroy; i<randomAccessPattern.length; ++i) {
                BenchElement ele = elements.get(randomAccessPattern[i]);
                data.destroy(ele);
            }
        }
        elements.clear();

        try(Profiler p = Profiler.start("Compact")) {
            data.compactData();
        }
    }


    private static void run(BMeshDataBenchmark[] benches, int rounds) {
        for(int i=0; i<rounds; ++i) {
            for(int b=0; b<benches.length; ++b) {
                try(Profiler p = Profiler.start("Elements: " + benches[b].numElements)) {
                    benches[b].run();
                }
            }
            System.gc();
        }
    }


    public static void main(String[] args) {
        BMeshDataBenchmark[] benches = new BMeshDataBenchmark[] {
            new BMeshDataBenchmark(100),
            new BMeshDataBenchmark(1000),
            new BMeshDataBenchmark(10000),
            new BMeshDataBenchmark(100000),
            //new BMeshDataBenchmark(500000),
        };

        Profiler.setEnabled(false);
        try(Profiler p0 = Profiler.start("Warmup")) {
            run(benches, 200);
        }
        Profiler.setEnabled(true);

        try(Profiler p0 = Profiler.start("Total")) {
            run(benches, 2000);
        }
    }
}
