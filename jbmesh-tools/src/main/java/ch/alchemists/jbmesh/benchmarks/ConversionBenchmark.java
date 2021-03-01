package ch.alchemists.jbmesh.benchmarks;

import ch.alchemists.jbmesh.conversion.Import;
import ch.alchemists.jbmesh.util.Profiler;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Torus;

public class ConversionBenchmark {
/*
===== Profiler =====                                   Total [ms]    % of Parent  Avg [ms]    Min [ms]    Max [ms]    Runs
Torus 16x16                                            229.30790000         %     2.29307900  1.20560000  25.4456000  100
·   Grid                                               44.259300000  19.3012%     0.44259300  0.22190000  12.6654000  100
·   Optimized Grid                                     28.326500000  12.3530%     0.28326500  0.18400000  2.64480000  100
·   Grid Mapped                                        29.507100000  12.8678%     0.29507100  0.17930000  1.97400000  100
·   Optimized Grid Mapped                              26.777200000  11.6773%     0.26777200  0.14270000  1.37690000  100
·   Sort Mapped                                        76.675100000  33.4376%     0.76675100  0.33080000  6.16710000  100
·   Exact Hash Mapped                                  23.106300000  10.0765%     0.23106300  0.11560000  1.43400000  100

Torus 64x64                                            2067.4022000         %     20.6740220  19.0316000  31.1309000  100
·   Grid                                               382.31739999  18.4926%     3.82317400  3.52820000  6.13470000  100
·   Optimized Grid                                     309.51650000  14.9712%     3.09516500  2.79320000  5.39000000  100
·   Grid Mapped                                        331.25630000  16.0228%     3.31256300  3.01380000  6.44450000  100
·   Optimized Grid Mapped                              256.85460000  12.4240%     2.56854600  2.34810000  4.66270000  100
·   Sort Mapped                                        588.34190000  28.4580%     5.88341900  5.45610000  8.86880000  100
·   Exact Hash Mapped                                  198.54270000  9.60348%     1.98542700  1.78820000  3.08010000  100

Torus 128x128                                          8238.7853999         %     82.3878539  79.8168999  96.8562000  100
·   Grid                                               1584.7440000  19.2351%     15.8474400  15.3027000  20.0108000  100
·   Optimized Grid                                     1204.0901999  14.6148%     12.0409020  11.6154000  15.3907000  100
·   Grid Mapped                                        1332.2232999  16.1701%     13.3222330  12.8450000  22.3631000  100
·   Optimized Grid Mapped                              1028.1976000  12.4799%     10.2819760  9.90140000  13.4042000  100
·   Sort Mapped                                        2305.0747000  27.9783%     23.0507470  22.1521000  29.8180000  100
·   Exact Hash Mapped                                  783.21179999  9.50639%     7.83211800  7.54970000  12.3158000  100

Torus 160x160                                          20358.247100         %     203.582471  136.582000  282.996700  100
·   Grid                                               3211.7744000  15.7762%     32.1177440  24.6324000  119.356000  100
·   Optimized Grid                                     3495.7453000  17.1711%     34.9574530  20.4554000  115.363599  100
·   Grid Mapped                                        2796.0703000  13.7343%     27.9607030  21.3560000  104.265999  100
·   Optimized Grid Mapped                              3231.0701000  15.8710%     32.3107010  17.5617000  105.514800  100
·   Sort Mapped                                        5226.5238000  25.6727%     52.2652380  38.7871000  131.975000  100
·   Exact Hash Mapped                                  2395.3730000  11.7661%     23.9537300  12.2803000  123.393999  100
*/



    private static void run(String name, Mesh in) {
        try(Profiler p0 = Profiler.start(name)) {
            /*try(Profiler p = Profiler.start("Simple")) {
                MeshConverter.convertSimple(in);
            }*/

            try(Profiler p = Profiler.start("Grid")) {
                Import.convertGrid(in);
            }

            try(Profiler p = Profiler.start("Optimized Grid")) {
                Import.convertOptimizedGrid(in);
            }

            /*try(Profiler p = Profiler.start("Simple Mapped")) {
                MeshConverter.convertSimpleMapped(in);
            }*/

            try(Profiler p = Profiler.start("Grid Mapped")) {
                Import.convertGridMapped(in);
            }

            try(Profiler p = Profiler.start("Optimized Grid Mapped")) {
                Import.convertOptimizedGridMapped(in);
            }

            try(Profiler p = Profiler.start("Sort Mapped")) {
                Import.convertSortMapped(in);
            }

            try(Profiler p = Profiler.start("Exact Hash Mapped")) {
                Import.convertExactMapped(in);
            }
        }
    }


    public static void main(String[] args) {
        // Warmup
        Mesh in = new Torus(128, 128, 2.0f, 4.0f);
        for(int i=0; i<10; ++i) {
            run("Warmup", in);
        }


        final int runs = 100;

        in = new Torus(16, 16, 2.0f, 4.0f);
        for(int i=0; i<runs; ++i) {
            run("Torus 16x16", in);
        }

        in = new Torus(64, 64, 2.0f, 4.0f);
        for(int i=0; i<runs; ++i) {
            run("Torus 64x64", in);
        }

        in = new Torus(128, 128, 2.0f, 4.0f);
        for(int i=0; i<runs; ++i) {
            run("Torus 128x128", in);
        }

        in = new Torus(160, 160, 2.0f, 4.0f);
        for(int i=0; i<runs; ++i) {
            run("Torus 160x160", in);
        }

        // Cannot have bigger meshes because they use short for the index buffer
    }
}
