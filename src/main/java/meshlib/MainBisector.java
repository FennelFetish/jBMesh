package meshlib;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import meshlib.conversion.LineExport;
import meshlib.operator.PolygonOffset;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Vertex;
import meshlib.util.ColorUtil;
import meshlib.util.Gizmo;

public class MainBisector extends SimpleApplication implements ActionListener {
    private final PolygonBuilder rectangle = new PolygonBuilder(
        new Vector3f(0, 0, 0),
        new Vector3f(4, 0, 0),
        new Vector3f(4, 2, 0),
        new Vector3f(0, 2, 0)
    );

    private final PolygonBuilder trapez = new PolygonBuilder(
        new Vector3f(0, 0, 0),
        new Vector3f(6, 0, 0),
        new Vector3f(4, 5, 0),
        new Vector3f(2, 5, 0)
    );

    private final PolygonBuilder angle = new PolygonBuilder(
        new Vector3f(0, 0, 0),
        new Vector3f(6, 0, 0),
        new Vector3f(6, 3, 0),
        new Vector3f(2, 2, 0),
        new Vector3f(3, 6, 0),
        new Vector3f(0, 6, 0)
    );

    private PolygonBuilder shape = rectangle;
    private float distance = 0.0f;
    private final Node node = new Node();


    private Geometry makeGeom(BMesh bmesh, AssetManager assetManager) {
        // TODO: Make util class "DebugLineExport"?
        LineExport origExport = new LineExport(bmesh);
        origExport.update();
        Mesh mesh = origExport.getMesh();

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(1, 0, 0, 1));
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

        Geometry geom = new Geometry("Geom", mesh);
        geom.setMaterial(mat);
        return geom;
    }


    private void recreateGeoms() {
        node.detachAllChildren();

        BMesh orig = new BMesh();
        Face origFace = shape.build(orig);

        BMesh result = new BMesh();
        Face resultFace = shape.build(result); // TODO: Clone face? (how to deal with connections? ignore?)

        PolygonOffset offset = new PolygonOffset(result);
        offset.setDistance(distance);
        offset.apply(resultFace);

        Geometry geomOrig = makeGeom(orig, assetManager);
        node.attachChild(geomOrig);

        Geometry geomResult = makeGeom(result, assetManager);
        node.attachChild(geomResult);
    }



    @Override
    public void simpleInitApp() {
        recreateGeoms();

        rootNode.attachChild(node);
        rootNode.attachChild(new Gizmo(assetManager, null, 1.0f));

        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(0.04f)));
        rootNode.addLight(new DirectionalLight(new Vector3f(-0.7f, -1, -1.5f).normalizeLocal(), ColorRGBA.White.mult(1.0f)));
        rootNode.addLight(new DirectionalLight(new Vector3f(0.7f, -1, 1.5f).normalizeLocal(), ColorRGBA.White.mult(0.07f)));

        flyCam.setMoveSpeed(5);
        viewPort.setBackgroundColor(ColorUtil.hsb(0.75f, 0.35f, 0.02f));
        cam.setFrustumPerspective(60, (float)cam.getWidth()/cam.getHeight(), 0.01f, 100f);

        inputManager.addMapping("DISTANCE+", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("DISTANCE-", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping("POLY1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("POLY2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("POLY3", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addListener(this, "DISTANCE+", "DISTANCE-", "POLY1", "POLY2", "POLY3");
    }



    @Override
    public void simpleUpdate(float tpf) {

    }


    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch(name) {
            case "DISTANCE+":
                distance += 0.1f;
                break;

            case "DISTANCE-":
                distance -= 0.1f;
                break;

            case "POLY1":
                shape = rectangle;
                break;

            case "POLY2":
                shape = trapez;
                break;

            case "POLY3":
                shape = angle;
                break;
        }


        System.out.println("Distance: " + distance);
        recreateGeoms();
    }



    private static class PolygonBuilder {
        private final Vector3f[] positions;

        private PolygonBuilder(Vector3f... positions) {
            this.positions = positions;
        }

        public Face build(BMesh bmesh) {
            Vertex[] vertices = new Vertex[positions.length];
            for(int i=0; i<positions.length; ++i)
                vertices[i] = bmesh.createVertex(positions[i]);

            Face face = bmesh.createFace(vertices);
            return face;
        }
    }


    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        settings.setFrameRate(200);
        settings.setSamples(8);
        settings.setGammaCorrection(true);

        MainBisector app = new MainBisector();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }


}
