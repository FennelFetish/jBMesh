package ch.alchemists.jbmesh.examples;

import ch.alchemists.jbmesh.conversion.BMeshJmeExport;
import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.ColorAttribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;

public class BasicAttributes extends SimpleApplication {
    private BMesh createMesh() {
        BMesh bmesh = new BMesh();

        // Create triangle, counterclockwise vertex order
        Vertex v1 = bmesh.createVertex(0, 0, 0);
        Vertex v2 = bmesh.createVertex(1, 0, 0);
        Vertex v3 = bmesh.createVertex(0.5f, 1, 0);
        bmesh.createFace(v1, v2, v3);

        // Set vertex colors
        ColorAttribute<Vertex> colors = new ColorAttribute<>(BMeshAttribute.Color);
        bmesh.vertices().addAttribute(colors);

        colors.set(v1, ColorRGBA.Red);
        colors.set(v2, ColorRGBA.Green);
        colors.set(v3, ColorRGBA.Blue);

        return bmesh;
    }


    @Override
    public void simpleInitApp() {
        BMesh bmesh = createMesh();

        // Convert BMesh to jme Mesh.
        // This exporter will detect the ColorAttribute and use it for a VertexBuffer.
        Mesh mesh = BMeshJmeExport.exportTriangles(bmesh);

        // Create Material
        Material mat = new Material(assetManager, Materials.UNSHADED);
        mat.setBoolean("VertexColor", true);

        // Create & attach Geometry
        Geometry geom = new Geometry("Geom", mesh);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);
    }


    public static void main(String[] args) {
        BasicAttributes app = new BasicAttributes();
        app.start();
    }
}
