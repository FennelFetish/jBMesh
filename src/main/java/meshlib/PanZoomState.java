package meshlib;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class PanZoomState extends BaseAppState {
    private static final int MOUSE_BACK = 3;
    private static final int MOUSE_FORWARD = 4;

    private static final String ACT_PAN = "ACTION_PAN";
    private static final String ACT_ZOOM_IN = "ACT_ZOOM_IN";
    private static final String ACT_ZOOM_OUT = "ACT_ZOOM_OUT";

    private static final float ZOOM_FACTOR_STEP = 1.33f;

    private float initialZ = 5;
    private Camera cam;

    private Vector2f panStart = null;
    private InputHandler inputHandler;


    public PanZoomState() {
        this(5);
    }

    public PanZoomState(float initialZ) {
        this.initialZ = initialZ;
    }


    @Override
    protected void initialize(Application app) {
        cam = app.getCamera();
        Vector3f loc = cam.getLocation();
        loc.z = initialZ;
        cam.setLocation(loc);
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        InputManager inputManager = app.getInputManager();
        inputManager.addMapping(ACT_PAN, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        //inputManager.addMapping(ACT_ZOOM_IN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        //inputManager.addMapping(ACT_ZOOM_OUT, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping(ACT_ZOOM_IN, new KeyTrigger(KeyInput.KEY_ADD), new MouseButtonTrigger(MOUSE_FORWARD));
        inputManager.addMapping(ACT_ZOOM_OUT, new KeyTrigger(KeyInput.KEY_SUBTRACT), new MouseButtonTrigger(MOUSE_BACK));

        inputHandler = new InputHandler();
        inputManager.addListener(inputHandler, ACT_PAN, ACT_ZOOM_IN, ACT_ZOOM_OUT);
    }

    @Override
    protected void cleanup(Application app) {
        cam = null;

        getApplication().getInputManager().removeListener(inputHandler);
        inputHandler = null;
    }


    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}


    @Override
    public void update(float tpf) {
        // Pan
        if(panStart != null) {
            Vector2f cursor = getApplication().getInputManager().getCursorPosition();
            Vector3f loc = cam.getLocation();

            Vector2f diff = panStart.subtract(cursor);
            diff.multLocal(0.00115f * loc.z);
            loc.x += diff.x;
            loc.y += diff.y;

            cam.setLocation(loc);
            panStart.set(cursor);
        }
    }


    private void zoom(float factor) {
        // TODO: Zoom into cursor position
        Vector3f loc = cam.getLocation();
        loc.z *= factor;
        cam.setLocation(loc);
    }


    private class InputHandler implements ActionListener {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if(!isEnabled())
                return;

            switch(name) {
                case ACT_ZOOM_IN:
                    if(isPressed)
                        zoom(1.0f / ZOOM_FACTOR_STEP);
                    break;

                case ACT_ZOOM_OUT:
                    if(isPressed)
                        zoom(ZOOM_FACTOR_STEP);
                    break;

                case ACT_PAN:
                    if(isPressed)
                        panStart = getApplication().getInputManager().getCursorPosition().clone();
                    else
                        panStart = null;
                    break;
            }
        }
    }
}
