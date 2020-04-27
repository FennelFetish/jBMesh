package meshlib.data;

import meshlib.data.property.ColorProperty;
import meshlib.data.property.IntProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Vertex;

public class PropertyAccessTest {
    private final BMesh bmesh;

    
    public PropertyAccessTest(BMesh bmesh) {
        this.bmesh = bmesh;
        Vec3Property<Face> propFace = new Vec3Property<>("FaceVec");
        bmesh.faces().addProperty(propFace);
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

        IntProperty<Face> propFace6 = IntProperty.get("FaceVec", bmesh.faces()); // ok fails
    }


    public void shouldWork() {
        Vec3Property<Face> propFace3 = Vec3Property.get("A", bmesh.faces());
        assert propFace3 == null;

        Vec3Property<Face> propFace5 = (Vec3Property<Face>) bmesh.faces().getProperty("FaceVec");
        Vec3Property<Face> propFace6 = Vec3Property.get("FaceVec", bmesh.faces());

        //Vec3Property<Face> propFace4        = bmesh.faceData().getProperty("FaceVec");

        //Vec3Property<Vertex> propPosition = (Vec3Property<Vertex>) bmesh.vertexData().getProperty(BMeshProperty.Vertex.POSITION);
        ColorProperty<Vertex> propVertexColor = (ColorProperty<Vertex>) bmesh.vertices().getProperty(BMeshProperty.Vertex.COLOR);
    }
}
