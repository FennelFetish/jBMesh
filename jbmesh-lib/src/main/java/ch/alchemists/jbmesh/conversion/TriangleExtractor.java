package ch.alchemists.jbmesh.conversion;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class TriangleExtractor {
    public static interface TriangleIndexVisitor {
        void visitTriangleIndices(int i0, int i1, int i2);
    }


    public abstract class TriangleLocationVisitor implements TriangleIndexVisitor {
        private final Vector3f p0 = new Vector3f();
        private final Vector3f p1 = new Vector3f();
        private final Vector3f p2 = new Vector3f();

        @Override
        public final void visitTriangleIndices(int i0, int i1, int i2) {
            getVertex(i0, p0);
            getVertex(i1, p1);
            getVertex(i2, p2);
            visitTriangle(p0, p1, p2);
        }

        public abstract void visitTriangle(Vector3f p0, Vector3f p1, Vector3f p2);
    }


    private float[] positionBuffer;
    private int[] indexBuffer;

    private Mesh.Mode meshMode;


    public TriangleExtractor(Mesh mesh) {
        setMesh(mesh);
    }


    public final void setMesh(Mesh mesh) {
        meshMode = mesh.getMode();

        FloatBuffer fbPos = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        positionBuffer = VertexBufferUtils.getFloatArray(fbPos);

        VertexBuffer vbIdx = mesh.getBuffer(VertexBuffer.Type.Index);
        switch(vbIdx.getFormat()) {
            case Int:
            case UnsignedInt:
                indexBuffer = VertexBufferUtils.getIntArray((IntBuffer) vbIdx.getData());
                break;

            case Short:
            case UnsignedShort: {
                indexBuffer = VertexBufferUtils.getIntArray((ShortBuffer) vbIdx.getData());
                break;
            }

            default:
                throw new UnsupportedOperationException("Index buffer format '" + vbIdx.getFormat() + "' not supported.");
        }
    }


    public void process(TriangleIndexVisitor visitor) {
        switch(meshMode) {
            case Triangles:
                processTriangles(visitor);
                break;

            case TriangleStrip:
                processTriangleStrip(visitor);
                break;

            case TriangleFan:
                processTriangleFan(visitor);
                break;

            default:
                throw new IllegalArgumentException("Mesh does not consist of triangles. Mode: " + meshMode.name());
        }
    }


    private void processTriangles(TriangleIndexVisitor visitor) {
        for(int i=2; i<=indexBuffer.length; i+=3) {
            visitor.visitTriangleIndices(indexBuffer[i-2], indexBuffer[i-1], indexBuffer[i]);
        }
    }


    private void processTriangleStrip(TriangleIndexVisitor visitor) {
        for(int i=2; i<indexBuffer.length; ++i) {
            if((i&1) == 0) {
                visitor.visitTriangleIndices(indexBuffer[i-2], indexBuffer[i-1], indexBuffer[i]);
            } else {
                visitor.visitTriangleIndices(indexBuffer[i-1], indexBuffer[i-2], indexBuffer[i]);
            }
        }
    }


    private void processTriangleFan(TriangleIndexVisitor visitor) {
        for(int i=2; i<indexBuffer.length; ++i) {
            visitor.visitTriangleIndices(indexBuffer[0], indexBuffer[i-1], indexBuffer[i]);
        }
    }


    public int getIndex(int index) {
        return indexBuffer[index];
    }

    public int getNumIndices() {
        return indexBuffer.length;
    }

    public int getNumVertices() {
        return positionBuffer.length / 3;
    }

    public void getVertex(int index, Vector3f store) {
        int offset = index * 3;
        store.x = positionBuffer[offset];
        store.y = positionBuffer[offset+1];
        store.z = positionBuffer[offset+2];
    }

    public float[] getPositionArray() {
        return positionBuffer;
    }

    public int[] getIndexArray() {
        return indexBuffer;
    }
}
