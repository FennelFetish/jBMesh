package ch.alchemists.jbmesh.tools;

import ch.alchemists.jbmesh.operator.triangulation.SeidelTriangulation;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.tools.polygoneditor.PolygonEditorState;
import ch.alchemists.jbmesh.util.DebugVisualState;
import ch.alchemists.jbmesh.util.Profiler;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;
import java.util.ArrayList;
import java.util.Map;

public class TriangulationEditor extends SimpleApplication {
    private static final String STORAGE_PATH       = "F:/jme/jBMesh/points";
    private static final String EXPORT_FILE        = "triangulation.points";

    private static final String ACT_TRIANGULATE    = "ACT_TRIANGULATE";
    private static final String ACT_BENCHMARK      = "ACT_BENCHMARK";
    private static final String ACT_EXPORT         = "ACT_EXPORT";

    private final PolygonEditorState polygonEditor;
    private final DebugVisualState debugVisualState;
    private final Node node = new Node("TriangulationEditor");


    private TriangulationEditor() {
        super(null);

        polygonEditor = new PolygonEditorState(pointListener);
        polygonEditor.setStoragePath(STORAGE_PATH);
        stateManager.attach(polygonEditor);

        debugVisualState = new DebugVisualState("Seidel");
        stateManager.attach(debugVisualState);

        polygonEditor.importFromDefaultFile();
    }


    @Override
    public void simpleInitApp() {
        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        rootNode.attachChild(node);

        inputManager.addMapping(ACT_TRIANGULATE, new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping(ACT_BENCHMARK, new KeyTrigger(KeyInput.KEY_B));
        inputManager.addMapping(ACT_EXPORT, new KeyTrigger(KeyInput.KEY_E));
        inputManager.addListener(actionListener, ACT_TRIANGULATE, ACT_BENCHMARK, ACT_EXPORT);
    }


    private void updateTriangulation() {
        node.detachAllChildren();

        BMesh bmesh = new BMesh();
        Face face = polygonEditor.createBMeshFace(bmesh);

        if(face != null) {
            SeidelTriangulation triangulation = new SeidelTriangulation(bmesh);
            try(Profiler p = Profiler.start("SeidelTriangulation.apply")) {
                triangulation.apply(face);
            }

            debugVisualState.setEnabled(true);
        }
        else {
            debugVisualState.setEnabled(false);
        }
    }


    private void benchmark() {
        BMesh bmesh = new BMesh();
        Face face = polygonEditor.createBMeshFace(bmesh);

        SeidelTriangulation triangulation = new SeidelTriangulation(bmesh);

        for(int i=0; i<200; ++i) {
            triangulation.apply(face);
        }

        try(Profiler p0 = Profiler.start("SeidelTriangulation Benchmark")) {
            for(int i = 0; i < 1000; ++i) {
                try(Profiler p = Profiler.start("SeidelTriangulation.apply")) {
                    triangulation.apply(face);
                }
                System.gc();
            }
        }

        updateTriangulation();
    }


    private void exportFile() {
        polygonEditor.exportPoints(EXPORT_FILE);
    }


    private final PolygonEditorState.PointListener pointListener = new PolygonEditorState.PointListener() {
        @Override
        public void onPointsReset() {}

        @Override
        public void onPointsUpdated(Map<Integer, ArrayList<Vector2f>> pointMap) {
            updateTriangulation();
        }
    };


    private final ActionListener actionListener = (String name, boolean isPressed, float tpf) -> {
        if(!isPressed)
            return;

        switch(name) {
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

        TriangulationEditor app = new TriangulationEditor();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
}
