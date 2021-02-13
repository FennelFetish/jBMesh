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
import meshlib.util.Profiler;

public class StraightSkeleton {
    private final BMesh bmesh;
    private final FaceOps faceOps;
    private final Vec3Property<Vertex> propPosition;

    private float offsetDistance = 0.0f; // Absolute value
    private float distanceSign = 1.0f;

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
        distanceSign = Math.signum(distance);
        offsetDistance = Math.abs(distance);
    }


    public void apply(Face face) {
        //System.out.println("===== Apply StraightSkeleton, distance: " + (offsetDistance*distanceSign) + " =====");
        List<Vertex> vertices = face.getVertices();
        assert vertices.size() >= 3;

        ctx.reset(offsetDistance, distanceSign);

        Vector3f n = faceOps.normal(face);
        coordSys = new PlanarCoordinateSystem(propPosition.get(vertices.get(0)), propPosition.get(vertices.get(1)), n);

        createNodes(vertices);

        if(offsetDistance != 0) {
            initBisectors();

            try(Profiler p = Profiler.start("StraightSkeleton.initEvents")) {
                initEvents();
            }

            try(Profiler p = Profiler.start("StraightSkeleton.loop")) {
                loop();
            }
        }
    }


    private void loop() {
        ctx.time = 0;

        while(true) {
            //ctx.printEvents();
            SkeletonEvent event = ctx.pollQueue();
            if(event == null) {
                scale((offsetDistance - ctx.time) * distanceSign);
                return;
            }

            scale((event.time - ctx.time) * distanceSign);
            ctx.time = event.time;

            //try(Profiler p = Profiler.start("StraightSkeleton.loop.handle")) {
                //System.out.println("{{ handle: " + event);
                event.handle(ctx);
                //System.out.println("}} handled");
            //}
        }
    }


    private void createNodes(List<Vertex> vertices) {
        initialNodes.clear();
        initialNodes.ensureCapacity(vertices.size());

        Vector3f vertexPos = new Vector3f();
        MovingNode first = createNode(vertices.get(0), vertexPos);
        MovingNode prev = first;

        for(int i=1; i<vertices.size(); ++i) {
            MovingNode movingNode = createNode(vertices.get(i), vertexPos);
            movingNode.prev = prev;
            prev.next = movingNode;

            prev = movingNode;
        }

        first.prev = prev;
        prev.next = first;
    }

    private MovingNode createNode(Vertex vertex, Vector3f vertexPos) {
        propPosition.get(vertex, vertexPos);

        SkeletonNode initialNode = new SkeletonNode();
        coordSys.project(vertexPos, initialNode.p);
        initialNodes.add(initialNode);

        MovingNode movingNode = ctx.createMovingNode();
        movingNode.skelNode = initialNode;
        return movingNode;
    }


    private void initBisectors() {
        List<MovingNode> degenerates = new ArrayList<>();

        for(MovingNode node : ctx.getNodes()) {
            boolean validBisector = node.calcBisector(distanceSign);
            if(!validBisector)
                degenerates.add(node);
        }

        for(MovingNode degenerate : degenerates) {
            // Check if 'degenerate' was already removed in previous handleInit() calls
            if(degenerate.next != null)
                SkeletonEvent.handleInit(degenerate, ctx);
        }
    }


    private void initEvents() {
        List<MovingNode> reflexNodes = new ArrayList<>();

        for(MovingNode current : ctx.getNodes()) {
            current.leaveSkeletonNode();

            current.calcEdgeLengthChange(distanceSign);
            ctx.tryQueueEdgeEvent(current, current.next);

            if(current.isReflex())
                reflexNodes.add(current);
        }

        for(MovingNode reflex : reflexNodes)
            initSplitEvents(reflex);
    }

    private void initSplitEvents(MovingNode reflexNode) {
        MovingNode current = reflexNode.next.next;
        MovingNode end = reflexNode.prev.prev; // exclusive

        // Ignore triangles, quads will also be ignored by the loop condition below
        if(current == end.next)
            return;

        for(; current != end; current = current.next)
            ctx.tryQueueSplitEvent(reflexNode, current, current.next);
    }


    private void scale(float dist) {
        if(dist == 0)
            return;

        //try(Profiler p = Profiler.start("StraightSkeleton.scale")) {
            //System.out.println("=> scaling " + ctx.movingNodes.size() + " nodes by " + dist);
            Vector2f dir = new Vector2f();

            for(MovingNode node : ctx.getNodes()) {
                dir.set(node.bisector).multLocal(dist);
                node.skelNode.p.addLocal(dir);

                assert !isInvalid(node.skelNode.p) : "Invalid position after scale: bisector=" + node.bisector + ", dir=" + dir;
            }
        //}
    }


    private boolean isInvalid(Vector2f v) {
        return Float.isNaN(v.x) || Float.isInfinite(v.x);
    }


    public SkeletonVisualization getVisualization() {
        return new SkeletonVisualization(coordSys, initialNodes, ctx);
    }
}
