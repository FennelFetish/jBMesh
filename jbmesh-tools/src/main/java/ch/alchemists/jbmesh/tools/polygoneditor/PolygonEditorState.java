package ch.alchemists.jbmesh.tools.polygoneditor;

import ch.alchemists.jbmesh.conversion.LineExport;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.BasicShapes;
import ch.alchemists.jbmesh.util.Gizmo;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.*;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PolygonEditorState extends BaseAppState {
    public interface PointListener {
        void onPointsReset();
        void onPointsUpdated(Map<Integer, ArrayList<Vector2f>> pointMap);
    }

    public static class PointDrawType {
        public final Node container = new Node("Point Container");
        public final Mesh mesh;
        public final Material mat;
        public final float fontSize;

        public ColorRGBA textColor = new ColorRGBA(1, 0.2f, 0.2f, 1.0f);

        public PointDrawType(AssetManager assetManager, ColorRGBA color, float pointSize, float fontSize) {
            mesh = BasicShapes.disc(16, pointSize*0.5f);

            mat = new Material(assetManager, Materials.UNSHADED);
            mat.setColor("Color", color);

            this.fontSize = fontSize;
        }
    }


    private static final String ACT_RESET_POINTS = "ACT_RESET_POINTS";
    private static final String ACT_SUBDIVIDE    = "ACT_SUBDIVIDE";
    private static final String ACT_PERTURB      = "ACT_PERTURB";
    private static final String ACT_RELOAD_FILE  = "ACT_RELOAD_FILE";

    private static final String DEFAULT_EXPORT_FILE = "last.points";
    private Path storageBasePath = null;
    private String currentFile = DEFAULT_EXPORT_FILE;

    private final PointListener listener;
    private final Plane plane;
    private final Map<Integer, ArrayList<Vector2f>> pointMap = new HashMap<>();
    private int currentPolygon = 0;

    private static final float BG_SIZE = 5000;
    private Geometry bg;

    private BitmapFont font;
    private PointDrawType defaultPointType;

    private PanZoomState panZoomState;
    private ToolBar toolBar;
    private Gizmo gizmo;
    private BitmapText cursorPositionLabel;


    public PolygonEditorState(PointListener listener) {
        this.listener = listener;
        plane = new Plane(Vector3f.UNIT_Z.clone(), new Vector3f(1, 0, 0));
    }


    public List<Vector2f> getPoints() {
        return getPoints(currentPolygon);
    }

    public ArrayList<Vector2f> getPoints(int polygonIndex) {
        ArrayList<Vector2f> list = pointMap.computeIfAbsent(polygonIndex, k -> new ArrayList<>());
        return list;
    }

    public Collection<List<Vector2f>> getAllPoints() {
        return Collections.unmodifiableCollection(pointMap.values());
    }


    public void setStoragePath(String path) {
        storageBasePath = Paths.get(path).toAbsolutePath().normalize();
    }


    @Override
    protected void initialize(Application app) {
        SimpleApplication simpleApp = (SimpleApplication) app;
        AssetManager assetManager = app.getAssetManager();
        Node rootNode = simpleApp.getRootNode();

        panZoomState = new PanZoomState(35);
        app.getStateManager().attach(panZoomState);

        toolBar = new ToolBar(this);
        app.getStateManager().attach(toolBar);

        setupBackground(assetManager, rootNode);
        setupInput(app.getInputManager());

        font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        setupCursorPositionLabel(simpleApp.getGuiNode());

        defaultPointType = new PointDrawType(assetManager, ColorRGBA.Red, 0.2f, 0.3f);
        rootNode.attachChild(defaultPointType.container);

        gizmo = new Gizmo(assetManager, "", 1.0f);
        rootNode.attachChild(gizmo);

        updatePoints();
        centerView();
    }

    private void setupBackground(AssetManager assetManager, Node rootNode) {
        Texture bgTex = assetManager.loadTexture("Textures/raster-bg.png");
        bgTex.setWrap(Texture.WrapMode.Repeat);
        bgTex.setMinFilter(Texture.MinFilter.Trilinear);
        bgTex.setMagFilter(Texture.MagFilter.Nearest);

        Material mat = new Material(assetManager, Materials.UNSHADED);
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
        getApplication().getViewPort().setBackgroundColor(new ColorRGBA(0.04f, 0.03f, 0.04f, 1.0f));
    }

    private void setupInput(InputManager inputManager) {
        inputManager.addMapping(ACT_RESET_POINTS, new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping(ACT_SUBDIVIDE, new KeyTrigger(KeyInput.KEY_X));
        inputManager.addMapping(ACT_PERTURB, new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping(ACT_RELOAD_FILE, new KeyTrigger(KeyInput.KEY_HOME));

        inputManager.addListener(actionListener, ACT_RESET_POINTS, ACT_SUBDIVIDE, ACT_PERTURB, ACT_RELOAD_FILE);
        inputManager.addRawInputListener(numberInputListener);
    }

    private void setupCursorPositionLabel(Node guiNode) {
        cursorPositionLabel = new BitmapText(font);
        cursorPositionLabel.setSize(12f);
        cursorPositionLabel.setTabWidth(10f);
        cursorPositionLabel.setLocalTranslation(4, 18, 0);
        guiNode.attachChild(cursorPositionLabel);
    }


    @Override
    protected void cleanup(Application app) {
        SimpleApplication simpleApp = (SimpleApplication) app;
        simpleApp.getRootNode().detachChild(bg);
        bg = null;

        simpleApp.getGuiNode().detachChild(cursorPositionLabel);
        cursorPositionLabel = null;

        simpleApp.getRootNode().detachChild(gizmo);
        gizmo = null;

        app.getStateManager().detach(panZoomState);
        panZoomState = null;

        app.getStateManager().detach(toolBar);
        toolBar = null;

        app.getInputManager().deleteMapping(ACT_RESET_POINTS);
        app.getInputManager().deleteMapping(ACT_SUBDIVIDE);
        app.getInputManager().deleteMapping(ACT_RELOAD_FILE);
        app.getInputManager().removeListener(actionListener);
        app.getInputManager().removeRawInputListener(numberInputListener);
    }


    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}


    @Override
    public void update(float tpf) {
        Vector2f pick = pick(getCursorPosition());
        String text = String.format("X: %.2f\tY: %.2f", pick.x, pick.y);
        cursorPositionLabel.setText(text);
    }


    public Face createBMeshFace(BMesh bmesh) {
        return createBMeshFace(bmesh, currentPolygon);
    }

    public Face createBMeshFace(BMesh bmesh, int polygonIndex) {
        List<Vector2f> points = getPoints(polygonIndex);
       return createBMeshFace(bmesh, points);
    }

    public Face createBMeshFace(BMesh bmesh, List<Vector2f> points) {
        if(points.size() < 3)
            return null;

        List<Vertex> vertices = new ArrayList<>(points.size());
        for(Vector2f p : points)
            vertices.add( bmesh.createVertex(p.x, p.y, 0) );

        return bmesh.createFace(vertices);
    }


    public Geometry createLineGeom(BMesh bmesh, ColorRGBA color) {
        // TODO: Make util class "DebugLineExport"?
        LineExport export = new LineExport(bmesh);
        export.update();

        Material mat = new Material(getApplication().getAssetManager(), Materials.UNSHADED);
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        mat.getAdditionalRenderState().setLineWidth(2.0f);

        Geometry geom = new Geometry("Geom", export.getMesh());
        geom.setMaterial(mat);
        return geom;
    }


    public Vector2f getCursorPosition() {
        return getApplication().getInputManager().getCursorPosition();
    }


    public Vector2f pick(Vector2f cursor) {
        Camera cam = getApplication().getCamera();

        Vector3f near = cam.getWorldCoordinates(cursor, 0.0f);
        Vector3f far  = cam.getWorldCoordinates(cursor, 1.0f);
        Vector3f dir  = far.subtract(near).normalizeLocal();

        Vector3f intersection = new Vector3f();
        Ray ray = new Ray(cam.getLocation(), dir);
        ray.intersectsWherePlane(plane, intersection);

        return new Vector2f(intersection.x, intersection.y);
    }


    public void updatePoints() {
        defaultPointType.container.detachAllChildren();

        for(Map.Entry<Integer, ArrayList<Vector2f>> entry : pointMap.entrySet()) {
            // Draw points with numbers
            List<Vector2f> points = entry.getValue();
            for(int i = 0; i < points.size(); ++i) {
                Vector2f p = points.get(i);
                createPointVis(defaultPointType, p, Integer.toString(i + 1));
            }

            // Draw lines between points
            BMesh bmesh = new BMesh();
            createBMeshFace(bmesh, points);
            ColorRGBA color = (entry.getKey() == currentPolygon) ? ColorRGBA.Red : ColorRGBA.Black;
            Geometry geom = createLineGeom(bmesh, color);
            defaultPointType.container.attachChild(geom);
        }

        listener.onPointsUpdated(pointMap);
    }


    public void createPointVis(PointDrawType type, Vector3f p, String text) {
        Vector2f p2f = new Vector2f(p.x, p.y);
        createPointVis(type, p2f, text);
    }

    public void createPointVis(PointDrawType type, Vector2f p, String text) {
        Geometry geom = new Geometry("Point", type.mesh);
        geom.setMaterial(type.mat);
        geom.setLocalTranslation(p.x, p.y, 0);
        type.container.attachChild(geom);

        BitmapText bitmapText = new BitmapText(font);
        bitmapText.setText(text);
        bitmapText.setSize(type.fontSize);
        bitmapText.setColor(type.textColor);
        bitmapText.setQueueBucket(RenderQueue.Bucket.Transparent);
        bitmapText.setLocalTranslation(p.x, p.y, 0);
        bitmapText.move(0.2f * type.fontSize, -0.2f * type.fontSize, 0);
        type.container.attachChild(bitmapText);
    }


    private String getFilePath(String file) {
        if(storageBasePath == null)
            return null;
        return storageBasePath.toString() + "/" + file;
    }

    public void importFromDefaultFile() {
        importPoints(DEFAULT_EXPORT_FILE);
    }

    public void importPoints(String file) {
        currentFile = file;
        String path = getFilePath(file);
        if(path == null)
            return;

        try(BufferedReader reader = new BufferedReader(new FileReader(path))) {
            pointMap.clear();
            int pointIndex = 0;

            for(String line; (line = reader.readLine()) != null; ) {
                if(line.startsWith("---")) {
                    pointIndex++;
                    continue;
                }

                String[] split = line.split(" ");

                Vector2f p = new Vector2f();
                p.x = Float.parseFloat(split[0]);
                p.y = Float.parseFloat(split[1]);
                getPoints(pointIndex).add(p);
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }

        if(isInitialized()) {
            listener.onPointsReset();
            updatePoints();
            centerView();
        }
    }


    public void exportToDefaultFile() {
        exportPoints(DEFAULT_EXPORT_FILE);
    }

    public void exportPoints(String file) {
        String path = getFilePath(file);
        if(path == null)
            return;

        try(Writer writer = new BufferedWriter(new FileWriter(path))) {
            for(List<Vector2f> points : pointMap.values()) {
                for(Vector2f p : points) {
                    writer.write(Float.toString(p.x));
                    writer.write(" ");
                    writer.write(Float.toString(p.y));
                    writer.write("\n");
                }

                writer.write("---\n");
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }


    public void scalePoints(int polygonIndex, float scale) {
        List<Vector2f> points = getPoints(polygonIndex);

        // Find center
        Vector2f center = new Vector2f();
        for(Vector2f p : points)
            center.addLocal(p);
        center.divideLocal(points.size());

        for(Vector2f p : points) {
            p.subtractLocal(center);
            p.multLocal(scale);
            p.addLocal(center);
        }

        updatePoints();
    }

    public void reversePoints(int polygonIndex) {
        List<Vector2f> points = getPoints(polygonIndex);

        List<Vector2f> copy = new ArrayList<>(points);
        points.clear();
        for(int i=copy.size()-1; i>=0; --i)
            points.add(copy.get(i));

        updatePoints();
    }

    public void subdivide(int polygonIndex) {
        ArrayList<Vector2f> points = getPoints(polygonIndex);

        List<Vector2f> copy = new ArrayList<>(points);
        points.clear();
        points.ensureCapacity(copy.size() * 2);

        final Vector2f midway = new Vector2f();
        final Vector2f last = new Vector2f(copy.get(0));
        points.add(last.clone());

        for(int i=1; i<copy.size(); ++i) {
            Vector2f current = copy.get(i);
            midway.set(current).subtractLocal(last).multLocal(0.5f).addLocal(last);

            points.add(midway.clone());
            points.add(current.clone());

            last.set(current);
        }

        // Close loop
        Vector2f current = copy.get(0);
        midway.set(current).subtractLocal(last).multLocal(0.5f).addLocal(last);
        points.add(midway.clone());

        updatePoints();
    }

    public void perturb(int polygonIndex) {
        List<Vector2f> points = getPoints(polygonIndex);

        float max = 0.03f;
        for(Vector2f p : points) {
            p.x += (FastMath.nextRandomFloat()*2-1) * max;
            p.y += (FastMath.nextRandomFloat()*2-1) * max;
        }

        updatePoints();
    }


    private void centerView() {
        Vector2f min = new Vector2f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector2f max = new Vector2f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        boolean hasPoints = false;

        for(List<Vector2f> points : pointMap.values()) {
            for(Vector2f p : points) {
                hasPoints = true;
                min.x = Math.min(min.x, p.x);
                min.y = Math.min(min.y, p.y);

                max.x = Math.max(max.x, p.x);
                max.y = Math.max(max.y, p.y);
            }
        }

        if(hasPoints) {
            Vector2f center = max.subtract(min);
            center.multLocal(0.5f).addLocal(min);
            panZoomState.setPos(center);
        }
    }


    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if(isPressed)
                onPressed(name);
        }

        private void onPressed(String name) {
            switch(name) {
                case ACT_RESET_POINTS:
                    pointMap.clear();
                    currentPolygon = 0;
                    updatePoints();
                    break;

                case ACT_SUBDIVIDE:
                    subdivide(currentPolygon);
                    break;

                case ACT_PERTURB:
                    perturb(currentPolygon);
                    break;

                case ACT_RELOAD_FILE:
                    importPoints(currentFile);
                    break;
            }
        }
    };


    @SuppressWarnings("override")
    private final RawInputListener numberInputListener = new RawInputListener() {
        @Override
        public void onKeyEvent(KeyInputEvent evt) {
            // Select polygon
            int p = evt.getKeyCode() - KeyInput.KEY_F1;
            if(p >= 0 && p <= 12) {
                currentPolygon = p;
                updatePoints();
                return;
            }

            // Import
            int c = evt.getKeyCode() - KeyInput.KEY_1 + 1;
            if(c >= 1 && c <= 9) {
                String file = "bug" + c + ".points";
                importPoints(file);
            }
        }

        public void beginInput() {}
        public void endInput() {}
        public void onJoyAxisEvent(JoyAxisEvent evt) {}
        public void onJoyButtonEvent(JoyButtonEvent evt) {}
        public void onMouseMotionEvent(MouseMotionEvent evt) {}
        public void onMouseButtonEvent(MouseButtonEvent evt) {}
        public void onTouchEvent(TouchEvent evt) {}
    };
}
