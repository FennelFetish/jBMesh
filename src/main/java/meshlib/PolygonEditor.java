package meshlib;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.*;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import meshlib.conversion.LineExport;
import meshlib.operator.skeleton.SkeletonVisualization;
import meshlib.operator.skeleton.StraightSkeleton;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Vertex;
import meshlib.util.Gizmo;
import meshlib.util.Profiler;

public class PolygonEditor extends SimpleApplication {
    private static final String ACT_ADD_POINT    = "ACT_ADD_POINT";
    private static final String ACT_REMOVE_POINT = "ACT_REMOVE_POINT";
    private static final String ACT_RESET_POINTS = "ACT_RESET_POINTS";
    private static final String ACT_INC_DISTANCE = "ACT_INC_DISTANCE";
    private static final String ACT_DEC_DISTANCE = "ACT_DEC_DISTANCE";
    private static final String ACT_RESET_DISTANCE = "ACT_RESET_DISTANCE";
    private static final String ACT_BENCHMARK = "ACT_BENCHMARK";

    private static final String DEFAULT_EXPORT_PATH = "F:/jme/jBMesh/last.points";
    private static final String IMPORT_PATH = "";
    //private static final String IMPORT_PATH = "F:/jme/jBMesh/bench.points";

    private static final float BG_SIZE = 5000;
    private Geometry bg;

    private final Node pointNode = new Node("Points");
    private final Mesh pointMesh = new Sphere(8, 16, 0.1f);
    private final Mesh nodeMesh  = new Sphere(8, 16, 0.03f);
    private Material pointMat;
    private Material nodeMat;
    private BitmapFont font;

    private final Plane plane;
    private final List<Vector2f> points = new ArrayList<>();

    private static final float SKEL_DISTANCE_STEP = 0.05f;
    private static final float DEFAULT_DISTANCE = 0.0f;
    private float skeletonDistance = DEFAULT_DISTANCE;

    private boolean snapToGrid = true;


    private PolygonEditor() {
        super(new PanZoomState(25));

        plane = new Plane(Vector3f.UNIT_Z.clone(), new Vector3f(1, 0, 0));
    }


