package meshlib;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import meshlib.conversion.MeshConverter;
import meshlib.structure.BMesh;
import meshlib.structure.BMeshData;
import meshlib.structure.BMeshProperty;
import meshlib.structure.Vertex;
import meshlib.util.BMeshVisualization;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication {
    private final ColorRGBA color = new ColorRGBA().fromIntABGR(0);

    @Override
    public void simpleInitApp() {
        Box box = new Box(1f, 1f, 1f);
        BMesh bmesh = MeshConverter.convert(box);
        bmesh.loopData().createProperty("Bla", BMeshData.PropertyType.Long, 14);
        bmesh.compactData();
        bmesh.edgeData().createProperty("Bloo", BMeshData.PropertyType.String, 2);

        BMeshData<Vertex>.Property propVertexColor = bmesh.vertexData().createProperty(BMeshProperty.Vertex.COLOR, BMeshData.PropertyType.Float, 3);
        float hue = 0;
        for(Vertex v : bmesh.vertices()) {
            ColorRGBA color = hsb(hue, 1.0f, 1.0f);
            hue += 0.71f;
            if(hue > 1.0f)
                hue -= 1.0f;
            propVertexColor.setVec3(v, color.r, color.g, color.b);
        }

        Mesh mesh = BMeshVisualization.create(bmesh);

        Geometry geom = new Geometry("Geom", mesh);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseVertexColor", true);
        geom.setMaterial(mat);

        rootNode.attachChild(geom);
        rootNode.addLight(new AmbientLight(ColorRGBA.White));

        flyCam.setMoveSpeed(10);
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }


    private ColorRGBA hsb(float h, float s, float b) {
        ColorRGBA color = new ColorRGBA();
        color.a = 1.0f;

		if (s == 0) {
			// achromatic ( grey )
			color.r = b;
			color.g = b;
			color.b = b;
			return color;
		}

		//float hh = h / 60.0f;
        float hh = h * 6f;
		int i = (int) Math.floor(hh);
		float f = hh - i;
		float p = b * (1 - s);
		float q = b * (1 - s * f);
		float t = b * (1 - s * (1 - f));

        switch(i) {
            case 0:
                color.r = b;
                color.g = t;
                color.b = p;
                break;
            case 1:
                color.r = q;
                color.g = b;
                color.b = p;
                break;
            case 2:
                color.r = p;
                color.g = b;
                color.b = t;
                break;
            case 3:
                color.r = p;
                color.g = q;
                color.b = b;
                break;
            case 4:
                color.r = t;
                color.g = p;
                color.b = b;
                break;
            default:
                color.r = b;
                color.g = p;
                color.b = q;
                break;
        }

        return color;
    }



    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);

        Main app = new Main();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
}
