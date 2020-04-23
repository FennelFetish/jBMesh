package meshlib.util;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.util.List;
import meshlib.structure.BMesh;
import meshlib.structure.BMeshData;
import meshlib.structure.BMeshProperty;
import meshlib.structure.Face;
import meshlib.structure.Vertex;

public class BMeshVisualization {
    public static Mesh create(BMesh bmesh) {
        bmesh.vertexData().compact();
        
        BMeshData<Vertex>.Property propPosition = bmesh.vertexData().getProperty(BMeshProperty.Vertex.POSITION);
        float[] pos = propPosition.getFloatArray();

        BMeshData<Vertex>.Property propVertexColor = bmesh.vertexData().getProperty(BMeshProperty.Vertex.COLOR);
        float[] color = propVertexColor.getFloatArray();

        List<Face> faces = bmesh.faces();
        int[] indices = new int[faces.size() * 3];
        int index = 0;
        for(int i=0; i<faces.size(); ++i) {
            System.out.println("add face");
            Face f = faces.get(i);

            int numVertices = 0;
            for(Vertex vertex : f.vertices()) {
                numVertices++;
                indices[index++] = vertex.getIndex();
            }

            if(numVertices != 3) {
                throw new RuntimeException("Face is not a triangle");
            }
        }

        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Triangles);
        
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(pos));
        mesh.setBuffer(VertexBuffer.Type.Color, 3, BufferUtils.createFloatBuffer(color));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
        mesh.updateBound();

        return mesh;
    }
}
