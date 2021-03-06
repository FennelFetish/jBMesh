// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.tools.polygoneditor;

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

    private static final float ZOOM_FACTOR_STEP = 1.5f; //1.33f;

    private Vector3f initialPos = new Vector3f(18, 9, 5);
    private Camera cam;

    private Vector2f panStart = null;


    public PanZoomState() {}

    public PanZoomState(float initialZ) {
        initialPos.z = initialZ;
    }


    public void setPos(Vector2f pos) {
        initialPos.x = pos.x;
        initialPos.y = pos.y;

        if(cam != null)
            cam.setLocation(initialPos);
    }


    @Override
    protected void initialize(Application app) {
        cam = app.getCamera();
        cam.setLocation(initialPos);
        cam.lookAt(new Vector3f(initialPos.x, initialPos.y, 0), Vector3f.UNIT_Y);
        cam.setFrustumPerspective(45.0f, (float)cam.getWidth()/cam.getHeight(), 1.0f, 10000);

        InputManager inputManager = app.getInputManager();
        inputManager.addMapping(ACT_PAN, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        //inputManager.addMapping(ACT_ZOOM_IN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        //inputManager.addMapping(ACT_ZOOM_OUT, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping(ACT_ZOOM_IN, new KeyTrigger(KeyInput.KEY_ADD), new MouseButtonTrigger(MOUSE_FORWARD));
        inputManager.addMapping(ACT_ZOOM_OUT, new KeyTrigger(KeyInput.KEY_SUBTRACT), new MouseButtonTrigger(MOUSE_BACK));

        inputManager.addListener(actionListener, ACT_PAN, ACT_ZOOM_IN, ACT_ZOOM_OUT);
    }

    @Override
    protected void cleanup(Application app) {
        cam = null;

        getApplication().getInputManager().removeListener(actionListener);
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


    private final ActionListener actionListener = (String name, boolean isPressed, float tpf) -> {
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
    };
}
