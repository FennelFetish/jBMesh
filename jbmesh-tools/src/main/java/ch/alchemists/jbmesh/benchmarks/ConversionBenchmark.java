package ch.alchemists.jbmesh.benchmarks;

import ch.alchemists.jbmesh.conversion.DirectImport;
import ch.alchemists.jbmesh.conversion.Import;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.util.Profiler;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Torus;
import java.util.function.Function;

public class ConversionBenchmark {
/*
===== Profiler =====                                   Total [ms]    % of Parent  Avg [ms]    Min [ms]    Max [ms]    Runs
HashGrid Dedup                                         14421.277100         %     48.0709236  32.6904000  92.6225999  300
·   Torus 16x16                                        51.570500000  0.35760%     0.17190166  0.16200000  0.24800000  300
·   Torus 64x64                                        990.76519999  6.87016%     3.30255066  2.75150000  53.2623000  300
·   Torus 128x128                                      5191.2252000  35.9969%     17.3040840  11.2725000  70.3554999  300
·   Torus 160x160                                      8186.2338000  56.7649%     27.2874460  18.3072000  76.2132999  300

ExactHash Dedup                                        10064.161399         %     33.5472046  22.1313000  114.868100  300
·   Torus 16x16                                        37.411600000  0.37173%     0.12470533  0.11860000  0.23390000  300
·   Torus 64x64                                        887.75990000  8.82100%     2.95919966  1.81270000  63.0346999  300
·   Torus 128x128                                      3606.3309000  35.8333%     12.0211030  7.64670000  75.9763000  300
·   Torus 160x160                                      5531.6726000  54.9640%     18.4389086  12.4648000  88.6999000  300

Direct Import                                          11601.325600         %     38.6710853  27.5123000  94.1293000  300
·   Torus 16x16                                        50.095400000  0.43180%     0.16698466  0.15240000  0.28370000  300
·   Torus 64x64                                        1000.0464999  8.62010%     3.33348833  2.25840000  57.6282000  300
·   Torus 128x128                                      3992.4831000  34.4140%     13.3082770  9.43550000  75.5944000  300
·   Torus 160x160                                      6557.3948999  56.5228%     21.8579830  15.5642000  80.7501999  300
*/


    private static final Mesh in1 = new Torus(16, 16, 2.0f, 4.0f);
    private static final Mesh in2 = new Torus(64, 64, 2.0f, 4.0f);
    private static final Mesh in3 = new Torus(128, 128, 2.0f, 4.0f);
    private static final Mesh in4 = new Torus(160, 160, 2.0f, 4.0f);
    // Cannot have bigger meshes because they use short for the index buffer

    private static void run(String name, Function<Mesh, BMesh> func) {
        try(Profiler p0 = Profiler.start(name)) {
            try(Profiler p = Profiler.start("Torus 16x16")) {
                func.apply(in1);
            }

            try(Profiler p = Profiler.start("Torus 64x64")) {
                func.apply(in2);
            }

            try(Profiler p = Profiler.start("Torus 128x128")) {
                func.apply(in3);
            }

            try(Profiler p = Profiler.start("Torus 160x160")) {
                func.apply(in4);
            }
        }
    }


    public static void main(String[] args) {
        final int runs = 300;

        // Warmup
        try(Profiler p0 = Profiler.start("Warmup")) {
            for(int i=runs/10; i>=0; --i) {
                run("HashGrid Dedup", Import::convert);
                run("ExactHash Dedup", Import::convertExact);
                run("Direct Import", DirectImport::importTriangles);
            }
        }
        
        for(int i=0; i<runs; ++i) {
            run("HashGrid Dedup", Import::convert);
        }

        for(int i=0; i<runs; ++i) {
            run("ExactHash Dedup", Import::convertExact);
        }

        for(int i=0; i<runs; ++i) {
            run("Direct Import", DirectImport::importTriangles);
        }
    }
}
