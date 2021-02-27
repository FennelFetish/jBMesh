package ch.alchemists.jbmesh.operator.skeleton;

import ch.alchemists.jbmesh.data.BMeshProperty;
import ch.alchemists.jbmesh.data.property.Vec3Property;
import ch.alchemists.jbmesh.operator.FaceOps;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.PlanarCoordinateSystem;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class StraightSkeleton {
    private final BMesh bmesh;
    private final FaceOps faceOps;
    private final Vec3Property<Vertex> propPosition;

    private float offsetDistance = Float.POSITIVE_INFINITY; // Absolute value
    private float distanceSign = -1.0f;

    private PlanarCoordinateSystem coordSys;
    private final ArrayList<SkeletonNode> initialNodes = new ArrayList<>();
    private final SkeletonContext ctx = new SkeletonContext();


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
        if(distance == Float.POSITIVE_INFINITY) {
            throw new IllegalArgumentException("Cannot scale outwards to infinity.");
        }

        distanceSign = Math.signum(distance);
        offsetDistance = Math.abs(distance);
    }


    public void apply(Face face) {
        List<Vertex> vertices = face.getVertices();
        assert vertices.size() >= 3;

        ctx.reset(offsetDistance, distanceSign);

        Vector3f n = faceOps.normal(face);
        coordSys = new PlanarCoordinateSystem(propPosition.get(vertices.get(0)), propPosition.get(vertices.get(1)), n);

        float diagonalSize = createNodes(vertices);

        // When shrinking to infinity, use polygon's bounding rectangle to determine max distance (less events queued = speed up)
        if(distanceSign < 0 && offsetDistance == Float.POSITIVE_INFINITY) {
            ctx.distance = diagonalSize * 0.51f;
        }

        if(ctx.distance != 0) {
            initBisectors();
            initEvents();
            loop();
        }
    }


    private void loop() {
        ctx.time = 0;

        while(true) {
            //ctx.printNodes();
            //ctx.printEvents();

            SkeletonEvent event = ctx.pollQueue();
            if(event == null) {
                scale(ctx.distance - ctx.time);
                break;
            }

            scale(event.time - ctx.time);
            ctx.time = event.time;
            event.handle(ctx);
            ctx.recheckAbortedReflexNodes();
        }
    }


    /**
     * Creates MovingNodes out of the vertices.
     * Also calculates bounding rectangle.
     * @return Diagonal length of bounding rectangle.
     */
    private float createNodes(List<Vertex> vertices) {
        initialNodes.clear();
        initialNodes.ensureCapacity(vertices.size());

        Vector3f min = Vector3f.POSITIVE_INFINITY.clone();
        Vector3f max = Vector3f.NEGATIVE_INFINITY.clone();

        Vector3f vertexPos = new Vector3f();
        final MovingNode first = createNode(vertices.get(0), vertexPos, min, max);
        MovingNode prev = first;

        for(int i=1; i<vertices.size(); ++i) {
            MovingNode movingNode = createNode(vertices.get(i), vertexPos, min, max);

            // Link nodes
            movingNode.prev = prev;
            prev.next = movingNode;

            prev = movingNode;
        }

        first.prev = prev;
        prev.next = first;

        return max.subtractLocal(min).length();
    }

    private MovingNode createNode(Vertex vertex, Vector3f vertexPos, Vector3f min, Vector3f max) {
        propPosition.get(vertex, vertexPos);
        min.minLocal(vertexPos);
        max.maxLocal(vertexPos);

        SkeletonNode initialNode = new SkeletonNode();
        coordSys.project(vertexPos, initialNode.p);
        initialNodes.add(initialNode);

        MovingNode movingNode = ctx.createMovingNode();
        movingNode.skelNode = initialNode;
        return movingNode;
    }


    private void initBisectors() {
        List<MovingNode> degenerates = new LinkedList<>();

        for(MovingNode node : ctx.getNodes()) {
            boolean validBisector = node.calcBisector(distanceSign);
            if(!validBisector)
                degenerates.add(node);
        }

        for(MovingNode degenerateNode : degenerates) {
            // Check if 'degenerateNode' was already removed in previous handleInit() calls
            if(degenerateNode.next != null)
                SkeletonEvent.handleInit(degenerateNode, ctx);
        }
    }


    private void initEvents() {
        List<MovingNode> reflexNodes = new LinkedList<>();

        for(MovingNode current : ctx.getNodes()) {
            current.leaveSkeletonNode();

            current.updateEdge();
            ctx.tryQueueEdgeEvent(current, current.next);

            if(current.isReflex())
                reflexNodes.add(current);
        }

        // Process the reflex nodes after all edges have been initialized with updateEdge().
        for(MovingNode reflex : reflexNodes)
            SkeletonEvent.createSplitEvents(reflex, ctx);
    }


    private void scale(float dist) {
        if(dist == 0)
            return;

        Vector2f dir = new Vector2f();

        for(MovingNode node : ctx.getNodes()) {
            dir.set(node.bisector).multLocal(dist);
            node.skelNode.p.addLocal(dir);

            assert !isInvalid(node.skelNode.p) : "Invalid position after scale: bisector=" + node.bisector + ", dir=" + dir;
        }
    }


    private boolean isInvalid(Vector2f v) {
        return Float.isNaN(v.x) || Float.isInfinite(v.x);
    }


    public SkeletonVisualization getVisualization() {
        return new SkeletonVisualization(coordSys, initialNodes, ctx);
    }
}
