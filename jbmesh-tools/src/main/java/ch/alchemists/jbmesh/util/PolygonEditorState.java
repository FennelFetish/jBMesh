package ch.alchemists.jbmesh.util;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.*;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PolygonEditorState extends BaseAppState {
    public interface PointListener {
        void onPointsReset();
        void onPointsUpdated(List<Vector2f> points);
    }

    public static class PointDrawType {
        public final Node container = new Node("Point Container");
        public final Mesh mesh;
        public final Material mat;
        public final float fontSize;

        public ColorRGBA textColor = new ColorRGBA(1, 0.2f, 0.2f, 1.0f);

        public PointDrawType(AssetManager assetManager, ColorRGBA color, float pointSize, float fontSize) {
            mesh = new Sphere(8, 16, pointSize*0.5f);

            mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", color);

            this.fontSize = fontSize;
        }
    }


    private static final String ACT_ADD_POINT    = "ACT_ADD_POINT";
    private static final String ACT_REMOVE_POINT = "ACT_REMOVE_POINT";
    private static final String ACT_RESET_POINTS = "ACT_RESET_POINTS";
    private static final String ACT_SUBDIVIDE    = "ACT_SUBDIVIDE";
    private static final String ACT_RELOAD_FILE  = "ACT_RELOAD_FILE";

    private static final String DEFAULT_EXPORT_FILE = "last.points";
    private Path storageBasePath = null;
    private String currentFile = DEFAULT_EXPORT_FILE;

    private static final float BG_SIZE = 5000;
    private Geometry bg;

    private final PointListener listener;
    private PanZoomState panZoomState;

    private BitmapFont font;
    private PointDrawType defaultPointType;

    private final Plane plane;
    private final ArrayList<Vector2f> points = new ArrayList<>();

    private boolean snapToGrid = true;

    private static final float AUTODRAW_INTERVAL = 0.2f;
    private boolean autoDraw = false;
    private float tDraw = 0;
    private Vector2f mouseDownPos;


    public PolygonEditorState(PointListener listener) {
        this.listener = listener;
        plane = new Plane(Vector3f.UNIT_Z.clone(), new Vector3f(1, 0, 0));
    }


    public List<Vector2f> getPoints() {
        return Collections.unmodifiableList(points);
    }


    public void setStoragePath(String path) {
        storageBasePath = Paths.get(path).toAbsolutePath().normalize();
    }


    @Override
    protected void initialize(Application app) {
        AssetManager assetManager = app.getAssetManager();
        Node rootNode = ((SimpleApplication)app).getRootNode();

        panZoomState = new PanZoomState(25);
        app.getStateManager().attach(panZoomState);

        setupBackground(assetManager, rootNode);
        setupInput(app.getInputManager());

        font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        defaultPointType = new PointDrawType(assetManager, ColorRGBA.Red, 0.2f, 0.3f);
        rootNode.attachChild(defaultPointType.container);

        Gizmo gizmo = new Gizmo(assetManager, "", 1.0f);
        rootNode.attachChild(gizmo);

        updatePoints();
        centerView();
    }

    private void setupBackground(AssetManager assetManager, Node rootNode) {
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
        getApplication().getViewPort().setBackgroundColor(new ColorRGBA(0.04f, 0.03f, 0.04f, 1.0f));
    }

    private void setupInput(InputManager inputManager) {
        inputManager.addMapping(ACT_ADD_POINT, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(ACT_REMOVE_POINT, new KeyTrigger(KeyInput.KEY_DELETE));
        inputManager.addMapping(ACT_RESET_POINTS, new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping(ACT_SUBDIVIDE, new KeyTrigger(KeyInput.KEY_X));
        inputManager.addMapping(ACT_RELOAD_FILE, new KeyTrigger(KeyInput.KEY_HOME));

        inputManager.addListener(actionListener, ACT_ADD_POINT, ACT_REMOVE_POINT, ACT_RESET_POINTS, ACT_SUBDIVIDE, ACT_RELOAD_FILE);
        inputManager.addRawInputListener(numberInputListener);
    }


    @Override
    protected void cleanup(Application app) {
        app.getStateManager().detach(panZoomState);
        panZoomState = null;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}


    @Override
    public void update(float tpf) {
        if(mouseDownPos == null)
            return;

        if(autoDraw) {
            tDraw += tpf;
            if(tDraw >= AUTODRAW_INTERVAL) {
                tDraw = 0;
                addPoint();
            }
        }
        else {
            // Enable auto draw if cursor moves 3px away from mouseDownPos
            Vector2f cursorPos = getApplication().getInputManager().getCursorPosition();
            float e2 = 3 * 3;

            float dist2 = cursorPos.distanceSquared(mouseDownPos);
            if(dist2 >= e2)
                autoDraw = true;
        }
    }


    private Vector2f pick() {
        Camera cam = getApplication().getCamera();

        Vector2f cursor = getApplication().getInputManager().getCursorPosition();
        Vector3f near   = cam.getWorldCoordinates(cursor, 0.0f);
        Vector3f far    = cam.getWorldCoordinates(cursor, 1.0f);
        Vector3f dir    = far.subtract(near).normalizeLocal();

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

        updatePoints();
        exportPoints(DEFAULT_EXPORT_FILE);
    }

    private void removePoint() {
        final float e = 0.5f; //0.001f;
        final float e2 = e*e;

        Vector2f pick = pick();

        for(int i=0; i<points.size(); ++i) {
            if(pick.distanceSquared(points.get(i)) < e2) {
                points.remove(i);
                break;
            }
        }

        updatePoints();
        exportPoints(DEFAULT_EXPORT_FILE);
    }


    private void updatePoints() {
        defaultPointType.container.detachAllChildren();

        for(int i=0; i<points.size(); ++i) {
            Vector2f p = points.get(i);
            createPointVis(defaultPointType, p, Integer.toString(i+1));
        }

        listener.onPointsUpdated(getPoints());
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

    public void importDefaultPoints() {
        importPoints(DEFAULT_EXPORT_FILE);
    }

    public void importPoints(String file) {
        currentFile = file;
        String path = getFilePath(file);
        if(path == null)
            return;

        try(BufferedReader reader = new BufferedReader(new FileReader(path))) {
            points.clear();

            for(String line; (line = reader.readLine()) != null; ) {
                String[] split = line.split(" ");

                Vector2f p = new Vector2f();
                p.x = Float.parseFloat(split[0]);
                p.y = Float.parseFloat(split[1]);
                points.add(p);
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

    public void exportPoints(String file) {
        String path = getFilePath(file);
        if(path == null)
            return;

        try(Writer writer = new BufferedWriter(new FileWriter(path))) {
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


    public void scalePoints(float scale) {
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

    public void reversePoints() {
        List<Vector2f> copy = new ArrayList<>(points);
        points.clear();
        for(int i=copy.size()-1; i>=0; --i)
            points.add(copy.get(i));

        updatePoints();
    }

    public void subdivide() {
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


    private void centerView() {
        Vector2f min = new Vector2f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector2f max = new Vector2f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

        for(Vector2f p : points) {
            min.x = Math.min(min.x, p.x);
            min.y = Math.min(min.y, p.y);

            max.x = Math.max(max.x, p.x);
            max.y = Math.max(max.y, p.y);
        }

        if(!points.isEmpty()) {
            Vector2f center = max.subtract(min);
            center.multLocal(0.5f).addLocal(min);
            panZoomState.setPos(center);
        }
    }


    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            switch(name) {
                case ACT_ADD_POINT:
                    if(isPressed) {
                        addPoint();
                        mouseDownPos = getApplication().getInputManager().getCursorPosition().clone();
                    } else {
                        mouseDownPos = null;
                        autoDraw = false;
                    }
                    return;
            }

            if(isPressed)
                onPressed(name);
        }

        private void onPressed(String name) {
            switch(name) {
                case ACT_REMOVE_POINT:
                    removePoint();
                    break;

                case ACT_RESET_POINTS:
                    points.clear();
                    updatePoints();
                    break;

                case ACT_SUBDIVIDE:
                    subdivide();
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
