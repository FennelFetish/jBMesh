package meshlib;

import meshlib.data.BMeshProperty;
import meshlib.data.property.ColorProperty;
import meshlib.data.property.IntProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Vertex;

public class PropertyAccess {
    private final BMesh bmesh;

    public PropertyAccess(BMesh bmesh) {
        this.bmesh = bmesh;
    }


    public void init() {
        Vec3Property<Face> propFace = new Vec3Property<>("FaceVec", bmesh.faceData());
    }

    public void shouldFail() {
        // Invalid element type
        Vec3Property<Face> propFace2        = bmesh.vertexData().getProperty("FaceVec", Vec3Property.class);
        BMeshProperty<?, Face> propFace3    = bmesh.vertexData().getProperty("FaceVec");
        Vec3Property<Face> propFace4        = bmesh.vertexData().getProperty("FaceVec");
        
        Vec3Property<Face> propFace5        = (Vec3Property) bmesh.vertexData().getProperty("FaceVec");  // !!!!
        Vec3Property<Face> propFace5        = (Vec3Property<Face>) bmesh.vertexData().getProperty("FaceVec");

        Vec3Property<Face> propFace6        = Vec3Property.get("FaceVec", bmesh.vertexData());
    }


    public void shouldFailAtRuntime() {
        //IntProperty<Face> propInt           = (IntProperty<Face>) bmesh.faceData().getProperty("FaceVec");
        //IntProperty<Face> propInt2          = bmesh.faceData().getProperty("FaceVec", IntProperty.class);
        Vec3Property<Vertex> propInt2          = bmesh.faceData().getProperty("FaceVec", Vec3Property.class);
    }


    public void shouldWork() {
        Vec3Property<Face> propFace6 = Vec3Property.get("FaceVec", bmesh.faceData());

        Vec3Property<Vertex> propPosition = (Vec3Property<Vertex>) bmesh.vertexData().getProperty(BMeshProperty.Vertex.POSITION, Vec3Property.TYPE);
        //Vec3Property<Vertex> propPosition = (Vec3Property<Vertex>) bmesh.vertexData().getProperty(BMeshProperty.Vertex.POSITION);
        ColorProperty<Vertex> propVertexColor = (ColorProperty) bmesh.vertexData().getProperty(BMeshProperty.Vertex.COLOR);
    }
}
