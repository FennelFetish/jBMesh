package ch.alchemists.jbmesh.util;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.operator.FaceOps;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

public class DebugNormals {
    public static Geometry faceNormals(AssetManager assetManager, BMesh bmesh, float length) {
        Mesh mesh = createFaceNormals(bmesh, length);
        return createGeometry(assetManager, mesh, "Face Normals");
    }

    public static Geometry loopNormals(AssetManager assetManager, BMesh bmesh, float length) {
        Mesh mesh = createLoopNormals(bmesh, length);
        return createGeometry(assetManager, mesh, "Loop Normals");
    }


    private static Geometry createGeometry(AssetManager assetManager, Mesh mesh, String name) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setBoolean("VertexColor", true);

        Geometry geom = new Geometry(name, mesh);
        geom.setShadowMode(RenderQueue.ShadowMode.Off);
        geom.setMaterial(mat);
        return geom;
    }


    private static Mesh createFaceNormals(BMesh bmesh, float length) {
        float[] vbuf = new float[bmesh.faces().size() * 6];
        int idxVertex = 0;

        float[] cbuf = new float[bmesh.faces().size() * 6];
        int idxColor = 0;

        FaceOps faceOps = new FaceOps(bmesh);
        Vector3f centroid = new Vector3f();
        Vector3f normal = new Vector3f();
        Vector3f color = new Vector3f();

        for(Face face : bmesh.faces()) {
            faceOps.centroid(face, centroid);
            faceOps.normal(face, normal);
            colorFromNormal(normal, color);
            normal.multLocal(length);
            normal.addLocal(centroid);

            idxVertex = addToBuffer(vbuf, idxVertex, centroid);
            idxVertex = addToBuffer(vbuf, idxVertex, normal);

            idxColor = addToBuffer(cbuf, idxColor, color);
            idxColor = addToBuffer(cbuf, idxColor, color);
        }

        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Lines);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, vbuf);
        mesh.setBuffer(VertexBuffer.Type.Color, 3, cbuf);
        mesh.updateBound();
        return mesh;
    }


    private static Mesh createLoopNormals(BMesh bmesh, float length) {
        Vec3Attribute<Loop> normals = Vec3Attribute.get(BMeshAttribute.Normal, bmesh.loops());
        if(normals == null)
            throw new IllegalArgumentException("The provided BMesh object doesn't have Loop normals.");

        Vec3Attribute<Vertex> positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());

        // Filling an array and then putting it into a FloatBuffer all at once scales better with bigger meshes performance-wise
        float[] vbuf = new float[bmesh.loops().size() * 6];
        int idxVertex = 0;

        float[] cbuf = new float[bmesh.loops().size() * 6];
        int idxColor = 0;

        Vector3f p = new Vector3f();
        Vector3f normal = new Vector3f();
        Vector3f color = new Vector3f();

        for(Loop loop : bmesh.loops()) {
            positions.get(loop.vertex, p);
            normals.get(loop, normal);
            colorFromNormal(normal, color);
            normal.multLocal(length);
            normal.addLocal(p);

            idxVertex = addToBuffer(vbuf, idxVertex, p);
            idxVertex = addToBuffer(vbuf, idxVertex, normal);

            idxColor = addToBuffer(cbuf, idxColor, color);
            idxColor = addToBuffer(cbuf, idxColor, color);
        }

        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Lines);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, vbuf);
        mesh.setBuffer(VertexBuffer.Type.Color, 3, cbuf);
        mesh.updateBound();
        return mesh;
    }


    private static void colorFromNormal(Vector3f normal, Vector3f destColor) {
        destColor.set(normal);
        if(destColor.x < 0)
            destColor.x *= -0.5f;
        if(destColor.y < 0)
            destColor.y *= -0.5f;
        if(destColor.z < 0)
            destColor.z *= -0.5f;
    }


    private static int addToBuffer(float[] buf, int i, Vector3f vec) {
        buf[i++] = vec.x;
        buf[i++] = vec.y;
        buf[i++] = vec.z;
        return i;
    }
}
