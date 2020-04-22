package meshlib.structure;

import com.jme3.math.Vector3f;

// Not necessarily a member of a Face or an Edge
public class Vertex {
    private int index;

    private final Vector3f location = new Vector3f();

    // Can be NULL
    public Edge edge;


    private Vertex() {}


    public void setLocation(float x, float y, float z) {
        location.set(x, y, z);
    }

    public void setLocation(Vector3f loc) {
        setLocation(loc.x, loc.y, loc.z);
    }

    
    public Vector3f getLocation() {
        return location.clone();
    }

    public void getLocation(Vector3f store) {
        store.set(location);
    }


    /**
     * Inserts Edge at end of disk cycle at this Vertex;
     * @param edge
     */
    void addEdge(Edge edge) {
        if(this.edge == null) {
            this.edge = edge;
            return;
        }

        // Find last edge of disk cycle at this vertex
        Edge lastEdge;
        Edge nextEdge = this.edge;
        do {
            if(nextEdge == edge)
                throw new IllegalArgumentException("Edge already exists in disk cycle for vertex"); // Return false instead?

            lastEdge = nextEdge;
            nextEdge = lastEdge.getNextEdge(this);
        } while(nextEdge != this.edge);

        edge.setNextEdge(this, this.edge);
        lastEdge.setNextEdge(this, edge);

        // TODO: Check/modify edge.v0NextEdge/v1NextEdge?
        // Why would parameter 'edge' already have a disk cycle at this vertex?
        // 'edge' must be a newly constructed object
    }


    public Edge getEdgeTo(Vertex other) {
        if(edge == null)
            return null;

        Edge currentEdge = this.edge;
        do {
            if(currentEdge.connects(this, other))
                return currentEdge;
            currentEdge = currentEdge.getNextEdge(this);
        } while(edge != this.edge);

        return null;
    }


    // Store data in arrays in a "Data" object, accessed via index.
    // Options:
    // a) Reference to Data in each Vertex      --> Memory consumption
    // b) Vertex.getLocation(BMesh, Vector3f)
    // c) BMesh.getLocation(Vertex, Vector3f)   --> all functions like this would have to go to BMesh, making it larger


    static final BMeshData.ElementAccessor<Vertex> ACCESSOR = new BMeshData.ElementAccessor<Vertex>() {
        @Override
        public Vertex create() {
            return new Vertex();
        }

        @Override
        public void release(Vertex element) {
            element.index = -1;
            element.location.zero();
            element.edge = null;
        }

        @Override
        public int getIndex(Vertex element) {
            return element.index;
        }

        @Override
        public void setIndex(Vertex element, int index) {
            element.index = index;
        }
    };
}
