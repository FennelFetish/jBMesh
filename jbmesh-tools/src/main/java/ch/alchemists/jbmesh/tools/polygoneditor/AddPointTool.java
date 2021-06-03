// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.tools.polygoneditor;

import com.jme3.math.Vector2f;

class AddPointTool extends PolygonEditorTool {
    private static final float ADD_INTERVAL = 0.2f;

    private final PolygonEditorState editor;

    private boolean snapToGrid = true;

    private boolean autoDraw = false;
    private float tDraw = 0;
    private Vector2f mouseDownPos;


    AddPointTool(PolygonEditorState editor) {
        super("Add Points");
        this.editor = editor;

        addProperty(new ToolProperty.BooleanProperty("Snap to Grid", snapToGrid, val -> {
            snapToGrid = val;
        }));
    }


    private Vector2f pickAndSnap(Vector2f cursor) {
        Vector2f pick = editor.pick(cursor);

        if(snapToGrid) {
            pick.x = Math.round(pick.x);
            pick.y = Math.round(pick.y);
        }

        return pick;
    }


    private void addPoint(Vector2f p) {
        editor.getPoints().add(p);
        editor.updatePoints();
        editor.exportToDefaultFile();
    }


    @Override
    public void mouseDown(Vector2f cursor, int button) {
        Vector2f pick = pickAndSnap(cursor);
        addPoint(pick);
        mouseDownPos = cursor.clone();
    }

    @Override
    public void mouseUp(Vector2f cursor, int button) {
        mouseDownPos = null;
        autoDraw = false;
    }


    @Override
    public void updateTool(float tpf) {
        if(mouseDownPos == null)
            return;

        Vector2f cursor = editor.getCursorPosition();

        if(autoDraw) {
            tDraw += tpf;
            if(tDraw >= ADD_INTERVAL) {
                tDraw = 0;
                addPoint(pickAndSnap(cursor));
            }
        }
        else {
            // Enable auto draw if cursor moves 3px away from mouseDownPos
            float e2 = 3 * 3;
            if(cursor.distanceSquared(mouseDownPos) >= e2)
                autoDraw = true;
        }
    }
}
