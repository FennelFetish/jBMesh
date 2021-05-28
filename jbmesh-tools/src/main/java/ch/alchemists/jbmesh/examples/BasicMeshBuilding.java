package ch.alchemists.jbmesh.examples;

import ch.alchemists.jbmesh.conversion.BMeshJmeExport;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;

public class BasicMeshBuilding extends SimpleApplication {
    private BMesh createMesh() {
        BMesh bmesh = new BMesh();

        // Create quad, counterclockwise vertex order
        Vertex v1 = bmesh.createVertex(0, 0, 0);
        Vertex v2 = bmesh.createVertex(1, 0, 0);
        Vertex v3 = bmesh.createVertex(1, 1, 0);
        Vertex v4 = bmesh.createVertex(0, 1, 0);

        Face face = bmesh.createFace(v1, v2, v3, v4);
        return bmesh;
    }


    @Override
    public void simpleInitApp() {
        BMesh bmesh = createMesh();

        // Convert BMesh to jme Mesh. The quad is exported as 2 triangles.
        Mesh mesh = BMeshJmeExport.exportTriangles(bmesh);

        // Create Material
        Material mat = new Material(assetManager, Materials.UNSHADED);
        //mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Red);

        // Create & attach Geometry
        Geometry geom = new Geometry("Geom", mesh);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);
    }


    public static void main(String[] args) {
        BasicMeshBuilding app = new BasicMeshBuilding();
        app.start();
    }
}
