package meshlib;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Torus;
import com.jme3.system.AppSettings;
import meshlib.conversion.*;
import meshlib.data.BMeshProperty;
import meshlib.data.property.ColorProperty;
import meshlib.operator.*;
import meshlib.operator.normalgen.NormalGenerator;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Vertex;
import meshlib.util.Profiler;


public class Main extends SimpleApplication {
    private final Node node = new Node();

    @Override
    public void simpleInitApp() {
        //Mesh in = new Torus(128, 128, 2.0f, 4.0f);
        //Mesh in = new Torus(32, 24, 1.2f, 2.5f);
        //Mesh in = new Sphere(32, 32, 5.0f);
        //Mesh in = new Sphere(12, 12, 5.0f);
        Mesh in = new Box(1, 1, 1);
        //Mesh in = new Quad(1.0f, 1.0f);
        //Mesh in = loadModel();

        BMesh bmesh;
        try(Profiler p = Profiler.start("Import")) {
            //BMesh bmesh = Import.convertGridMapped(in); // TODO: Wrong results!
            //BMesh bmesh = Import.convertSortMapped(in);
            bmesh = Import.convertExactMapped(in);
            //bmesh = TestMesh.testSphere();
        }

        try(Profiler p = Profiler.start("Processing")) {
            MeshOps.mergePlanarFaces(bmesh);
            processMesh(bmesh);
        }
        bmesh.compactData();

        NormalGenerator normGen = new NormalGenerator(bmesh, 40);
        try(Profiler p = Profiler.start("Norm Gen")) {
            normGen.apply();
        }

        //Spatial obj = createDebugMesh(bmesh);
        Spatial obj = createMesh(bmesh);
        cam.lookAt(obj.getWorldBound().getCenter(), Vector3f.UNIT_Y);
        node.attachChild(obj);

        //node.attachChild(createNormalVis(bmesh));
        rootNode.attachChild(node);

        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(0.7f)));
        rootNode.addLight(new DirectionalLight(new Vector3f(-0.7f, -1, -0.9f).normalizeLocal(), ColorRGBA.White));
        //rootNode.addLight(new DirectionalLight(new Vector3f(0.7f, 1, 0.9f).normalizeLocal(), ColorRGBA.Yellow));

        flyCam.setMoveSpeed(5);
        cam.setLocation(new Vector3f(0, 0, 5));
        cam.setFrustumPerspective(60, (float)cam.getWidth()/cam.getHeight(), 0.01f, 100f);
        viewPort.setBackgroundColor(hsb(0.75f, 0.2f, 0.15f));
    }


    private Mesh loadModel() {
        assetManager.registerLocator("assets/", FileLocator.class);
        Node model = (Node) assetManager.loadModel("Models/Jaime.j3o");
        return ((Geometry) model.getChild(0)).getMesh();
    }


    private void processMesh(BMesh bmesh) {
        // Inset
        // TODO: This Inset operator doesn't create nice topology and that's probably the reason why the normals aren't smooth.
        //       Insead, it should subdive the face 2 times and use the resulting vertices for forming the inset. -> Make nice quad strips
        Inset inset = new Inset(bmesh, 0.6f, -0.4f);
        ScaleFace scale = new ScaleFace(bmesh, 0.8f);
        for(Face face : bmesh.faces().getAll()) {
            //if(Math.random() > 0.03f) continue;
            inset.apply(face);
            scale.apply(face);
            inset.apply(face);
            scale.apply(face);
        }

        SubdivideFace subdiv = new SubdivideFace(bmesh, 2);
        subdiv.apply(bmesh.faces().getAll());

        try(Profiler p = Profiler.start("Catmull-Clark")) {
            Smooth smooth = new Smooth(bmesh);
            for(int i = 0; i < 3; ++i)
                smooth.apply(bmesh.faces().getAll());

            // TODO: Make faces planar after smoothing?
        }

        // TODO: Operator for removing collinear loops (those that were generated using the edge split above)
        //       It would collapse all vertices which lie between exactly two collinear edges.
    }


    private Geometry createMesh(BMesh bmesh) {
        ColorProperty<Vertex> propVertexColor = new ColorProperty<>(BMeshProperty.Vertex.COLOR);
        bmesh.vertices().addProperty(propVertexColor);

        ColorRGBA color = hsb(0.25f, 0.1f, 0.5f);
        for(Vertex v : bmesh.vertices()) {
            //color = hsb((float)Math.random(), 0.8f, 0.8f);
            propVertexColor.set(v, color.r, color.g, color.b, color.a);
        }

        Export export = new TriangleExport(bmesh);
        //Export export = new LineExport(bmesh);
        try(Profiler p = Profiler.start("Export")) {
            export.update();
        }

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        mat.setBoolean("UseVertexColor", true);

        Geometry geom = new Geometry("Geom", export.getMesh());
        geom.setMaterial(mat);
        return geom;
    }

    
    private Geometry createDebugMesh(BMesh bmesh) {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseVertexColor", true);
        //mat.getAdditionalRenderState().setWireframe(true);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

        DebugMeshExport export = new DebugMeshExport();
        export.apply(bmesh);
        Geometry geom = new Geometry("Geom", export.createMesh());
        geom.setMaterial(mat);

        return geom;
    }


    private Node createNormalVis(BMesh bmesh) {
        Material matNormals = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matNormals.setBoolean("VertexColor", true);
        matNormals.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        Node node = new Node("Normals");

        /*Geometry geomNormals = new Geometry("GeomNormals", DebugMeshExport.createNormals(bmesh, 0.1f));
        geomNormals.setMaterial(matNormals);
        node.attachChild(geomNormals);*/

        Geometry geomLoopNormals = new Geometry("GeomLoopNormals", DebugMeshExport.createLoopNormals(bmesh, 0.1f));
        geomLoopNormals.setMaterial(matNormals);
        geomLoopNormals.setQueueBucket(RenderQueue.Bucket.Translucent);
        node.attachChild(geomLoopNormals);

        return node;
    }
    

    @Override
    public void simpleUpdate(float tpf) {
        final float speed = 0.3f;
        node.rotate(0, speed * tpf, 0);
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
		int i = (int) hh;
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
        /*PropertyAccessTest pa = new PropertyAccessTest(bmesh);
        pa.shouldWork();
        pa.shouldFailAtRuntime();*/

        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        settings.setFrameRate(200);

        Main app = new Main();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
}
