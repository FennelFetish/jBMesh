package meshlib;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.shape.Box;
import java.nio.FloatBuffer;
import java.util.List;
import meshlib.conversion.MeshConverter;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;
import org.lwjgl.BufferUtils;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication {
    private final ColorRGBA color = new ColorRGBA().fromIntABGR(0);

    @Override
    public void simpleInitApp() {
        Box box = new Box(0.5f, 0.5f, 0.5f);
        BMesh mesh = MeshConverter.convert(box);

        /*Geometry geom = new Geometry("Box", b);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);

        rootNode.attachChild(geom);*/


        FloatBuffer posBuffer = BufferUtils.createFloatBuffer(100 * 3);

        ColorRGBA color = new ColorRGBA();
        color.setAsSrgb(speed, speed, speed, speed);

        Object arr = new int[10];
        int[] a = (int[]) arr;
        List<int[]> bla;
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }


    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
}
