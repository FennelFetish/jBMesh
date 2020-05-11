package meshlib.conversion;

import com.jme3.scene.Mesh;
import java.util.ArrayList;
import java.util.List;
import meshlib.structure.BMesh;
import meshlib.structure.Vertex;

public abstract class Export<T> {
    public interface DuplicationStrategy<T> {
        boolean splitVertex(T a, T b);
        void copyProperties(T src, Vertex dest);
        void setBuffers(Mesh outputMesh);
    }


    protected final BMesh bmesh;
    protected final Mesh outputMesh = new Mesh();
    protected DuplicationStrategy<T> duplicationStrategy;

    protected final List<Vertex> virtualVertices = new ArrayList<>(32);


    public Export(BMesh bmesh, DuplicationStrategy<T> duplicationStrategy) {
        this.bmesh = bmesh;
        this.duplicationStrategy = duplicationStrategy;
    }


    public final Mesh getMesh() {
        return outputMesh;
    }


    public void update() {
        duplicateVertices();
        bmesh.compactData();

        updateOutputMesh();

        duplicationStrategy.setBuffers(outputMesh);
        outputMesh.updateBound();
    }


    protected abstract void updateOutputMesh();

    protected abstract void getVertexNeighborhood(Vertex vertex, List<T> dest);
    protected abstract void setVertexReference(Vertex contactPoint, T element, Vertex ref);
    protected abstract Vertex getVertexReference(Vertex contactPoint, T element);


    /**
     * Creates virtual vertices.
     */
    private void duplicateVertices() {
        for(Vertex v : virtualVertices)
            bmesh.vertices().destroy(v);
        virtualVertices.clear();

        List<Vertex> vertices = bmesh.vertices().getAll();
        List<T> neighbors = new ArrayList<>(6);

        for(Vertex vertex : vertices) {
            // Get elements that use vertex
            neighbors.clear();
            getVertexNeighborhood(vertex, neighbors);

            T element = neighbors.get(0);
            setVertexReference(vertex, element, vertex);
            duplicationStrategy.copyProperties(element, vertex);

            // Create virtual Vertex (slot in data array) for elements with different properties
            for(int i=1; i<neighbors.size(); ++i) {
                element = neighbors.get(i);

                // Compare element properties with previous elements
                Vertex ref = null;
                for(int k=0; k<i; ++k) {
                    T prev = neighbors.get(k);
                    if(duplicationStrategy.splitVertex(element, prev)) {
                        ref = getVertexReference(vertex, prev);
                        break;
                    }
                }

                if(ref == null) {
                    ref = bmesh.vertices().createVirtual();
                    virtualVertices.add(ref);
                    bmesh.vertices().copyProperties(vertex, ref);
                    duplicationStrategy.copyProperties(element, ref);
                }

                setVertexReference(vertex, element, ref);
            }
        }
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
