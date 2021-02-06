package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.*;
import meshlib.data.BMeshProperty;
import meshlib.data.property.Vec3Property;
import meshlib.operator.FaceOps;
import meshlib.structure.BMesh;
import meshlib.structure.Face;
import meshlib.structure.Vertex;
import meshlib.util.PlanarCoordinateSystem;

public class StraightSkeleton {
    private static final float EPSILON = 0.001f;
    private static final float EPSILON_SQUARED = EPSILON * EPSILON;

    private final BMesh bmesh;
    private final FaceOps faceOps;
    private final Vec3Property<Vertex> propPosition;

    private float initialDistance = 0.0f; // Positive: Grow polygon, Negative: Shrink
    private float distanceSign = 1.0f;


    /*class Result {
        private PlanarCoordinateSystem coordSys;
        private final ArrayList<SkeletonNode> initialNodes = new ArrayList<>();
        private final ArrayList<MovingNode> movingNodes = new ArrayList<>();
    }*/

    private PlanarCoordinateSystem coordSys;
    private final ArrayList<SkeletonNode> initialNodes = new ArrayList<>();
    private final ArrayList<MovingNode> movingNodes = new ArrayList<>();

    // TODO: This mapping adds the functionality for scaling polygons.
    //       It's not directly related to straight skeleton.
    //       --> Move to separate class?
    //       And this map is not needed. The mapping already exists in directed skeleton graph.
    private final Map<Vertex, SkeletonNode> vertexMap = new HashMap<>();

    private final PriorityQueue<SkeletonEvent> eventQueue = new PriorityQueue<>();



    public StraightSkeleton(BMesh bmesh) {
        this.bmesh = bmesh;
        faceOps = new FaceOps(bmesh);
        propPosition = Vec3Property.get(BMeshProperty.Vertex.POSITION, bmesh.vertices());
    }


    /**
     * @param distance Absolute distance in units by which the edges should be moved.<br>
     *                 Positive: Grow face. Negative: Shrink face.
     */
    public void setDistance(float distance) {
        distanceSign = Math.signum(distance);
        initialDistance = Math.abs(distance);
    }


    public void apply(Face face) {
        System.out.println("===== Apply StraightSkeleton =====");
        List<Vertex> vertices = face.getVertices();
        assert vertices.size() >= 3;

        // TODO: Remove duplicate vertices at same positions (or leave up to user?)

        Vector3f n = faceOps.normal(face);
        coordSys = new PlanarCoordinateSystem(propPosition.get(vertices.get(0)), propPosition.get(vertices.get(1)), n);

        project(vertices);
        calcAllBisectors();

        eventQueue.clear();
        createAllEdgeEvents();

        float distance = initialDistance;
        while(Math.abs(distance) > EPSILON) {
            System.out.println("distance = " + distance);
            distance = loop(distance);
        }
    }


    private float loop(float distance) {
        SkeletonEvent event = eventQueue.poll();
        if(event == null) {
            scale(distance * distanceSign);
            System.out.println("no event, loop ended");
            return 0;
        }

        if(distance < event.time) {
            System.out.println("event time: " + event.time + ", distance: " + distance);
            System.out.println("loop ended");

            scale(distance * distanceSign);
            return 0;
        }

        // Reduce time of events
        // TODO: Don't change time of events. Instead, keep track of used time so far?
        for(SkeletonEvent remainingEvent : eventQueue) {
            remainingEvent.time -= event.time;
            System.out.println("subtracting from " + remainingEvent + " time: " + event.time + ", now: " + remainingEvent.time);
        }

        scale(event.time * distanceSign);
        event.handle(movingNodes, eventQueue);
        return distance - event.time;
    }


    private void project(List<Vertex> vertices) {
        vertexMap.clear();
        initialNodes.clear();
        initialNodes.ensureCapacity(vertices.size());

        movingNodes.clear();
        movingNodes.ensureCapacity(vertices.size());

        Vector3f vertexPos = new Vector3f();

        int nextNodeId = 1;
        for(Vertex vertex : vertices) {
            propPosition.get(vertex, vertexPos);

            // Create initial node
            SkeletonNode initialNode = new SkeletonNode();
            coordSys.project(vertexPos, initialNode.p);
            initialNodes.add(initialNode);

            // Create moving node
            MovingNode movingNode = new MovingNode(nextNodeId++);
            movingNode.skelNode = new SkeletonNode();
            movingNode.skelNode.p.set(initialNode.p);
            initialNode.addEdge(movingNode.skelNode);

            movingNodes.add(movingNode);
            vertexMap.put(vertex, movingNode.skelNode);
        }
    }


    private void calcAllBisectors() {
        final int numVertices = movingNodes.size();
        for(MovingNode node : movingNodes) {
            node.edgeLengthChange = 0;
        }

        MovingNode prev = movingNodes.get(numVertices-1);
        MovingNode current = movingNodes.get(0);

        for(int i=0; i<numVertices; ++i) {
            int nextIndex = (i+1) % numVertices;
            MovingNode next = movingNodes.get(nextIndex);

            // Link nodes
            current.next = next;
            current.prev = prev;

            current.calcBisector();

            // Next iteration
            prev = current;
            current = next;
        }
    }


    private void scale(float dist) {
        if(dist == 0) {
            //System.out.println("zero dist, not scaling");
            //return;
            System.out.println("zero dist, scaling anyway");
        }

        System.out.println("scaling " + movingNodes.size() + " nodes by " + dist);
        Vector2f dir = new Vector2f();

        for(MovingNode node : movingNodes) {
            dir.set(node.bisector).multLocal(dist);
            node.skelNode.p.addLocal(dir);

            if(isInvalid(node.skelNode.p)) {
                System.out.println("invalid after scale: bisector=" + node.bisector + ", dir=" + dir);
            }
        }
    }


    private void createAllEdgeEvents() {
        /*if(movingNodes.size() < 3)
            return;*/

        for(MovingNode current : movingNodes) {
            if(sameSign(distanceSign, current.prev.edgeLengthChange))
                eventQueue.add(new EdgeEvent(current.prev, current));
        }
    }

    private static boolean sameSign(float a, float b) {
        return (a >= 0) ^ (b < 0);
    }

    private boolean isInvalid(Vector2f v) {
        return Float.isNaN(v.x) || Float.isInfinite(v.x);
    }


    public SkeletonVisualization getVisualization() {
        return new SkeletonVisualization(coordSys, initialNodes, movingNodes, distanceSign);
    }
}
