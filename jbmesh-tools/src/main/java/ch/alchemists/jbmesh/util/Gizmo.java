package ch.alchemists.jbmesh.util;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;

public class Gizmo extends Node {
    private static final Mesh MESH = new Mesh();
    static {
        MESH.setBuffer(VertexBuffer.Type.Position, 3, new float[] {
            0, 0, 0,
            1, 0, 0,
            0, 0, 0,
            0, 1, 0,
            0, 0, 0,
            0, 0, 1
        });

        MESH.setBuffer(VertexBuffer.Type.Color, 4, new float[] {
            1, 0, 0, 1,
            1, 0, 0, 1,
            0, 1, 0, 1,
            0, 1, 0, 1,
            0, 0, 1, 1,
            0, 0, 1, 1
        });

        MESH.setMode(Mesh.Mode.Lines);
    }


    private static final Rectangle TEXT_BOX = new Rectangle(-0.1f, 0f, 0.2f, 0.1f);
    private static final ColorRGBA TEXT_COLOR = new ColorRGBA(1.0f, 0.2f, 0.0f, 0.8f);

    private final Geometry gizmo;
    private final BitmapText namePlate;


    public Gizmo(AssetManager assetManager, String name, float scale) {
        this(assetManager, name, new Vector3f(scale, scale, scale));
    }

    
    public Gizmo(AssetManager assetManager, String name, Vector3f scale) {
        super("GizmoNode");
        setCullHint(CullHint.Never);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        mat.getAdditionalRenderState().setLineWidth(1.3f);
        mat.setBoolean("VertexColor", true);

        gizmo = new Geometry("Gizmo", MESH);
        gizmo.setMaterial(mat);
        attachChild(gizmo);

        if(name != null && !name.isEmpty()) {
            BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
            font.getPage(0).getAdditionalRenderState().setDepthTest(false);

            namePlate = new BitmapText(font);
            namePlate.setText(name);
            namePlate.setSize(0.05f);
            namePlate.setColor(TEXT_COLOR);
            namePlate.setBox(TEXT_BOX);
            namePlate.setAlignment(BitmapFont.Align.Center);
            namePlate.setQueueBucket(RenderQueue.Bucket.Translucent);

            BillboardControl billboardControl = new BillboardControl();
            namePlate.addControl(billboardControl);
            attachChild(namePlate);
        } else {
            namePlate = null;
        }

        setScale(scale);
    }


    public void setScale(float scale) {
        setScale(new Vector3f(scale, scale, scale));
    }

    public void setScale(Vector3f scale) {
        gizmo.setLocalScale(scale);

        if(namePlate != null)
            namePlate.setLocalTranslation(0, scale.y + 0.07f, 0);
    }
}

