package ch.alchemists.jbmesh.tools;

import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.operator.sweeptriang.SweepTriangulation;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.tools.polygoneditor.PolygonEditorState;
import ch.alchemists.jbmesh.util.DebugVisual;
import ch.alchemists.jbmesh.util.DebugVisualState;
import ch.alchemists.jbmesh.util.Profiler;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SweepTriangulationEditor extends SimpleApplication {
    private static final String STORAGE_PATH       = "F:/jme/jBMesh/sweepTriPoints";
    private static final String EXPORT_FILE        = "sweep-triangulation.points";

    private static final String ACT_MORE_SWEEP     = "ACT_MORE_SWEEP";
    private static final String ACT_LESS_SWEEP     = "ACT_LESS_SWEEP";
    private static final String ACT_RESET_SWEEP    = "ACT_RESET_SWEEP";

    private static final String ACT_TRIANGULATE    = "ACT_TRIANGULATE";
    private static final String ACT_BENCHMARK      = "ACT_BENCHMARK";
    private static final String ACT_EXPORT         = "ACT_EXPORT";

    private final PolygonEditorState polygonEditor;
    private final DebugVisualState debugVisualState;
    private Node sweepLine;

    private static final float SWEEP_STEP = 0.5f;
    private float sweepLimit = 0;


    private SweepTriangulationEditor() {
        super(null);

        polygonEditor = new PolygonEditorState(pointListener);
        polygonEditor.setStoragePath(STORAGE_PATH);
        stateManager.attach(polygonEditor);

        debugVisualState = new DebugVisualState("Triangles");
        stateManager.attach(debugVisualState);

        polygonEditor.importFromDefaultFile();
        //polygonEditor.importPoints("bench1000.points");
        //polygonEditor.importPoints("bench.points");
    }


    @Override
    public void simpleInitApp() {
        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        inputManager.addMapping(ACT_MORE_SWEEP, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping(ACT_LESS_SWEEP, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping(ACT_RESET_SWEEP, new KeyTrigger(KeyInput.KEY_0));

        inputManager.addMapping(ACT_TRIANGULATE, new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping(ACT_BENCHMARK, new KeyTrigger(KeyInput.KEY_B));
        inputManager.addMapping(ACT_EXPORT, new KeyTrigger(KeyInput.KEY_E));
        inputManager.addListener(actionListener, ACT_MORE_SWEEP, ACT_LESS_SWEEP, ACT_RESET_SWEEP, ACT_TRIANGULATE, ACT_BENCHMARK, ACT_EXPORT);
    }


    private void updateTriangulation() {
        // Prepare debug visuals
        DebugVisual.clear("Triangles");
        DebugVisual dbg = DebugVisual.get("Triangles");
        dbg.pointSize = 0.2f;
        dbg.pointColor = ColorRGBA.Yellow;

        DebugVisual.clear("SweepTriangulation");

        // Prepare BMesh & Triangulation
        BMesh bmesh = new BMesh();
        SweepTriangulation triangulation = new SweepTriangulation(bmesh);
        triangulation.yLimit = sweepLimit;

        Vec3Property<Vertex> propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
        triangulation.setTriangleCallback((v1, v2, v3) -> {
            //System.out.println("Triangle: " + (v1.index+1) + " " + (v2.index+1) + " " + (v3.index+1));
            Vector3f p1 = propPosition.get(v1.vertex);
            Vector3f p2 = propPosition.get(v2.vertex);
            Vector3f p3 = propPosition.get(v3.vertex);
            dbg.addFace(p1, p2, p3);
        });

        try {
            // Add faces
            boolean hasFaces = false;
            for(List<Vector2f> points : polygonEditor.getAllPoints()) {
                Face face = polygonEditor.createBMeshFace(bmesh, points);
                if(face != null) {
                    triangulation.addFace(face);
                    hasFaces = true;
                }
            }

            // Triangulate
            if(hasFaces) {
                try(Profiler p = Profiler.start("SweepTriangulation.apply")) {
                    triangulation.triangulate();
                }
            }
        }
        catch(Throwable ex) {
            ex.printStackTrace();
        }

        debugVisualState.updateVis();

        DebugVisual dbgLine = DebugVisual.get("SweepTriangulation");
        dbgLine.addLine(new Vector3f(-1000, sweepLimit, 0), new Vector3f(1000, sweepLimit, 0));
        if(sweepLine != null)
            rootNode.detachChild(sweepLine);
        sweepLine = dbgLine.createNode(assetManager);
        rootNode.attachChild(sweepLine);
    }


    private void benchmark() {
        final int runs = 100000;

        BMesh bmesh = new BMesh();
        Face face = polygonEditor.createBMeshFace(bmesh);

        SweepTriangulation triangulation = new SweepTriangulation(bmesh);
        triangulation.setTriangleCallback((v1, v2, v3) -> {});

        for(int i=runs/15; i>=0; --i) {
            triangulation.addFace(face);
            triangulation.triangulate();
        }

        try(Profiler p0 = Profiler.start("SweepTriangulation Benchmark")) {
            for(int i = 0; i < runs; ++i) {
                try(Profiler p = Profiler.start("SweepTriangulation.apply")) {
                    triangulation.addFace(face);
                    triangulation.triangulate();
                }

                if((i&8191) == 0)
                    System.gc();
            }
        }

        Profiler.printAndClear();
    }


    private void exportFile() {
        polygonEditor.exportPoints(EXPORT_FILE);
    }


    private final PolygonEditorState.PointListener pointListener = new PolygonEditorState.PointListener() {
        @Override
        public void onPointsReset() {
            sweepLimit = 0;
        }

        @Override
        public void onPointsUpdated(Map<Integer, ArrayList<Vector2f>> pointMap) {
            updateTriangulation();
        }
    };


    private final ActionListener actionListener = (String name, boolean isPressed, float tpf) -> {
        if(!isPressed)
            return;

        switch(name) {
            case ACT_MORE_SWEEP:
                sweepLimit += SWEEP_STEP;
                updateTriangulation();
                break;

            case ACT_LESS_SWEEP:
                sweepLimit -= SWEEP_STEP;
                updateTriangulation();
                break;

            case ACT_RESET_SWEEP:
                sweepLimit = 0;
                updateTriangulation();
                break;

            case ACT_TRIANGULATE:
                updateTriangulation();
                break;

            case ACT_BENCHMARK:
                benchmark();
                break;

            case ACT_EXPORT:
                exportFile();
                break;
        }
    };


    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        settings.setFrameRate(200);
        settings.setSamples(8);
        settings.setGammaCorrection(true);
        settings.setResizable(true);

        SweepTriangulationEditor app = new SweepTriangulationEditor();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
}
