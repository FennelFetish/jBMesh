package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.scene.Mesh;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class Export<T> {
    public interface DuplicationStrategy<T> {
        boolean equals(T a, T b);
        void applyProperties(T src, Vertex dest);
        void setBuffers(Mesh outputMesh);
    }


    private static final Logger LOG = Logger.getLogger(Export.class.getName());

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


    public Mesh update() {
        duplicateVertices();
        bmesh.compactData();

        updateOutputMesh();

        duplicationStrategy.setBuffers(outputMesh);
        outputMesh.updateBound();

        LOG.fine("Exported " + bmesh.vertices().size() + " vertices");
        return outputMesh;
    }


    protected abstract void updateOutputMesh();

    protected abstract void getVertexNeighborhood(Vertex vertex, List<T> dest);
    protected abstract void setVertexReference(Vertex contactPoint, T element, Vertex ref);
    protected abstract Vertex getVertexReference(Vertex contactPoint, T element);


    /**
     * Creates virtual vertices.
     */
    private void duplicateVertices() {
        // TODO: Pool virtual vertices and reuse objects? They are destroyed and recreated immediately.
        //       Do this by decorating BMeshData with free list functionality?
        for(Vertex v : virtualVertices)
            bmesh.vertices().destroy(v);
        virtualVertices.clear();

        // TODO: If there are no element properties (for Loop/Edge), there is nothing to duplicate. Leave method early.

        List<Vertex> vertices = bmesh.vertices().getAll();
        List<T> neighbors = new ArrayList<>(6);

        for(Vertex vertex : vertices) {
            // Get elements that use vertex
            neighbors.clear();
            getVertexNeighborhood(vertex, neighbors);
            if(neighbors.isEmpty())
                continue;

            T element = neighbors.get(0);
            setVertexReference(vertex, element, vertex);
            duplicationStrategy.applyProperties(element, vertex);

            // Create virtual Vertex (slot in data array) for elements with different properties
            for(int i=1; i<neighbors.size(); ++i) {
                element = neighbors.get(i);

                Vertex ref = tryVirtualize(vertex, neighbors, element, i);
                setVertexReference(vertex, element, ref);
            }
        }
    }


    private Vertex tryVirtualize(Vertex vertex, List<T> neighbors, T element, int i) {
        // Compare element properties with previous elements
        for(int k=0; k<i; ++k) {
            T prev = neighbors.get(k);
            if(duplicationStrategy.equals(element, prev)) {
                Vertex ref = getVertexReference(vertex, prev);
                assert ref != null;
                return ref;
            }
        }

        // Different properties found, duplicate vertex
        Vertex ref = bmesh.vertices().createVirtual();
        virtualVertices.add(ref);
        bmesh.vertices().copyProperties(vertex, ref);
        duplicationStrategy.applyProperties(element, ref);
        return ref;
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
