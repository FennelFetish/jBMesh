package meshlib.operator.skeleton;

import com.jme3.math.Vector2f;

class MovingNode {
    public final int id;

    public MovingNode next = null;
    public MovingNode prev = null;

    public SkeletonNode skelNode;
    public final Vector2f bisector = new Vector2f(); // Length determines speed
    public float edgeLengthChange = 0; // Change amount when shrinking. Outgoing edge from this vertex, counterclock-wise.
    public boolean reflex = false;


    public MovingNode(int id) {
        this.id = id;
    }


    public void reset() {
        bisector.zero();
        edgeLengthChange = 0;
        reflex = false;
    }


    /**
     * @return Degenerate
     */
    public boolean calcBisector() {
        if(next.next == this)
            return true;

        Vector2f vPrev = prev.skelNode.p.subtract(skelNode.p).normalizeLocal();
        Vector2f vNext = next.skelNode.p.subtract(skelNode.p).normalizeLocal();

        System.out.println(this + " vPrev: " + vPrev);
        System.out.println(this + " vNext: " + vNext);

        // Check if edges point in opposite directions
        float cos = vPrev.dot(vNext);
        //if(Math.abs(cos) > 0.999f) {
        if(cos < -0.999f) {
            // Rotate vPrev by 90°
            bisector.x = -vPrev.y;
            bisector.y = vPrev.x;
            reflex = false;
        }
        else {
            bisector.set(vPrev).addLocal(vNext).normalizeLocal();

            float sin = vPrev.determinant(bisector); // cross(vPrev, bisector).length() -> but determinant can be negative!
            System.out.println(this + " sin = " + sin);
            if(Math.abs(sin) < 0.001f) {
                System.out.println("small sin, degenerate");
                bisector.zero();
                reflex = false;
                return true;
            }
            else {
                float speed = 1.0f / sin;
                bisector.multLocal(speed);

                // Calc edge length change (same for both adjacent edges)
                float edgeChange = bisector.dot(vPrev);
                prev.edgeLengthChange += edgeChange;
                edgeLengthChange      += edgeChange;

                // Check for reflex vertices (concave corners)
                reflex = (edgeChange > 0.0f);
            }
        }

        System.out.println(this + " bisector: " + bisector);

        if(isInvalid(bisector)) {
            System.out.println("invalid bisector <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            bisector.zero();
            reflex = false;
            return true;
        }

        return false;
    }


    public void calcEdgeLengthChange() {
        Vector2f vDiff = next.skelNode.p.subtract(skelNode.p).normalizeLocal();
        edgeLengthChange = bisector.dot(vDiff);

        vDiff.negateLocal();
        edgeLengthChange += next.bisector.dot(vDiff);
    }


    private boolean isInvalid(Vector2f v) {
        return Float.isNaN(v.x) || Float.isInfinite(v.x);
    }


    @Override
    public String toString() {
        return "MovingNode{" + id + "}";
    }
}
