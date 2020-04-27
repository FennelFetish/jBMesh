package meshlib;

import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Torus;
import meshlib.conversion.MeshConverter;
import meshlib.util.Profiler;

public class ConversionBenchmark {
/*
===== Profiler =====                                   Total [ms]    % of Parent  Avg [ms]    Max [ms]    Runs
Torus 16x16                                            74,237800000         %     14,8475600  33,9599000  5
·   Simple                                             39,806100000  53,6197%     7,96122000  18,6547000  5
·   Simple Mapped                                      10,399500000  14,0083%     2,07990000  5,81320000  5
·   Grid Mapped                                        8,2674000000  11,1363%     1,65348000  4,20280000  5
·   Sort Mapped                                        15,668900000  21,1063%     3,13378000  5,58390000  5
Torus 64x64                                            1522,5481000         %     304,509620  315,419200  5
·   Simple                                             724,96039999  47,6149%     144,992080  148,253600  5
·   Simple Mapped                                      716,65010000  47,0691%     143,330020  144,950800  5
·   Grid Mapped                                        25,377500000  1,66677%     5,07550000  7,03640000  5
·   Sort Mapped                                        55,422300000  3,64010%     11,0844600  17,8638000  5
Torus 128x128                                          25503,020099         %     5100,60402  5158,95289  5
·   Simple                                             13156,919500  51,5896%     2631,38390  2646,49480  5
·   Simple Mapped                                      12131,033200  47,5670%     2426,20664  2478,92720  5
·   Grid Mapped                                        75,731900000  0,29695%     15,1463800  17,7935000  5
·   Sort Mapped                                        139,15840000  0,54565%     27,8316800  37,1934000  5
Torus 160x160                                          61563,524500         %     12312,7049  12635,6729  5
·   Simple                                             31757,291200  51,5845%     6351,45824  6635,23880  5
·   Simple Mapped                                      29389,379399  47,7382%     5877,87588  6001,82569  5
·   Grid Mapped                                        196,34560000  0,31893%     39,2691200  53,7339000  5
·   Sort Mapped                                        220,27390000  0,35779%     44,0547800  55,0167000  5

===== Profiler =====                                   Total [ms]    % of Parent  Avg [ms]    Max [ms]    Runs
Torus 16x16                                            64,735700000         %     3,23678500  17,3090000  20
·   Grid Mapped                                        29,701600000  45,8813%     1,48508000  11,7549000  20
·   Sort Mapped                                        34,894700000  53,9033%     1,74473500  5,53510000  20
Torus 64x64                                            255,84269999         %     12,7921350  31,1704000  20
·   Grid Mapped                                        100,56660000  39,3079%     5,02833000  14,8229000  20
·   Sort Mapped                                        155,06160000  60,6081%     7,75308000  16,3243000  20
Torus 128x128                                          762,90069999         %     38,1450349  44,1849000  20
·   Grid Mapped                                        295,90729999  38,7871%     14,7953650  19,7726000  20
·   Sort Mapped                                        466,80100000  61,1876%     23,3400500  30,2699000  20
Torus 160x160                                          1643,8165999         %     82,1908299  129,289400  20
·   Grid Mapped                                        585,19970000  35,6000%     29,2599850  79,4782000  20
·   Sort Mapped                                        1058,3742000  64,3851%     52,9187100  101,467199  20           
*/


    private static final float EPSILON = 0.01f;


    private static void run(String name, Mesh in) {
        try(Profiler p0 = Profiler.start(name)) {
            /*try(Profiler p = Profiler.start("Simple")) {
                MeshConverter.convertSimple(in);
            }*/

            try(Profiler p = Profiler.start("Grid")) {
                MeshConverter.convertGrid(in);
            }

            /*try(Profiler p = Profiler.start("Simple Mapped")) {
                MeshConverter.convertSimpleMapped(in);
            }*/

            try(Profiler p = Profiler.start("Grid Mapped")) {
                MeshConverter.convertGridMapped(in);
            }

            try(Profiler p = Profiler.start("Sort Mapped")) {
                MeshConverter.convertSortMapped(in);
            }
        }
    }


    public static void main(String[] args) {
        final int runs = 20;

        Mesh in = new Torus(16, 16, 2.0f, 4.0f);
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
