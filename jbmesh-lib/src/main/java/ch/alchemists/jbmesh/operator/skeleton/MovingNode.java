package ch.alchemists.jbmesh.operator.skeleton;

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

    private final ArrayList<SkeletonEvent> events = new ArrayList<>(); // ArrayList is faster than HashSet. Does its performance scale properly?


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
    public boolean calcBisector(SkeletonContext ctx) {
        return calcBisector(ctx, false);
    }

    /**
     * @param init True if the calculation is supposed to initialize the bisector.
     * @return True if bisector is valid and polygon is not degenerated at this corner.
     */
    public boolean calcBisector(SkeletonContext ctx, boolean init) {
        if(next.next == this)
            return false;

        // Calc direction to neighbor nodes. Make sure there's enough distance for stable calculation.
        Vector2f vPrev = prev.skelNode.p.subtract(skelNode.p);
        float vPrevLength = vPrev.length();
        if(vPrevLength < ctx.epsilon) {
            setDegenerate();
            return false;
        }

        Vector2f vNext = next.skelNode.p.subtract(skelNode.p);
        float vNextLength = vNext.length();
        if(vNextLength < ctx.epsilon) {
            setDegenerate();
            return false;
        }

        // Normalize
        vPrev.divideLocal(vPrevLength);
        vNext.divideLocal(vNextLength);

        // Check if edges point in opposite directions with an angle of 180° between them
        float cos = vPrev.dot(vNext);
        if(cos < ctx.epsilonMinusOne) {
            // Rotate vPrev by 90° counterclockwise
            bisector.x = -vPrev.y * ctx.distanceSign;
            bisector.y = vPrev.x * ctx.distanceSign;
            reflex = false;
        }
        else {
            // This fixes some cases where 90° bisectors (between adjacent edges that point in 180° different directions)
            // don't degenerate as they should. Presumably because these nodes advance too much (without being considered reflex)
            // and then lie on the wrong side of an approaching edge, and/or because of floating point inaccuracy.
            // Therefore we must ensure that vPrev (still) lies left of vNext. This is only a valid check if the node was not reflex
            // and the angle between vPrev and vNext is less than 90°.
            // Another way for catching more degenerates is to increase EPSILON.
            // TODO: THIS CREATES NEW ERRORS WHEN GROWING POLYGONS (see bug22).
            /*boolean reflexBefore = init || reflex;
            if(!reflexBefore && cos > 0 && vPrev.determinant(vNext) > 0) {
                setDegenerate();
                return false;
            }*/

            bisector.set(vPrev).addLocal(vNext).normalizeLocal();
            float sin = vPrev.determinant(bisector);

            // Check if degenerated
            if(Math.abs(sin) < ctx.epsilon) {
                setDegenerate();
                return false;
            }
            else {
                float speed = ctx.distanceSign / sin;
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
