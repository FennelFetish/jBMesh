package ch.alchemists.jbmesh.util;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.InputListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class DebugVisualState extends BaseAppState {
    private static final String ACTION_HISTORY_NEXT = "ACTION_HISTORY_NEXT";
    private static final String ACTION_HISTORY_PREV = "ACTION_HISTORY_PREV";

    private final Node node = new Node("DebugVisualState");

    private Spatial currentVis = null;
    private int currentIndex = 0;


    public DebugVisualState(String name) {
        super(name);
    }


    @Override
    protected void initialize(Application app) {
        InputManager inputManager = app.getInputManager();
        inputManager.addMapping(ACTION_HISTORY_NEXT, new KeyTrigger(KeyInput.KEY_NUMPAD3));
        inputManager.addMapping(ACTION_HISTORY_PREV, new KeyTrigger(KeyInput.KEY_NUMPAD1));
        inputManager.addListener(listener, ACTION_HISTORY_NEXT, ACTION_HISTORY_PREV);
    }

    @Override
    protected void cleanup(Application app) {

    }


    @Override
    protected void onEnable() {
        ((SimpleApplication)getApplication()).getRootNode().attachChild(node);
        reset();
    }

    @Override
    protected void onDisable() {
        ((SimpleApplication)getApplication()).getRootNode().detachChild(node);
    }


    public void reset() {
        updateVis(0);
    }

    public void updateVis() {
        updateVis(currentIndex);
    }

    private void updateVis(int index) {
        if(getApplication() == null)
            return;

        if(index < 0)
            return;

        DebugVisual vis = DebugVisual.get(getId(), index);
        if(vis == null)
            return;
        currentIndex = index;

        if(currentVis != null) {
            node.detachChild(currentVis);
        }

        currentVis = vis.createNode(getApplication().getAssetManager());
        node.attachChild(currentVis);
    }


    private final InputListener listener = (ActionListener) (String name, boolean isPressed, float tpf) -> {
        if(!isPressed)
            return;

        switch(name) {
            case ACTION_HISTORY_NEXT:
                updateVis(currentIndex + 1);
                break;

            case ACTION_HISTORY_PREV:
                updateVis(currentIndex - 1);
                break;
        }
    };
}
