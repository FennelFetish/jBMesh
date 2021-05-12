package ch.alchemists.jbmesh.tools;

import ch.alchemists.jbmesh.conversion.LineExport;
import ch.alchemists.jbmesh.operator.sweeptriang.SweepTriangulation;
import ch.alchemists.jbmesh.operator.triangulation.SeidelTriangulation;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.tools.polygoneditor.PolygonEditorState;
import ch.alchemists.jbmesh.util.DebugVisualState;
import ch.alchemists.jbmesh.util.Profiler;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.style.BaseStyles;
import java.util.List;

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
    private final Node node = new Node("SweepTriangulationEditor");

    private static final float SWEEP_STEP = 0.1f;
    private float sweepLimit = 0;


    private SweepTriangulationEditor() {
        super(null);

        polygonEditor = new PolygonEditorState(pointListener);
        polygonEditor.setStoragePath(STORAGE_PATH);
        stateManager.attach(polygonEditor);

        debugVisualState = new DebugVisualState();
        debugVisualState.setName("SweepTriangulation");
        stateManager.attach(debugVisualState);

        polygonEditor.importFromDefaultFile();
    }


    @Override
    public void simpleInitApp() {
        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        rootNode.attachChild(node);

        inputManager.addMapping(ACT_MORE_SWEEP, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping(ACT_LESS_SWEEP, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping(ACT_RESET_SWEEP, new KeyTrigger(KeyInput.KEY_0));

        inputManager.addMapping(ACT_TRIANGULATE, new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping(ACT_BENCHMARK, new KeyTrigger(KeyInput.KEY_B));
        inputManager.addMapping(ACT_EXPORT, new KeyTrigger(KeyInput.KEY_E));
        inputManager.addListener(actionListener, ACT_MORE_SWEEP, ACT_LESS_SWEEP, ACT_RESET_SWEEP, ACT_TRIANGULATE, ACT_BENCHMARK, ACT_EXPORT);
    }


    private void updateTriangulation() {
        node.detachAllChildren();

        BMesh bmesh = new BMesh();
        Face face = polygonEditor.createBMeshFace(bmesh);

        if(face != null) {
            node.attachChild(makeGeom(bmesh, ColorRGBA.Red));

            SweepTriangulation triangulation = new SweepTriangulation(bmesh);
            triangulation.yLimit = sweepLimit;

            try(Profiler p = Profiler.start("SweepTriangulation.apply")) {
                triangulation.apply(face);
            } catch(Throwable ex) {
                ex.printStackTrace();
            }

            debugVisualState.reset();
            debugVisualState.setEnabled(true);
        }
        else {
            debugVisualState.setEnabled(false);
        }
    }


    private Geometry makeGeom(BMesh bmesh, ColorRGBA color) {
        // TODO: Make util class "DebugLineExport"?
        LineExport export = new LineExport(bmesh);
        export.update();

        Material mat = new Material(assetManager, Materials.UNSHADED);
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        mat.getAdditionalRenderState().setLineWidth(2.0f);

        Geometry geom = new Geometry("Geom", export.getMesh());
        geom.setMaterial(mat);
        return geom;
    }


    private void benchmark() {
        BMesh bmesh = new BMesh();
        Face face = polygonEditor.createBMeshFace(bmesh);

        SweepTriangulation triangulation = new SweepTriangulation(bmesh);
        triangulation.yLimit = Float.POSITIVE_INFINITY;

        for(int i=0; i<200; ++i) {
            triangulation.apply(face);
        }

        try(Profiler p0 = Profiler.start("SweepTriangulation Benchmark")) {
            for(int i = 0; i < 1000; ++i) {
                try(Profiler p = Profiler.start("SweepTriangulation.apply")) {
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
        public void onPointsReset() {
            sweepLimit = 0;
        }

        @Override
        public void onPointsUpdated(List<Vector2f> points) {
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