    @Override
    public void simpleInitApp() {
        assetManager.registerLocator("F:/jme/jBMesh/assets", FileLocator.class);
        setupBackground();

        pointMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        pointMat.setColor("Color", ColorRGBA.Red);

        nodeMat = pointMat.clone();
        nodeMat.setColor("Color", ColorRGBA.Black);

        font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        inputManager.addMapping(ACT_ADD_POINT, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(ACT_REMOVE_POINT, new KeyTrigger(KeyInput.KEY_DELETE));
        inputManager.addMapping(ACT_RESET_POINTS, new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping(ACT_INC_DISTANCE, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping(ACT_DEC_DISTANCE, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping(ACT_RESET_DISTANCE, new KeyTrigger(KeyInput.KEY_0));
        inputManager.addMapping(ACT_BENCHMARK, new KeyTrigger(KeyInput.KEY_B));
        inputManager.addListener(new ClickHandler(),
            ACT_ADD_POINT, ACT_REMOVE_POINT, ACT_RESET_POINTS, ACT_INC_DISTANCE, ACT_DEC_DISTANCE, ACT_RESET_DISTANCE, ACT_BENCHMARK);
        inputManager.addRawInputListener(new NumberListener());

        rootNode.attachChild(pointNode);

        Gizmo gizmo = new Gizmo(assetManager, "", 1.0f);
        rootNode.attachChild(gizmo);

        if(!IMPORT_PATH.isEmpty())
            importPoints(IMPORT_PATH);
        else
            importPoints(DEFAULT_EXPORT_PATH);
    }


    private void setupBackground() {
        Texture bgTex = assetManager.loadTexture("Textures/raster-bg.png");
        bgTex.setWrap(Texture.WrapMode.Repeat);
        bgTex.setMinFilter(Texture.MinFilter.Trilinear);
        bgTex.setMagFilter(Texture.MagFilter.Nearest);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.1f, 0.35f, 0.45f, 0.5f));
        mat.setTexture("ColorMap", bgTex);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        Quad quad = new Quad(BG_SIZE, BG_SIZE);
        quad.scaleTextureCoordinates(new Vector2f(BG_SIZE, BG_SIZE));

        bg = new Geometry("Background", quad);
        bg.move(-BG_SIZE/2, -BG_SIZE/2, -0.001f);
        bg.setMaterial(mat);
        bg.setQueueBucket(RenderQueue.Bucket.Transparent);

        rootNode.attachChild(bg);
        viewPort.setBackgroundColor(new ColorRGBA(0.04f, 0.03f, 0.04f, 1.0f));
    }


    private Vector2f pick() {
        Vector2f cursor = inputManager.getCursorPosition();
        Vector3f near = cam.getWorldCoordinates(cursor, 0.0f);
        Vector3f far = cam.getWorldCoordinates(cursor, 1.0f);
        Vector3f dir = far.subtract(near).normalizeLocal();

        Vector3f intersection = new Vector3f();
        Ray ray = new Ray(cam.getLocation(), dir);
        ray.intersectsWherePlane(plane, intersection);

        if(snapToGrid) {
            intersection.x = Math.round(intersection.x);
            intersection.y = Math.round(intersection.y);
        }

        return new Vector2f(intersection.x, intersection.y);
    }

    private void addPoint() {
        points.add(pick());
        updateVis();
        exportPoints(DEFAULT_EXPORT_PATH);
    }

    private void removePoint() {
        final float e2 = 0.001f * 0.001f;
        Vector2f pick = pick();
        for(int i=0; i<points.size(); ++i) {
            if(pick.distanceSquared(points.get(i)) < e2) {
                points.remove(i);
                break;
            }
        }
        updateVis();
        exportPoints(DEFAULT_EXPORT_PATH);
    }


    private Face createBMeshFace(BMesh bmesh) {
        Vertex[] vertices = new Vertex[points.size()];

        pointNode.detachAllChildren();

        for(int i=0; i<points.size(); ++i) {
            Vector2f p = points.get(i);
            Vector3f v = new Vector3f(p.x, p.y, 0);

            createPointVis(v, i);
            vertices[i] = bmesh.createVertex(v);
        }

        if(points.size() >= 3)
            return bmesh.createFace(vertices);

        return null;
    }

    private void updateVis() {
        BMesh bmesh = new BMesh();
        Face face = createBMeshFace(bmesh);

        if(face != null) {
            pointNode.attachChild(makeGeom(bmesh, ColorRGBA.Red));

            StraightSkeleton skeleton = new StraightSkeleton(bmesh);
            skeleton.setDistance(skeletonDistance);

            try(Profiler p = Profiler.start("StraightSkeleton.apply")) {
                skeleton.apply(face);
            }

            SkeletonVisualization skelVis = skeleton.getVisualization();
            pointNode.attachChild( makeGeom(skelVis.createSkeletonMappingVis(), ColorRGBA.Yellow) );
            pointNode.attachChild( makeGeom(skelVis.createSkeletonDegeneracyVis(), ColorRGBA.Brown) );
            pointNode.attachChild( makeGeom(skelVis.createMovingNodesVis(), ColorRGBA.Cyan) );
            pointNode.attachChild( makeGeom(skelVis.createBisectorVis(), ColorRGBA.Green) );
            //pointNode.attachChild( makeGeom(skelVis.createMappingVis(), ColorRGBA.Magenta) );

            for(SkeletonVisualization.VisNode node : skelVis.getMovingNodes()) {
                createMovingNodeVis(node.pos, node.name);
            }
        }
    }


    private void createPointVis(Vector3f v, int i) {
        Geometry geom = new Geometry("Point", pointMesh);
        geom.setMaterial(pointMat);
        geom.setLocalTranslation(v);
        pointNode.attachChild(geom);

        BitmapText text = new BitmapText(font);
        text.setText(Integer.toString(i+1));
        text.setSize(0.3f);
        text.setColor(new ColorRGBA(1, 0.2f, 0.2f, 1.0f));
        text.setQueueBucket(RenderQueue.Bucket.Transparent);
        text.setLocalTranslation(v);
        text.move(0.1f, -0.1f, 0);
        pointNode.attachChild(text);
    }

    private void createMovingNodeVis(Vector3f v, String name) {
        Geometry geom = new Geometry("Point", nodeMesh);
        geom.setMaterial(nodeMat);
        geom.setLocalTranslation(v);
        pointNode.attachChild(geom);

        BitmapText text = new BitmapText(font);
        text.setText(name);
        text.setSize(0.15f);
        text.setColor(new ColorRGBA(0.0f, 0.6f, 0.6f, 1.0f));
        text.setQueueBucket(RenderQueue.Bucket.Transparent);
        text.setLocalTranslation(v);
        text.move(0.05f, -0.05f, 0);
        pointNode.attachChild(text);
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


    private void importPoints(String file) {
        points.clear();
        skeletonDistance = DEFAULT_DISTANCE;

        Vector2f center = new Vector2f();

        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            for(String line; (line = reader.readLine()) != null; ) {
                String[] split = line.split(" ");
                Vector2f p = new Vector2f();
                p.x = Float.parseFloat(split[0]);
                p.y = Float.parseFloat(split[1]);
                points.add(p);
                center.addLocal(p);
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }

        if(!points.isEmpty()) {
            center.divideLocal(points.size());
            stateManager.getState(PanZoomState.class).setPos(center);
        }

        updateVis();
    }

    private void exportPoints(String file) {
        try(Writer writer = new BufferedWriter(new FileWriter(file))) {
            for(Vector2f p : points) {
                writer.write(Float.toString(p.x));
                writer.write(" ");
                writer.write(Float.toString(p.y));
                writer.write("\n");
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }


    private void benchmark() {
        skeletonDistance = Float.NEGATIVE_INFINITY;

        BMesh bmesh = new BMesh();
        Face face = createBMeshFace(bmesh);

        StraightSkeleton skeleton = new StraightSkeleton(bmesh);
        skeleton.setDistance(skeletonDistance);

        for(int i=0; i<200; ++i) {
            skeleton.apply(face);
        }

        try(Profiler p0 = Profiler.start("StraightSkeleton Benchmark")) {
            for(int i = 0; i < 2000; ++i) {
                try(Profiler p = Profiler.start("StraightSkeleton.apply")) {
                    skeleton.apply(face);
                }
            }
        }

        updateVis();
    }


    private class ClickHandler implements ActionListener {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if(!isPressed)
                return;

            switch(name) {
                case ACT_ADD_POINT:
                    addPoint();
                    break;

                case ACT_REMOVE_POINT:
                    removePoint();
                    break;

                case ACT_RESET_POINTS:
                    points.clear();
                    skeletonDistance = 0;
                    updateVis();
                    break;

                case ACT_INC_DISTANCE:
                    skeletonDistance += SKEL_DISTANCE_STEP;
                    updateVis();
                    break;

                case ACT_DEC_DISTANCE:
                    skeletonDistance -= SKEL_DISTANCE_STEP;
                    updateVis();
                    break;

                case ACT_RESET_DISTANCE:
                    skeletonDistance = 0;
                    updateVis();
                    break;

                case ACT_BENCHMARK:
                    benchmark();
                    break;
            }
        }
    }


    private class NumberListener implements RawInputListener {
        @Override
        public void onKeyEvent(KeyInputEvent evt) {
            int c = evt.getKeyCode() - KeyInput.KEY_1 + 1;
            if(c >= 1 && c <= 9) {
                String path = "F:/jme/jBMesh/bug" + c + ".points";
                if(Files.exists(Paths.get(path)))
                    importPoints(path);
            }
        }


        @Override
        public void beginInput() {}

        @Override
        public void endInput() {}

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {}

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {}

        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {}

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {}

        @Override
        public void onTouchEvent(TouchEvent evt) {}
    }



    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        //settings.setResolution(1900, 1000);
        settings.setFrameRate(200);
        settings.setSamples(8);
        settings.setGammaCorrection(true);
        settings.setResizable(true);

        PolygonEditor app = new PolygonEditor();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
}
