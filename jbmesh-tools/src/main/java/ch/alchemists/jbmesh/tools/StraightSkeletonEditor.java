package ch.alchemists.jbmesh.tools;

import ch.alchemists.jbmesh.conversion.LineExport;
import ch.alchemists.jbmesh.operator.skeleton.SkeletonVisualization;
import ch.alchemists.jbmesh.operator.skeleton.StraightSkeleton;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.PolygonEditorState;
import ch.alchemists.jbmesh.util.Profiler;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import java.util.List;

public class StraightSkeletonEditor extends SimpleApplication {
    private static final String STORAGE_PATH       = "F:/jme/jBMesh/points";

    private static final String ACT_INC_DISTANCE   = "ACT_INC_DISTANCE";
    private static final String ACT_DEC_DISTANCE   = "ACT_DEC_DISTANCE";
    private static final String ACT_RESET_DISTANCE = "ACT_RESET_DISTANCE";
    private static final String ACT_MAX_DISTANCE   = "ACT_MAX_DISTANCE";
    private static final String ACT_BENCHMARK      = "ACT_BENCHMARK";

    private static final float SKEL_DISTANCE_STEP  = 0.05f;
    private static final float DEFAULT_DISTANCE    = 0.0f;
    private float skeletonDistance                 = DEFAULT_DISTANCE;

    private PolygonEditorState polygonEditor;
    private final Node node = new Node("StraightSkeletonEditor");

    private PolygonEditorState.PointDrawType movingNodeType;


    private StraightSkeletonEditor() {
        super(null);
    }


    @Override
    public void simpleInitApp() {
        polygonEditor = new PolygonEditorState(new SkeletonEditorPointListener());
        polygonEditor.setStoragePath(STORAGE_PATH);
        polygonEditor.importDefaultPoints();
        //polygonEditor.importPoints("bench1000.points");
        stateManager.attach(polygonEditor);

        rootNode.attachChild(node);

        inputManager.addMapping(ACT_INC_DISTANCE, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping(ACT_DEC_DISTANCE, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping(ACT_RESET_DISTANCE, new KeyTrigger(KeyInput.KEY_0));
        inputManager.addMapping(ACT_MAX_DISTANCE, new KeyTrigger(KeyInput.KEY_M));
        inputManager.addMapping(ACT_BENCHMARK, new KeyTrigger(KeyInput.KEY_B));
        inputManager.addListener(new ClickHandler(), ACT_INC_DISTANCE, ACT_DEC_DISTANCE, ACT_RESET_DISTANCE, ACT_MAX_DISTANCE, ACT_BENCHMARK);

        movingNodeType = new PolygonEditorState.PointDrawType(assetManager, ColorRGBA.Black, 0.06f, 0.15f);
        movingNodeType.textColor = new ColorRGBA(0.0f, 0.6f, 0.6f, 1.0f);
        rootNode.attachChild(movingNodeType.container);
    }


    private void updateSkeletonVis() {
        node.detachAllChildren();
        movingNodeType.container.detachAllChildren();

        BMesh bmesh = new BMesh();
        Face face = createBMeshFace(polygonEditor.getPoints(), bmesh);

        if(face != null) {
            node.attachChild(makeGeom(bmesh, ColorRGBA.Red));

            StraightSkeleton skeleton = new StraightSkeleton(bmesh);
            skeleton.setDistance(skeletonDistance);

            try(Profiler p = Profiler.start("StraightSkeleton.apply")) {
                skeleton.apply(face);
            }

            SkeletonVisualization skelVis = skeleton.getVisualization();
            node.attachChild( makeGeom(skelVis.createSkeletonMappingVis(), ColorRGBA.Yellow) );
            node.attachChild( makeGeom(skelVis.createSkeletonDegeneracyVis(), ColorRGBA.Brown) );
            node.attachChild( makeGeom(skelVis.createMovingNodesVis(), ColorRGBA.Cyan) );
            //node.attachChild( makeGeom(skelVis.createBisectorVis(), ColorRGBA.Green) );
            //node.attachChild( makeGeom(skelVis.createMappingVis(), ColorRGBA.Magenta) );

            for(SkeletonVisualization.VisNode node : skelVis.getMovingNodes()) {
                polygonEditor.createPointVis(movingNodeType, node.pos, node.name);
            }
        }
    }


    private Geometry makeGeom(BMesh bmesh, ColorRGBA color) {
        // TODO: Make util class "DebugLineExport"?
        LineExport export = new LineExport(bmesh);
        export.update();

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        mat.getAdditionalRenderState().setLineWidth(2.0f);

        Geometry geom = new Geometry("Geom", export.getMesh());
        geom.setMaterial(mat);
        return geom;
    }


    private Face createBMeshFace(List<Vector2f> points, BMesh bmesh) {
        Vertex[] vertices = new Vertex[points.size()];

        for(int i=0; i<points.size(); ++i) {
            Vector2f p = points.get(i);
            Vector3f v = new Vector3f(p.x, p.y, 0);
            vertices[i] = bmesh.createVertex(v);
        }

        if(points.size() >= 3)
            return bmesh.createFace(vertices);

        return null;
    }


    private void benchmark() {
        skeletonDistance = Float.NEGATIVE_INFINITY;

        BMesh bmesh = new BMesh();
        Face face = createBMeshFace(polygonEditor.getPoints(), bmesh);

        StraightSkeleton skeleton = new StraightSkeleton(bmesh);
        skeleton.setDistance(skeletonDistance);

        for(int i=0; i<200; ++i) {
            skeleton.apply(face);
        }

        try(Profiler p0 = Profiler.start("StraightSkeleton Benchmark")) {
            for(int i = 0; i < 1000; ++i) {
                try(Profiler p = Profiler.start("StraightSkeleton.apply")) {
                    skeleton.apply(face);
                }
            }
        }

        updateSkeletonVis();
    }



    private final class SkeletonEditorPointListener implements PolygonEditorState.PointListener {
        @Override
        public void onPointsReset() {
            skeletonDistance = DEFAULT_DISTANCE;
        }

        @Override
        public void onPointsUpdated(List<Vector2f> points) {
            updateSkeletonVis();
        }
    }



    private class ClickHandler implements ActionListener {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if(!isPressed)
                return;

            switch(name) {
                case ACT_INC_DISTANCE:
                    skeletonDistance += SKEL_DISTANCE_STEP;
                    updateSkeletonVis();
                    break;

                case ACT_DEC_DISTANCE:
                    skeletonDistance -= SKEL_DISTANCE_STEP;
                    updateSkeletonVis();
                    break;

                case ACT_RESET_DISTANCE:
                    skeletonDistance = 0;
                    updateSkeletonVis();
                    break;

                case ACT_MAX_DISTANCE:
                    skeletonDistance = Float.NEGATIVE_INFINITY;
                    updateSkeletonVis();
                    break;

                case ACT_BENCHMARK:
                    benchmark();
                    break;
            }
        }
    }



    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        //settings.setResolution(1900, 1000);
        settings.setFrameRate(200);
        settings.setSamples(8);
        settings.setGammaCorrection(true);
        settings.setResizable(true);

        StraightSkeletonEditor app = new StraightSkeletonEditor();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
}
