package meshlib.conversion;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import meshlib.data.BMeshProperty;
import meshlib.data.Element;
import meshlib.data.property.ColorProperty;
import meshlib.data.property.ObjectProperty;
import meshlib.data.property.Vec3Property;
import meshlib.structure.*;

public class Export {
    private static class LoopVertexProperty<E extends Element> extends ObjectProperty<E, Vertex> {
        protected LoopVertexProperty(String name) {
            super(name);
        }

        @Override
        protected meshlib.structure.Vertex[] alloc(int size) {
            return new meshlib.structure.Vertex[size];
        }
    }


    private final BMesh bmesh;
    private final Mesh outputMesh = new Mesh();

    private final List<Vertex> virtualVertices = new ArrayList<>(32);
    private final LoopVertexProperty<Loop> propLoopVertex = new LoopVertexProperty<>("LoopVertex");

    private final Vec3Property<Loop> propLoopNormal;
    private final Vec3Property<Vertex> propVertexNormal;
    private final Vector3f tempNormal = new Vector3f();


    public Export(BMesh bmesh) {
        this.bmesh = bmesh;

        propLoopVertex.setComparable(false);
        bmesh.loops().addProperty(propLoopVertex);

        propLoopNormal = Vec3Property.get(BMeshProperty.Loop.NORMAL, bmesh.loops());

        Vec3Property<Vertex> propVertexNormal = Vec3Property.get(BMeshProperty.Vertex.NORMAL, bmesh.vertices());
        if(propVertexNormal == null) {
            propVertexNormal = new Vec3Property<>(BMeshProperty.Vertex.NORMAL);
            bmesh.vertices().addProperty(propVertexNormal);
        }
        this.propVertexNormal = propVertexNormal;

        outputMesh.setMode(Mesh.Mode.Triangles);
    }


    public Mesh getMesh() {
        return outputMesh;
    }


    public void update() {
        duplicateVertices();
        bmesh.compactData();

        ArrayList<Integer> indices = new ArrayList<>(bmesh.loops().size());
        ArrayList<Loop> loops = new ArrayList<>(4);

        for(Face f : bmesh.faces()) {
            for(Loop loop : f.loops()) {
                loops.add(loop);
            }

            // Fan-like triangulation for inner area
            // TODO: This creates superfluous triangles for collinear edges?
            for(int i=2; i<loops.size(); ++i) {
                Vertex v0 = propLoopVertex.get(loops.get(0));
                Vertex v1 = propLoopVertex.get(loops.get(i-1));
                Vertex v2 = propLoopVertex.get(loops.get(i));

                indices.add(v0.getIndex());
                indices.add(v1.getIndex());
                indices.add(v2.getIndex());
            }

            loops.clear();
        }

        IntBuffer ibuf = BufferUtils.createIntBuffer(indices.size());
        for(int i=0; i<indices.size(); ++i)
            ibuf.put(indices.get(i));
        ibuf.flip();
        //System.out.println("indices: " + indices.size());

        Vec3Property<Vertex> propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());

        /*printArr("Position", propPosition.array(), 3);
        printArr("Normal", propVertexNormal.array(), 3);
        printArr("Color", propVertexColor.array(), 4);
        printArr("Index", indices, 3);*/

        outputMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(propPosition.array()));
        outputMesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(propVertexNormal.array()));
        outputMesh.setBuffer(VertexBuffer.Type.Index, 3, ibuf);

        ColorProperty<Vertex> propVertexColor = ColorProperty.get(BMeshProperty.Vertex.COLOR, bmesh.vertices());
        if(propVertexColor != null)
            outputMesh.setBuffer(VertexBuffer.Type.Color, 4, BufferUtils.createFloatBuffer(propVertexColor.array()));

        outputMesh.updateBound();
    }


    private void duplicateVertices() {
        // Create virtual vertices
        for(Vertex v : virtualVertices)
            bmesh.vertices().destroy(v);
        virtualVertices.clear();

        List<Vertex> vertices = bmesh.vertices().getAll();
        List<Loop> loops = new ArrayList<>(6);

        for(Vertex vertex : vertices) {
            // Get loops that use vertex
            loops.clear();
            for(Edge edge : vertex.edges()) {
                for(Loop loop : edge.loops()) {
                    if(loop.vertex == vertex)
                        loops.add(loop);
                }
            }

            propLoopVertex.set(loops.get(0), vertex);
            copyToVertex(loops.get(0), vertex);

            for(int i=1; i<loops.size(); ++i) {
                Loop loop = loops.get(i);

                Vertex ref = null;
                for(int k=0; k<i; ++k) {
                    Loop prev = loops.get(k);
                    if(bmesh.loops().equals(loop, prev)) {
                        ref = propLoopVertex.get(prev);
                        break;
                    }
                }

                if(ref == null) {
                    ref = bmesh.vertices().createVirtual();
                    virtualVertices.add(ref);
                    bmesh.vertices().copyProperties(vertex, ref);
                    copyToVertex(loop, ref);
                }

                propLoopVertex.set(loop, ref);
            }
        }

        System.out.println("vertices: " + bmesh.vertices().size());
        System.out.println("virtual vertices: " + virtualVertices.size());
        System.out.println("vertex uses (indices): " + (bmesh.vertices().size() + virtualVertices.size()));
    }


    private void copyToVertex(Loop src, Vertex dest) {
        propLoopNormal.get(src, tempNormal);
        propVertexNormal.set(dest, tempNormal);
    }


    private void printArr(String name, float[] arr, int comp) {
        System.out.println(name + " ------");

        int o=0;
        for(int i=0; i<arr.length; ) {
            System.out.print(o + ": ");
            for(int c=0; c<comp; ++c) {
                System.out.print(arr[i++]);
                System.out.print(", ");
            }
            System.out.println("");
            o++;
        }
    }

    private void printArr(String name, ArrayList<Integer> arr, int comp) {
        System.out.println(name + " ------");

        int o=0;
        for(int i=0; i<arr.size(); ) {
            System.out.print(o + ": ");
            for(int c=0; c<comp; ++c) {
                System.out.print(arr.get(i++));
                System.out.print(", ");
            }
            System.out.println("");
            o++;
        }
    }
}
