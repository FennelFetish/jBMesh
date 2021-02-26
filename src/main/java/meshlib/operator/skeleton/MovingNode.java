package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;
import java.util.ArrayList;

class MovingNode {
    public final String id;
    public SkeletonNode skelNode;

    public MovingNode next = null;
    public MovingNode prev = null;

    public final Vector2f edgeDir = new Vector2f();
    public float edgeCollapseTime = 0;

    // Bisector points in move direction which depends on whether we're growing or shrinking. Length determines speed.
    public final Vector2f bisector = new Vector2f();
    private boolean reflex = false;

    private final ArrayList<SkeletonEvent> events = new ArrayList<>();


    MovingNode(String id) {
        this.id = id;
    }


    public boolean isReflex() {
        return reflex;
    }


    public void addEvent(SkeletonEvent event) {
        events.add(event);
    }

    public void removeEvent(SkeletonEvent event) {
        boolean removed = events.remove(event);
        assert removed;
    }

    public boolean tryRemoveEvent(SkeletonEvent event) {
        return events.remove(event);
    }

    public void clearEvents() {
        events.clear();
    }

    public Iterable<SkeletonEvent> events() {
        return events;
    }


    /**
     * @return True if bisector is valid and polygon is not degenerated at this corner.
     */
    public boolean calcBisector(float distanceSign) {
        if(next.next == this)
            return false;

        // Calc direction to neighbor nodes. Make sure there's enough distance for stable calculation.
        Vector2f vPrev = prev.skelNode.p.subtract(skelNode.p);
        float vPrevLength = vPrev.length();
        if(vPrevLength < SkeletonContext.EPSILON) {
            setDegenerate();
            return false;
        }

        Vector2f vNext = next.skelNode.p.subtract(skelNode.p);
        float vNextLength = vNext.length();
        if(vNextLength < SkeletonContext.EPSILON) {
            setDegenerate();
            return false;
        }

        // Normalize
        vPrev.divideLocal(vPrevLength);
        vNext.divideLocal(vNextLength);

        // Check if edges point in opposite directions
        float cos = vPrev.dot(vNext);
        if(cos < SkeletonContext.EPSILON_MINUS_ONE) {
            // Rotate vPrev by 90° counterclockwise
            bisector.x = -vPrev.y * distanceSign;
            bisector.y = vPrev.x * distanceSign;
            reflex = false;
        }
        else {
            bisector.set(vPrev).addLocal(vNext).normalizeLocal();
            float sin = vPrev.determinant(bisector);

            // Check if degenerated
            if(Math.abs(sin) < SkeletonContext.EPSILON) {
                setDegenerate();
                return false;
            }
            else {
                float speed = distanceSign / sin;
                bisector.multLocal(speed);
                reflex = (bisector.dot(vPrev) < 0);
            }
        }

        return true;
    }


    public void updateEdge() {
        edgeDir.set(next.skelNode.p).subtractLocal(skelNode.p);
        float edgeLength = edgeDir.length();
        edgeDir.divideLocal(edgeLength); // Normalize

        float edgeShrinkSpeed = bisector.dot(edgeDir);
        edgeShrinkSpeed -= next.bisector.dot(edgeDir); // equivalent to: edgeShrinkSpeed += next.bisector.dot(edgeDir.negate());

        if(edgeShrinkSpeed > 0)
            edgeCollapseTime = edgeLength / edgeShrinkSpeed;
        else
            edgeCollapseTime = SkeletonEvent.INVALID_TIME;
    }


    private void setDegenerate() {
        bisector.zero();
        reflex = false;
    }


    public void leaveSkeletonNode() {
        // Leave a SkeletonNode at old place and create new one
        SkeletonNode oldSkelNode = skelNode;
        skelNode = new SkeletonNode();
        skelNode.p.set(oldSkelNode.p);
        oldSkelNode.addEdge(skelNode);
    }


    @Override
    public String toString() {
        if(reflex)
            return "MovingNode{" + id + " (reflex)}";
        else
            return "MovingNode{" + id + "}";
    }
}
