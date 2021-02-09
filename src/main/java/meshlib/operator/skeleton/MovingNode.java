package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;

class MovingNode {
    public final String id;

    public MovingNode next = null;
    public MovingNode prev = null;

    public SkeletonNode skelNode;
    public final Vector2f bisector = new Vector2f(); // Length determines speed. Points outwards (in growing direction).
    public float edgeLengthChange = 0; // Change amount when shrinking. Outgoing edge from this vertex, counterclock-wise.
    private boolean reflex = false;

    // TODO: We could precalculate and store edgeDirection here because it stays the same until the node (or an adjacent node) is involved in event.


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

        Vector2f vPrev = prev.skelNode.p.subtract(skelNode.p).normalizeLocal();
        Vector2f vNext = next.skelNode.p.subtract(skelNode.p).normalizeLocal();

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
                bisector.zero();
                reflex = false;
                return false;
            }
            else {
                float speed = 1.0f / sin;
                bisector.multLocal(speed);

                // Calc edge length change (same for both adjacent edges)
                float edgeChange = bisector.dot(vPrev);
                prev.edgeLengthChange += edgeChange;
                edgeLengthChange      += edgeChange;

                // Check for reflex vertices (concave corners)
                reflex = ((edgeChange > 0.0f) == (distanceSign < 0.0f));
            }
        }

        return true;
    }


    public void calcEdgeLengthChange() {
        Vector2f vDiff = next.skelNode.p.subtract(skelNode.p).normalizeLocal();
        edgeLengthChange = bisector.dot(vDiff);

        // Equivalent to: edgeLengthChange += next.bisector.dot(vDiff.negate());
        edgeLengthChange -= next.bisector.dot(vDiff);
    }


    @Override
    public String toString() {
        return "MovingNode{" + id + "}";
    }
}
