package ch.alchemists.jbmesh.benchmarks;

import ch.alchemists.jbmesh.conversion.Import;
import ch.alchemists.jbmesh.util.Profiler;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Torus;

public class ConversionBenchmark {
/*
===== Profiler =====                                   Total [ms]    % of Parent  Avg [ms]    Min [ms]    Max [ms]    Runs
Torus 16x16                                            131.48740000         %     1.31487400  1.14770000  1.90640000  100
·   Grid                                               22.670000000  17.2411%     0.22670000  0.19840000  0.32370000  100
·   Optimized Grid                                     18.596100000  14.1428%     0.18596100  0.16100000  0.27640000  100
·   Grid Mapped                                        21.750400000  16.5418%     0.21750400  0.19090000  0.32050000  100
·   Optimized Grid Mapped                              17.858400000  13.5818%     0.17858400  0.15320000  0.27910000  100
·   Sort Mapped                                        35.121800000  26.7111%     0.35121800  0.30750000  0.64550000  100
·   Exact Hash Mapped                                  15.141500000  11.5155%     0.15141500  0.12580000  0.24160000  100

Torus 64x64                                            2110.0178000         %     21.1001780  19.5122000  25.6085000  100
·   Grid                                               367.86949999  17.4344%     3.67869500  3.37500000  4.48910000  100
·   Optimized Grid                                     298.08729999  14.1272%     2.98087300  2.73750000  3.77290000  100
·   Grid Mapped                                        347.48300000  16.4682%     3.47483000  3.24360000  4.31720000  100
·   Optimized Grid Mapped                              277.08320000  13.1317%     2.77083200  2.52700000  3.55370000  100
·   Sort Mapped                                        589.25040000  27.9263%     5.89250400  5.49640000  7.17880000  100
·   Exact Hash Mapped                                  229.77120000  10.8895%     2.29771200  1.95600000  4.81990000  100

Torus 128x128                                          8411.8395000         %     84.1183950  81.7796000  100.359299  100
·   Grid                                               1542.8678000  18.3416%     15.4286780  14.7763000  18.5259000  100
·   Optimized Grid                                     1177.6369000  13.9997%     11.7763690  11.3921000  15.2473000  100
·   Grid Mapped                                        1415.9125999  16.8323%     14.1591260  13.7803000  16.7398000  100
·   Optimized Grid Mapped                              1100.5850000  13.0837%     11.0058500  10.6543000  13.5028000  100
·   Sort Mapped                                        2317.4888000  27.5503%     23.1748880  22.4726000  29.8629000  100
·   Exact Hash Mapped                                  856.48859999  10.1819%     8.56488600  8.25420000  12.4457000  100

Torus 160x160                                          20974.885800         %     209.748858  138.581900  439.127600  100
·   Grid                                               3894.5416999  18.5676%     38.9454170  24.7248000  212.455899  100
·   Optimized Grid                                     3472.0195000  16.5532%     34.7201950  20.6840000  125.861899  100
·   Grid Mapped                                        3104.0411000  14.7988%     31.0404110  22.5838000  112.278200  100
·   Optimized Grid Mapped                              3073.3887000  14.6527%     30.7338870  18.7810000  101.239600  100
·   Sort Mapped                                        5379.5292000  25.6474%     53.7952920  37.3898000  156.631299  100
·   Exact Hash Mapped                                  2049.7774999  9.77253%     20.4977750  13.4377000  111.662399  100
*/



    private static void run(String name, Mesh in) {
        try(Profiler p0 = Profiler.start(name)) {
            try(Profiler p = Profiler.start("Hash Grid Mapped")) {
                Import.convert(in);
            }

            try(Profiler p = Profiler.start("Exact Hash Mapped")) {
                Import.convertExact(in);
            }
        }
    }


    public static void main(String[] args) {
        // Warmup
        Mesh in = new Torus(128, 128, 2.0f, 4.0f);
        for(int i=0; i<50; ++i) {
            run("Warmup", in);
        }


        final int runs = 300;

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
