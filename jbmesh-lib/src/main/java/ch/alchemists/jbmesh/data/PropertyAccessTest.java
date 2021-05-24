package ch.alchemists.jbmesh.data;

import ch.alchemists.jbmesh.data.property.ColorAttribute;
import ch.alchemists.jbmesh.data.property.IntAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;

public class PropertyAccessTest {
    private final BMesh bmesh;

    
    public PropertyAccessTest(BMesh bmesh) {
        this.bmesh = bmesh;
        Vec3Attribute<Face> propFace = new Vec3Attribute<>("FaceVec");
        bmesh.faces().addAttribute(propFace);
    }


   /* public void shouldNotCompile() {
        // Invalid element type
        //Vec3Property<Face> propFace2        = bmesh.vertexData().getProperty("FaceVec", Vec3Property.class);
        BMeshProperty<Face, ?> propFace3    = bmesh.vertexData().getProperty("FaceVec");
        Vec3Property<Face> propFace4        = bmesh.vertexData().getProperty("FaceVec");
        
        Vec3Property<Face> propFace5        = (Vec3Property) bmesh.vertexData().getProperty("FaceVec");  // !!!! ok issues warning
        Vec3Property<Face> propFace6        = (Vec3Property<Face>) bmesh.vertexData().getProperty("FaceVec");

        Vec3Property<Face> propFace7        = Vec3Property.get("FaceVec", bmesh.vertexData());
        Vec3Property propFace8              = Vec3Property.get("FaceVec", bmesh.vertexData()); // !!!!
    }*/


    public void shouldFailAtRuntime() {
        //IntProperty<Face> propInt           = (IntProperty<Face>) bmesh.faceData().getProperty("FaceVec");
        //IntProperty<Face> propInt2          = bmesh.faceData().getProperty("FaceVec", IntProperty.class);
        //Vec3Property<Vertex> propInt2          = bmesh.faceData().getProperty("FaceVec", Vec3Property.class);

        IntAttribute<Face> propFace6 = IntAttribute.get("FaceVec", bmesh.faces()); // ok fails
    }


    public void shouldWork() {
        Vec3Attribute<Face> propFace3 = Vec3Attribute.get("A", bmesh.faces());
        assert propFace3 == null;

        Vec3Attribute<Face> propFace5 = (Vec3Attribute<Face>) bmesh.faces().getAttribute("FaceVec");
        Vec3Attribute<Face> propFace6 = (Vec3Attribute<Face>) bmesh.faces().getAttribute("FaceVec", float[].class);
        Vec3Attribute<Face> propFace7 = Vec3Attribute.get("FaceVec", bmesh.faces());

        //Vec3Property<Face> propFace4        = bmesh.faceData().getProperty("FaceVec");

        //Vec3Property<Vertex> propPosition = (Vec3Property<Vertex>) bmesh.vertexData().getProperty(BMeshProperty.Vertex.POSITION);
        ColorAttribute<Vertex> propVertexColor = (ColorAttribute<Vertex>) bmesh.vertices().getAttribute(Vertex.Color);
    }
}
