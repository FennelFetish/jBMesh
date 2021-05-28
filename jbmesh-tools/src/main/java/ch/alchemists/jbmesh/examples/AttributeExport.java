package ch.alchemists.jbmesh.examples;

import ch.alchemists.jbmesh.conversion.BMeshJmeExport;
import ch.alchemists.jbmesh.conversion.LineExport;
import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.ColorAttribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Edge;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

public class AttributeExport extends SimpleApplication {
    private BMesh createMesh() {
        BMesh bmesh = new BMesh();

        // Create quad, counterclockwise vertex order
        Vertex v1 = bmesh.createVertex(0, 0, 0);
        Vertex v2 = bmesh.createVertex(1, 0, 0);
        Vertex v3 = bmesh.createVertex(1, 1, 0);
        Vertex v4 = bmesh.createVertex(0, 1, 0);

        Face face = bmesh.createFace(v1, v2, v3, v4);

        // Generate random color for each Edge
        ColorAttribute<Edge> edgeColors = ColorAttribute.getOrCreate(BMeshAttribute.Color, bmesh.edges());
        for(Edge edge : face.edges())
            edgeColors.set(edge, ColorRGBA.randomColor());

        return bmesh;
    }


    private Mesh exportExplicit(BMesh bmesh) {
        // Create LineExport which converts BMesh Edges to a jme Mesh.
        LineExport export = new LineExport(bmesh);

        // Create target attribute for Vertex. Its data is sent to the GPU as VertexBuffer.
        ColorAttribute<Vertex> vertexColors = ColorAttribute.getOrCreate(BMeshAttribute.Color, bmesh.vertices());

        // Register Edge colors as source for Vertex colors.
        ColorAttribute<Edge> edgeColors = ColorAttribute.get(BMeshAttribute.Color, bmesh.edges());
        export.mapAttribute(VertexBuffer.Type.Color, edgeColors, vertexColors);

        // The exporter duplicates vertices where needed.
        return export.update();
    }


    private Mesh exportImplicit(BMesh bmesh) {
        // Create LineExport which converts BMesh Edges to a jme Mesh.
        LineExport export = new LineExport(bmesh);

        // Alternative short form which automatically creates the target ColorAttribute<Vertex>.
        ColorAttribute<Edge> edgeColors = ColorAttribute.get(BMeshAttribute.Color, bmesh.edges());
        export.mapAttribute(VertexBuffer.Type.Color, edgeColors);

        // The exporter duplicates vertices where needed.
        return export.update();
    }


    @Override
    public void simpleInitApp() {
        BMesh bmesh = createMesh();

        //Mesh mesh = exportExplicit(bmesh);
        //Mesh mesh = exportImplicit(bmesh);

        // Alternative one-liner which automatically maps all recognized default attributes.
        Mesh mesh = BMeshJmeExport.exportLines(bmesh);

        // Create Material
        Material mat = new Material(assetManager, Materials.UNSHADED);
        mat.setBoolean("VertexColor", true);

        // Create & attach Geometry
        Geometry geom = new Geometry("Geom", mesh);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);
    }


    public static void main(String[] args) {
        AttributeExport app = new AttributeExport();
        app.start();
    }
}
