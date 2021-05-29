package ch.alchemists.jbmesh.examples;

import ch.alchemists.jbmesh.conversion.BMeshJmeExport;
import ch.alchemists.jbmesh.operator.ExtrudePath;
import ch.alchemists.jbmesh.operator.normalgen.NormalGenerator;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.BasicShapes;
import ch.alchemists.jbmesh.util.Gizmo;
import ch.alchemists.jbmesh.util.PlanarCoordinateSystem;
import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.system.AppSettings;

public class ExtrudeHelix extends SimpleApplication {
    private final float helixRadius   = 1.0f;
    private final float helixLength   = 5f;
    private final int rounds          = 5;
    private final int roundSamples    = 48;


    private Face createDisc(BMesh bmesh, PlanarCoordinateSystem coordSys) {
        float radius = 0.25f;
        int samples  = 12;

        Face face = BasicShapes.createDisc(bmesh, coordSys, samples, radius);
        return face;
    }


    private Face createCross(BMesh bmesh, PlanarCoordinateSystem coordSys) {
        float thickness = 0.1f / 2f;
        float length    = 0.3f;

        // Create 3D vertices on defined plane by unprojecting 2D coordinates
        Vertex[] vertices = {
            bmesh.createVertex(coordSys.unproject(-thickness, -length)),
            bmesh.createVertex(coordSys.unproject( thickness, -length)),
            bmesh.createVertex(coordSys.unproject( thickness, -thickness)),
            bmesh.createVertex(coordSys.unproject( length,    -thickness)),
            bmesh.createVertex(coordSys.unproject( length,     thickness)),
            bmesh.createVertex(coordSys.unproject( thickness,  thickness)),
            bmesh.createVertex(coordSys.unproject( thickness,  length)),
            bmesh.createVertex(coordSys.unproject(-thickness,  length)),
            bmesh.createVertex(coordSys.unproject(-thickness,  thickness)),
            bmesh.createVertex(coordSys.unproject(-length,     thickness)),
            bmesh.createVertex(coordSys.unproject(-length,    -thickness)),
            bmesh.createVertex(coordSys.unproject(-thickness, -thickness))
        };

        return bmesh.createFace(vertices);
    }


    private void extrudeHelix(BMesh bmesh, Face face, PlanarCoordinateSystem coordSys) {
        // We need to set the tangents for each extruded segment.
        // The tangents define the rotational orientation of the segment.
        ExtrudePath.PointListPath path = new ExtrudePath.PointListPath(coordSys.p) {
            @Override
            protected void setTangent(int i, Vector3f tangent, Vector3f normal) {
                // Last face normal should point upwards
                if(i == points.size()-1)
                    normal.set(Vector3f.UNIT_Y);

                tangent.set(normal).crossLocal(Vector3f.UNIT_Z).normalizeLocal();
            }
        };

        // Calculate points of helix-path
        int totalSamples  = rounds * roundSamples; // The first sample already exists (=original Face)
        float sampleZFeed = helixLength / totalSamples;
        float angleFeed   = FastMath.TWO_PI / roundSamples;
        float angle       = 0;
        Vector3f p        = new Vector3f();

        for(int i=0; i<totalSamples; ++i) {
            angle += angleFeed;

            p.x = FastMath.cos(angle) * helixRadius;
            p.y = FastMath.sin(angle) * helixRadius;
            p.z += sampleZFeed;
            path.addPoint(p.clone());
        }

        // Do the extrusion.
        // We must tell the algorithm the orientation of the original Face, hence we pass coordSys.
        ExtrudePath extrudePath = new ExtrudePath(bmesh);
        extrudePath.apply(face, coordSys, path);
        extrudePath.applyLoopTexCoords();

        // Remove face at the end
        bmesh.removeFace(face);
    }


    @Override
    public void simpleInitApp() {
        // A Face defines the basic shape of the extruded tube
        // and a PlanarCoordinateSystem defines position and orientation of this Face.
        // We build the Face at the start of the helix.
        Vector3f faceCenter = new Vector3f(helixRadius, 0, 0);
        PlanarCoordinateSystem coordSys = new PlanarCoordinateSystem();
        coordSys.withXAt(faceCenter, Vector3f.UNIT_X, Vector3f.UNIT_Y); // Tangent = [0, 0, -1]

        BMesh bmesh = new BMesh();
        //Face face = createDisc(bmesh, coordSys);
        Face face = createCross(bmesh, coordSys);

        // Create a helix by extruding the defined Face along a path
        extrudeHelix(bmesh, face, coordSys);

        // Calculate normals
        NormalGenerator normalGenerator = new NormalGenerator(bmesh, 80);
        normalGenerator.apply();

        // Convert BMesh to jme Mesh
        Mesh mesh = BMeshJmeExport.exportTriangles(bmesh);

        // Create Material
        Material mat = new Material(assetManager, Materials.LIGHTING);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        mat.setTexture("DiffuseMap", assetManager.loadTexture("Textures/colors.jpg"));
        //mat.getAdditionalRenderState().setWireframe(true);

        // Create & attach Geometry
        Geometry geom = new Geometry("Geom", mesh);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);

        // This Gizmo is placed at [0, 0, 0] and its lines have length 1.0
        rootNode.attachChild(new Gizmo(assetManager, null, 1f));

        setupLight();
        flyCam.setMoveSpeed(5);
    }


    private void setupLight() {
        ColorRGBA ambientColor = new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootNode.addLight(ambient);

        Vector3f lightDirection = new Vector3f(-0.5f, 0, -1f).normalizeLocal();
        ColorRGBA lightColor = new ColorRGBA(0.6f, 0.6f, 0.6f, 1.0f);
        DirectionalLight directional = new DirectionalLight(lightDirection, lightColor);
        rootNode.addLight(directional);
    }


    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        settings.setFrameRate(200);
        settings.setSamples(8);
        settings.setGammaCorrection(true);

        ExtrudeHelix app = new ExtrudeHelix();
        app.setSettings(settings);
        app.start();
    }
}
