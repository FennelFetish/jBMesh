package meshlib;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Torus;
import com.jme3.system.AppSettings;
import java.util.ArrayList;
import java.util.List;
import meshlib.conversion.DebugMeshExport;
import meshlib.conversion.Import;
import meshlib.data.BMeshProperty;
import meshlib.data.property.ColorProperty;
import meshlib.operator.EdgeOps;
import meshlib.operator.Inset;
import meshlib.structure.BMesh;
import meshlib.structure.Edge;
import meshlib.structure.Face;
import meshlib.structure.Vertex;
import meshlib.conversion.Export;


public class Main extends SimpleApplication {
    @Override
    public void simpleInitApp() {
        //Mesh in = new Torus(128, 128, 2.0f, 4.0f);
        Mesh in = new Torus(32, 24, 1.2f, 2.5f);
        //Mesh in = new Sphere(32, 32, 5.0f);
        //Mesh in = new Box(1, 1, 1);

        BMesh bmesh = Import.convertGridMapped(in);
        processMesh(bmesh);
        bmesh.compactData();
        rootNode.attachChild(createDebugMesh(bmesh));
        //rootNode.attachChild(createMesh(bmesh));

        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(0.7f)));
        rootNode.addLight(new DirectionalLight(new Vector3f(-0.7f, -1, -0.9f).normalizeLocal(), ColorRGBA.White));

        flyCam.setMoveSpeed(10);
        cam.setFrustumPerspective(60, (float)cam.getWidth()/cam.getHeight(), 0.01f, 100f);
        viewPort.setBackgroundColor(hsb(0.75f, 0.2f, 0.15f));
    }


    private void processMesh(BMesh bmesh) {
        List<Edge> edges = new ArrayList<>();
        bmesh.edges().forEach(e -> edges.add(e));

        // Edge split
        EdgeOps edgeOps = new EdgeOps(bmesh);
        for(Edge e : edges) {
            Vertex vert = edgeOps.splitAtCenter(e);

            // Revert split
            Edge newEdge = e.getNextEdge(vert);
            assert newEdge != e;
            if(!bmesh.joinEdge(newEdge, vert))
                throw new RuntimeException();
        }

        // Second edge split
        edges.clear();
        bmesh.edges().forEach(e -> edges.add(e));

        for(Edge e : edges) {
            Vertex vert = edgeOps.splitAtCenter(e);
        }

        // Invert faces
        /*for(Face f : bmesh.faces()) {
            bmesh.invertFace(f);
            //bmesh.invertFace(f);
        }*/

        // Inset
        List<Face> faces = new ArrayList<>();
        bmesh.faces().forEach(f -> faces.add(f));

        Inset inset = new Inset(bmesh, 0.7f, -0.1f);
        for(Face face : faces)
            inset.apply(face);
    }


    private Geometry createMesh(BMesh bmesh) {
        ColorProperty<Vertex> propVertexColor = new ColorProperty<>(BMeshProperty.Vertex.COLOR);
        bmesh.vertices().addProperty(propVertexColor);

        for(Vertex v : bmesh.vertices()) {
            ColorRGBA color = hsb((float)Math.random(), 0.8f, 0.8f);
            propVertexColor.set(v, color.r, color.g, color.b, color.a);
        }

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseVertexColor", true);

        Geometry geom = new Geometry("Geom", Export.create(bmesh));
        geom.setMaterial(mat);
        return geom;
    }

    
    private Node createDebugMesh(BMesh bmesh) {
        Node node = new Node("Debug");

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseVertexColor", true);
        //mat.getAdditionalRenderState().setWireframe(true);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

        DebugMeshExport export = new DebugMeshExport();
        export.apply(bmesh);
        Geometry geom = new Geometry("Geom", export.createMesh());
        geom.setMaterial(mat);
        node.attachChild(geom);

        Material matNormals = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matNormals.setBoolean("VertexColor", true);
        Geometry geomNormals = new Geometry("GeomNormals", DebugMeshExport.createNormals(bmesh, 0.33f));
        geomNormals.setMaterial(matNormals);
        node.attachChild(geomNormals);

        return node;
    }
    

    @Override
    public void simpleUpdate(float tpf) {}


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

        Main app = new Main();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
}
