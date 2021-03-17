package ch.alchemists.jbmesh.tools.polygoneditor;

import ch.alchemists.jbmesh.util.BasicShapes;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

class RemovePointTool extends PolygonEditorTool {
    private static final float REMOVE_INTERVAL = 0.1f;

    private final PolygonEditorState editor;
    private float size = 3.0f;

    private boolean removing = false;
    private float tDraw = 0;

    private final Node rootNode;
    private final Geometry selectionCircle;


    RemovePointTool(PolygonEditorState editor, Application app) {
        super("Remove Points");
        this.editor = editor;

        rootNode = ((SimpleApplication) app).getRootNode();
        selectionCircle = createSelectionCircle(app.getAssetManager());

        addProperty(new ToolProperty.FloatProperty("Radius", size, 0.1f, 10.0f, val -> {
            size = val;
            selectionCircle.setLocalScale(size);
        }));
    }


    private static Geometry createSelectionCircle(AssetManager assetManager) {
        Material mat = new Material(assetManager, Materials.UNSHADED);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.setColor("Color", new ColorRGBA(1, 0, 0, 0.1f));

        Geometry circle = new Geometry("RemovePointTool Circle", BasicShapes.disc(64, 1.0f));
        circle.setQueueBucket(RenderQueue.Bucket.Translucent);
        circle.setMaterial(mat);
        return circle;
    }


    private void removePointsInRange(Vector2f center) {
        float distSquared = size*size;
        editor.getPoints().removeIf((Vector2f p) -> p.distanceSquared(center) <= distSquared);
        editor.updatePoints();
        editor.exportToDefaultFile();
    }


    @Override
    public void onActivate() {
        rootNode.attachChild(selectionCircle);
        selectionCircle.setLocalScale(size);
    }

    @Override
    public void onDeactivate() {
        rootNode.detachChild(selectionCircle);
    }


    @Override
    public void mouseDown(Vector2f cursor, int button) {
        removing = true;
        tDraw = REMOVE_INTERVAL;
    }

    @Override
    public void mouseUp(Vector2f cursor, int button) {
        removing = false;
    }


    @Override
    public void updateTool(float tpf) {
        Vector2f pick = editor.pick(editor.getCursorPosition());
        selectionCircle.setLocalTranslation(pick.x, pick.y, 0);

        if(!removing)
            return;

        tDraw += tpf;
        if(tDraw >= REMOVE_INTERVAL) {
            removePointsInRange(pick);
            tDraw = 0;
        }
    }
}
