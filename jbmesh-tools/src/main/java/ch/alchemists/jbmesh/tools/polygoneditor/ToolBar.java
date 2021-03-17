package ch.alchemists.jbmesh.tools.polygoneditor;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import java.util.ArrayList;
import java.util.List;

class ToolBar extends BaseAppState {
    private static final String ACT_MOUSE_CLICK = "ACT_MOUSE_CLICK";

    private final List<PolygonEditorTool> tools = new ArrayList<>();
    private PolygonEditorTool activeTool = null;

    private final PolygonEditorState editor;
    private final Container toolContainer = new Container();
    private final Container propertyContainer = new Container();


    public ToolBar(PolygonEditorState editor) {
        this.editor = editor;
    }


    @Override
    protected void initialize(Application app) {
        tools.add(new AddPointTool(editor));
        tools.add(new RemovePointTool(editor, app));

        SimpleApplication simpleApp = (SimpleApplication) app;
        simpleApp.getGuiNode().attachChild(toolContainer);
        simpleApp.getGuiNode().attachChild(propertyContainer);

        rebuild();
        reposition();

        app.getGuiViewPort().addProcessor(resizeListener);

        app.getInputManager().addMapping(ACT_MOUSE_CLICK, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(actionListener, ACT_MOUSE_CLICK);
    }


    @Override
    protected void cleanup(Application app) {
        SimpleApplication simpleApp = (SimpleApplication) app;
        simpleApp.getGuiNode().detachChild(toolContainer);
        simpleApp.getGuiNode().detachChild(propertyContainer);

        app.getGuiViewPort().removeProcessor(resizeListener);
    }


    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}


    @Override
    public void update(float tpf) {
        if(activeTool != null) {
            activeTool.update(tpf);
        }
    }


    private void rebuild() {
        toolContainer.detachAllChildren();

        Button btnClearTool = new Button("No Tool");
        btnClearTool.addClickCommands((Button source) -> {
            selectTool(null);
        });
        toolContainer.addChild(btnClearTool);

        for(PolygonEditorTool tool : tools) {
            Button btn = new Button(tool.getName());
            btn.addClickCommands((Button source) -> {
                selectTool(tool);
            });
            toolContainer.addChild(btn);
        }
    }


    private void reposition() {
        int h = getApplication().getCamera().getHeight();
        toolContainer.setLocalTranslation(0, h, 0);
    }


    private void selectTool(PolygonEditorTool tool) {
        if(activeTool != null)
            activeTool.onDeactivate();

        propertyContainer.detachAllChildren();
        activeTool = tool;

        if(tool != null) {
            tool.onActivate();

            for(ToolProperty property : tool.getProperties())
                propertyContainer.addChild(property.getGuiElement());
            propertyContainer.setLocalTranslation(0, 200, 0);
        }
    }



    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            switch(name) {
                case ACT_MOUSE_CLICK:
                    if(activeTool != null) {
                        if(isPressed)
                            activeTool.mouseDown(editor.getCursorPosition(), 0);
                        else
                            activeTool.mouseUp(editor.getCursorPosition(), 0);
                    }
                    break;
            }
        }
    };


    @SuppressWarnings("override")
    private final SceneProcessor resizeListener = new SceneProcessor() {
        private boolean initialized = false;

        @Override
        public void initialize(RenderManager rm, ViewPort vp) {
            initialized = true;
        }

        @Override
        public void reshape(ViewPort vp, int w, int h) {
            reposition();
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }


        public void preFrame(float tpf) {}
        public void postQueue(RenderQueue rq) {}
        public void postFrame(FrameBuffer out) {}
        public void cleanup() {}
        public void setProfiler(AppProfiler profiler) {}
    };
}
