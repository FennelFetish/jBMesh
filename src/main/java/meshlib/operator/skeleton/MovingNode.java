package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;

class MovingNode {
    public final String id;
    public SkeletonNode skelNode;

    public MovingNode next = null;
    public MovingNode prev = null;

    public final Vector2f edgeDir = new Vector2f();
    public float edgeCollapseTime = 0;

    public final Vector2f bisector = new Vector2f(); // Length determines speed. Points outwards (in growing direction).
    private boolean reflex = false;


    MovingNode(String id) {
        this.id = id;
    }


    public boolean isReflex() {
        return reflex;
    }


    // TODO: Include distanceSign into bisector?
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
            bisector.x = -vPrev.y;
            bisector.y = vPrev.x;
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
                float speed = 1.0f / sin;
                bisector.multLocal(speed);

                float edgeLengthChange = bisector.dot(vPrev);
                reflex = !sameSign(edgeLengthChange, distanceSign);
            }
        }

        return true;
    }


    public void calcEdgeLengthChange(float distanceSign) {
        edgeDir.set(next.skelNode.p).subtractLocal(skelNode.p);
        float edgeLength = edgeDir.length();
        edgeDir.normalizeLocal();

        // Change amount when shrinking. Outgoing edge from this vertex, counterclock-wise.
        float edgeLengthChange = bisector.dot(edgeDir);

        // Equivalent to: edgeLengthChange += next.bisector.dot(edgeDir.negate());
        edgeLengthChange -= next.bisector.dot(edgeDir);

        if(sameSign(edgeLengthChange, distanceSign))
            edgeCollapseTime = edgeLength / Math.abs(edgeLengthChange);
        else
            edgeCollapseTime = SkeletonEvent.INVALID_TIME;
    }

    private static boolean sameSign(float a, float b) {
        return (a >= 0) ^ (b < 0);
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
        return "MovingNode{" + id + "}";
    }
}
