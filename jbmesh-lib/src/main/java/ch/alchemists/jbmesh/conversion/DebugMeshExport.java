package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.operator.FaceOps;
import ch.alchemists.jbmesh.operator.sweeptriang.SweepTriangulation;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class DebugMeshExport {
    private static final float INNER_SCALE       = 0.85f;
    private static final Vector3f COLOR_INNER    = new Vector3f(194, 185, 149).divideLocal(255);
    private static final Vector3f COLOR_SRC      = new Vector3f(30, 30, 30).divideLocal(255);
    private static final Vector3f COLOR_GREEN    = new Vector3f(0.0f, 0.5f, 0.0f);
    private static final Vector3f COLOR_GREEN_FIRSTLOOP = new Vector3f(0.0f, 1.0f, 0.7f);
    private static final Vector3f COLOR_RED      = new Vector3f(0.8f, 0.0f, 0.0f);
    private static final Vector3f COLOR_RED_FIRSTLOOP = new Vector3f(1.0f, 0.0f, 0.7f);
    private static final Vector3f COLOR_SELECTED = new Vector3f(0.0f, 1.0f, 1.0f);

    private static final Vector3f[] COLOR_INNER_ARR = new Vector3f[6];
    static {
        for(int i=0; i<COLOR_INNER_ARR.length; ++i) {
            float c = 0.5f + (0.5f*i/COLOR_INNER_ARR.length);
            COLOR_INNER_ARR[i] = COLOR_INNER.mult(c);
        }
    }

    private final ArrayList<Float> vertices  = new ArrayList<>();
    private final ArrayList<Float> normals   = new ArrayList<>();
    private final ArrayList<Float> colors    = new ArrayList<>();
    private final ArrayList<Integer> indices = new ArrayList<>();


    public DebugMeshExport() {}


    public void clear() {
        vertices.clear();
        normals.clear();
        colors.clear();
        indices.clear();
    }


    private int addVertex(Vector3f v) {
        int index = vertices.size() / 3;
        vertices.add(v.x);
        vertices.add(v.y);
        vertices.add(v.z);
        return index;
    }


    private void addNormal(Vector3f n) {
        normals.add(n.x);
        normals.add(n.y);
        normals.add(n.z);
    }

    private void addNormal(Vector3f n, int count) {
        for(int i=0; i<count; ++i)
            addNormal(n);
    }


    private void addColor(Vector3f c) {
        colors.add(c.x);
        colors.add(c.y);
        colors.add(c.z);
        colors.add(0.95f); // alpha
    }

    private void addColor(Vector3f c, int count) {
        for(int i=0; i<count; ++i)
            addColor(c);
    }


    public Mesh createMesh() {
        float[] vbuf = new float[vertices.size()];
        int i = 0;
        for(Float f : vertices)
            vbuf[i++] = f;

        float[] nbuf = new float[normals.size()];
        i = 0;
        for(Float f : normals)
            nbuf[i++] = f;

        float[] cbuf = new float[colors.size()];
        i = 0;
        for(Float f : colors)
            cbuf[i++] = f;

        int[] ibuf = new int[indices.size()];
        i = 0;
        for(Integer idx : indices)
            ibuf[i++] = idx;

        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, vbuf);
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, nbuf);
        mesh.setBuffer(VertexBuffer.Type.Color, 4, cbuf);
        mesh.setBuffer(VertexBuffer.Type.Index, 1, ibuf);

        mesh.setMode(Mesh.Mode.Triangles);
        mesh.updateBound();

        return mesh;
    }


    public void apply(BMesh bmesh) {
        Vec3Attribute<Vertex> positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
        final ArrayList<Vector3f> faceVertices = new ArrayList<>();
        FaceOps faceOps = new FaceOps(bmesh);

        SweepTriangulation triangulation = new SweepTriangulation();

        for(Face face : bmesh.faces()) {
            faceVertices.clear();
            for(Vertex vertex : face.vertices())
                faceVertices.add(positions.get(vertex));

            final int size = faceVertices.size();
            final Vector3f normal   = faceOps.normal(face);
            final Vector3f centroid = faceOps.centroid(face);

            // Scale vertices, TODO: Use inset operator
            Vector3f[] innerVerts = new Vector3f[size];
            for(int i=0; i<size; ++i)
                innerVerts[i] = faceVertices.get(i).subtract(centroid).multLocal(INNER_SCALE).addLocal(centroid);

            // Create "arrows"
            Loop loop = face.loop;
            for(int i=0; i<size; ++i) {
                int i0 = addVertex(innerVerts[i]);
                int i1 = addVertex(faceVertices.get(i));
                int i2 = addVertex(faceVertices.get((i+1)%size));
                int i3 = addVertex(innerVerts[(i+1)%size]);

                addNormal(normal, 4);
                addColor(COLOR_SRC, 2);

                if(loop.nextEdgeLoop == loop)
                    addColor(i==0 ? COLOR_RED_FIRSTLOOP : COLOR_RED, 2);
                else
                    addColor(i==0 ? COLOR_GREEN_FIRSTLOOP : COLOR_GREEN, 2);

                indices.add(i0);
                indices.add(i1);
                indices.add(i3);

                indices.add(i1);
                indices.add(i2);
                indices.add(i3);

                loop = loop.nextFaceLoop;
            }

            // Add inner vertices
            int[] innerIdx = new int[size];
            for(int i=0; i<size; ++i) {
                innerIdx[i] = addVertex(innerVerts[i]);
                addNormal(normal);
                //addColor(COLOR_INNER_ARR[i]);
                addColor(COLOR_INNER);
            }

            // Fan-like triangulation for inner area
            // TODO: This creates superfluous triangles for collinear edges?
            /*for(int i=2; i<size; ++i) {
                indices.add(innerIdx[0]);
                indices.add(innerIdx[i-1]);
                indices.add(innerIdx[i]);
            }*/

            triangulation.setTriangleCallback((v1, v2, v3) -> {
                indices.add(innerIdx[v1.index]);
                indices.add(innerIdx[v2.index]);
                indices.add(innerIdx[v3.index]);
            });

            triangulation.addFaceWithPositions(Arrays.asList(innerVerts));
            triangulation.triangulate();
        }
    }
}
