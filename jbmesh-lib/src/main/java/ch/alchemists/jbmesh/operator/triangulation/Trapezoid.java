package ch.alchemists.jbmesh.operator.triangulation;

import ch.alchemists.jbmesh.util.DebugVisual;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

class Trapezoid implements Cloneable {
    enum Side {
        Undefined, Left, Right
    }

    private static final float EPSILON = 0.0001f;

    final Vector2f high = new Vector2f(); // only y is relevant
    final Vector2f low  = new Vector2f(); // only y is relevant

    TrapezoidTree.EdgeSplitNode lSeg, rSeg; // left and right edges of trapezoid

    Trapezoid d1, d2;       // Regions below (down)
    Trapezoid u1, u2, u3;   // Regions above (up)
    Side u3Side = Side.Undefined;

    //private sink
    private boolean insideState = false;


    Trapezoid() {}


    @Override
    protected Trapezoid clone() {
        Trapezoid copy = new Trapezoid();
        copy.high.set(high);
        copy.low.set(low);

        copy.lSeg = lSeg;
        copy.rSeg = rSeg;

        copy.d1 = d1;
        copy.d2 = d2;

        copy.u1 = u1;
        copy.u2 = u2;
        copy.u3 = u3;
        copy.u3Side = u3Side;

        copy.insideState = insideState;
        return copy;
    }

    private void set(Trapezoid trapezoid) {
        high.set(trapezoid.high);
        low.set(trapezoid.low);

        lSeg = trapezoid.lSeg;
        rSeg = trapezoid.rSeg;

        d1 = trapezoid.d1;
        d2 = trapezoid.d2;

        u1 = trapezoid.u1;
        u2 = trapezoid.u2;
        u3 = trapezoid.u3;
        u3Side = trapezoid.u3Side;

        insideState = trapezoid.insideState;
    }


    public boolean vertexExists(Vector2f v) {
        //return Math.abs(v.y-high.y) < EPSILON || Math.abs(v.y-low.y) < EPSILON;
        return v.isSimilar(high, EPSILON) || v.isSimilar(low, EPSILON);
    }

    public void makeInfinite() {
        high.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        low.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }


    public Trapezoid splitByVertex(Vector2f vertexSplit) {
        Trapezoid oldAbove = this;
        Trapezoid newBelow = new Trapezoid();

        // Update new trapezoid
        newBelow.high.set(vertexSplit);
        newBelow.low.set(oldAbove.low);

        newBelow.lSeg = oldAbove.lSeg;
        newBelow.rSeg = oldAbove.rSeg;

        newBelow.u1 = oldAbove;

        newBelow.d1 = oldAbove.d1;
        if(newBelow.d1 != null)
            newBelow.d1.replaceUpperNeighbor(oldAbove, newBelow);

        newBelow.d2 = oldAbove.d2;
        if(newBelow.d2 != null)
            newBelow.d2.replaceUpperNeighbor(oldAbove, newBelow);

        // Update old trapezoid
        oldAbove.low.set(vertexSplit);
        oldAbove.d1 = newBelow;
        oldAbove.d2 = null;

        return newBelow;
    }


    private void replaceUpperNeighbor(Trapezoid from, Trapezoid to) {
        if(u1 == from)
            u1 = to;
        else if(u2 == from)
            u2 = to;
        else {
            assert u3 == from;
            u3 = to;
        }
    }


    public Trapezoid splitByEdge(TrapezoidTree.EdgeSplitNode edgeSplit) {
        Trapezoid oldLeft = this;
        Trapezoid newRight = new Trapezoid();

        updateUp(oldLeft, newRight, edgeSplit);
        updateDown(oldLeft, newRight, edgeSplit);

        newRight.lSeg = edgeSplit;
        newRight.rSeg = oldLeft.rSeg;
        newRight.high.set(oldLeft.high);
        newRight.low.set(oldLeft.low);

        oldLeft.rSeg = edgeSplit;

        return newRight;
    }


    private static void updateUp(Trapezoid oldLeft, Trapezoid newRight, TrapezoidTree.EdgeSplitNode edgeSplit) {
        final float edgeX = edgeSplit.xCoordsAt(oldLeft.high.y);

        // TODO: Check if new edge connects with oldLeft.lSeg or oldLeft.rSeg ?   --> "upwards cust"

        if(oldLeft.u1 != null && oldLeft.u2 != null) {
            assert oldLeft.u1.d1 == oldLeft;
            assert oldLeft.u2.d1 == oldLeft;

            // Three regions above
            if(oldLeft.u3 != null) {
                assert oldLeft.u3.d1 == oldLeft;

                assert oldLeft.u1.rSeg == oldLeft.u2.lSeg;
                assert oldLeft.u2.rSeg == oldLeft.u3.lSeg;

                final float upperSegX1 = oldLeft.u2.lSeg.lower.x;
                final float upperSegX2 = oldLeft.u2.rSeg.lower.x;
                assert upperSegX1 < upperSegX2;

                // Intersecting left region:    |   |
                //                            ^
                if(edgeX < upperSegX1) {
                    newRight.u1 = oldLeft.u1;
                    newRight.u1.d2 = newRight;

                    newRight.u2 = oldLeft.u2;
                    newRight.u2.d1 = newRight;

                    newRight.u3 = oldLeft.u3;
                    newRight.u3.d1 = newRight;
                    newRight.u3Side = oldLeft.u3Side; // ?

                    oldLeft.u2 = null;
                    oldLeft.u3 = null;
                    oldLeft.u3Side = Side.Undefined;
                }
                // Edge connects with left upper segment:   |   |
                //                                          ^
                else if(edgeX == upperSegX1) {
                    newRight.u1 = oldLeft.u2;
                    newRight.u1.d1 = newRight;

                    newRight.u2 = oldLeft.u3;
                    newRight.u2.d1 = newRight;

                    oldLeft.u2 = null;
                    oldLeft.u3 = null;
                    oldLeft.u3Side = Side.Undefined;
                }
                // Intersecting middle region:   |   |
                //                                 ^
                else if(edgeX < upperSegX2) {
                    newRight.u1 = oldLeft.u2;
                    newRight.u1.d2 = newRight;

                    newRight.u2 = oldLeft.u3;
                    newRight.u2.d1 = newRight;

                    oldLeft.u3 = null;
                    oldLeft.u3Side = Side.Undefined;
                }
                // Edge connects with right upper segment:   |   |
                //                                               ^
                else if(edgeX == upperSegX2) {
                    newRight.u1 = oldLeft.u3;
                    newRight.u1.d1 = newRight;

                    oldLeft.u3 = null;
                    oldLeft.u3Side = Side.Undefined;
                }
                // Intersecting right region:   |   |
                //                                    ^
                else {
                    newRight.u1 = oldLeft.u3;
                    newRight.u1.d2 = newRight;
                }
            }
            // Two regions above
            else {
                //assert oldLeft.u1.d2 == null;
                //assert oldLeft.u2.d2 == null;

                assert oldLeft.u1.rSeg == oldLeft.u2.lSeg;
                final float upperSegX = oldLeft.u1.rSeg.lower.x;

                /*final float upperSegX;
                if(oldLeft.u1.rSeg != null)
                    upperSegX = oldLeft.u1.rSeg.lower.x;
                else {
                    assert oldLeft.u2.lSeg != null;
                    upperSegX = oldLeft.u2.lSeg.lower.x;
                }*/

                // Intersecting left region:   |
                //                           ^
                if(edgeX < upperSegX) {
                    newRight.u1 = oldLeft.u1;
                    newRight.u1.d2 = newRight;

                    newRight.u2 = oldLeft.u2;
                    newRight.u2.d1 = newRight;

                    oldLeft.u2 = null;
                }
                // Edges connect:   |
                //                  ^
                else if(edgeX == upperSegX) {
                    newRight.u1 = oldLeft.u2;
                    newRight.u1.d1 = newRight;

                    oldLeft.u2 = null;
                }
                // Intersecting right region:   |
                //                                ^
                else {
                    newRight.u1 = oldLeft.u2;
                    newRight.u1.d2 = newRight;
                }
            }
        }
        // One region above
        else {
            assert oldLeft.u2 == null;
            assert oldLeft.u1 != null;
            //assert oldLeft.u1.d1 == oldLeft;

            // Fresh segment
            if(oldLeft.u1.d2 == null) {
                assert oldLeft.u1.d1 != null;

                newRight.u1 = oldLeft.u1;
                newRight.u1.d2 = newRight;
            }
            // Upwards cust
            else {
                if(oldLeft.lSeg != null) {
                    //   /^
                    final float lx = oldLeft.lSeg.xCoordsAt(oldLeft.high.y);
                    if(fEq(edgeX, lx)) {
                        assert oldLeft.u1.d2 == oldLeft;

                        newRight.u1 = oldLeft.u1;
                        newRight.u1.d2 = newRight;

                        oldLeft.u1 = null;
                    }
                }
                /*else if(oldLeft.rSeg != null) {
                    //   ^\
                    float rx = oldLeft.rSeg.xCoordsAt(oldLeft.high.y);
                    if(fEq(edgeX, rx)) {
                        // Nothing to do, no connection between newRight and oldLeft.u1
                    }
                }*/
            }
        }
    }


    private static void updateDown(Trapezoid oldLeft, Trapezoid newRight, TrapezoidTree.EdgeSplitNode edgeSplit) {
        final float edgeX = edgeSplit.xCoordsAt(oldLeft.low.y);

        // Two trapezoids below
        if(oldLeft.d1 != null && oldLeft.d2 != null) {
            //assert oldLeft.d1.u1 == oldLeft;
            //assert oldLeft.d2.u1 == oldLeft;

            assert oldLeft.d1.rSeg != null;
            assert oldLeft.d2.lSeg != null;
            assert oldLeft.d1.rSeg == oldLeft.d2.lSeg;

            final float lowerSegX = oldLeft.d1.rSeg.higher.x;

            // Intersecting lower left region:  v
            //                                    |
            if(edgeX < lowerSegX) {
                newRight.d1 = oldLeft.d1;
                newRight.d1.u2 = newRight;

                newRight.d2 = oldLeft.d2;
                newRight.d2.u1 = newRight;

                oldLeft.d2 = null;
            }
            // Connecting edges:   v
            //                     |
            else if(edgeX == lowerSegX) {
                newRight.d1 = oldLeft.d2;
                newRight.d1.u1 = newRight;

                oldLeft.d2 = null;
            }
            // Intersecting lower right region:     v
            //                                    |
            else {
                newRight.d1 = oldLeft.d2;
                newRight.d1.u2 = newRight;
            }
        }
        // Only one trapezoid below (can't have zero below)
        else {
            assert oldLeft.d2 == null;
            assert oldLeft.d1 != null;

            // Fresh segment
            if(oldLeft.d1.u2 == null) {
                assert oldLeft.d1.u1 != null;
                assert oldLeft.d1.u3 == null;

                newRight.d1 = oldLeft.d1;
                newRight.d1.u2 = newRight;
            }
            else {
                //  \v
                if(oldLeft.lSeg != null && fEq(edgeX, oldLeft.lSeg.xCoordsAt(oldLeft.low.y))) {
                    newRight.d1 = oldLeft.d1;
                    newRight.d1.u2 = newRight;

                    oldLeft.d1 = null;
                }
                //  v/
                else if(oldLeft.rSeg != null && fEq(edgeX, oldLeft.rSeg.xCoordsAt(oldLeft.low.y))) {
                    // Nothing to do, no connection between newRight and oldLeft.d1
                }
                // oldLeft.d1 now has 3 upper regions
                else {
                    //   v  |
                    if(oldLeft.d1.u1 == oldLeft) {
                        newRight.d1 = oldLeft.d1;
                        newRight.d1.u3 = newRight.d1.u2;
                        newRight.d1.u2 = newRight;
                        newRight.d1.u3Side = Side.Right;
                    }
                    //   |  v
                    else {
                        assert oldLeft.d1.u2 == oldLeft;

                        newRight.d1 = oldLeft.d1;
                        newRight.d1.u3 = newRight;
                        newRight.d1.u3Side = Side.Left;

                        // TODO: Why this order ???????????????? (ported code)
                        /*newRight.d1 = oldLeft.d1;
                        newRight.d1.u3 = newRight.d1.u1;
                        newRight.d1.u3Side = Side.Left;
                        newRight.d1.u1 = oldLeft;
                        newRight.d1.u2 = newRight;*/
                    }
                }
            }
        }
    }


    private static void updateUpOld(Trapezoid oldLeft, Trapezoid newRight, TrapezoidTree.EdgeSplitNode edgeSplit) {
        if(oldLeft.u1 != null && oldLeft.u2 != null) {
            // Sub condition 1: Two trapezoids above
            if(oldLeft.u3 == null) {
                // TODO: Check where x intersection of edgeSplit at height oldLeft.high.y is
                newRight.u1 = oldLeft.u2;
                newRight.u1.d1 = newRight;
                oldLeft.u2 = null;
            }
            // Sub condition 2: Three trapezoids above
            else {
                if(oldLeft.u3Side == Side.Left) {
                    newRight.u1 = oldLeft.u2;
                    newRight.u1.d1 = newRight;
                    oldLeft.u2 = oldLeft.u1;
                    oldLeft.u1 = oldLeft.u3;
                    oldLeft.u3 = null;
                } else {
                    assert oldLeft.u3Side == Side.Right;

                    newRight.u1 = oldLeft.u2;
                    newRight.u2 = oldLeft.u3;
                    newRight.u1.d1 = newRight;
                    newRight.u2.d1 = newRight;
                    oldLeft.u2 = null;
                    oldLeft.u3 = null;
                }
            }
        }
        else {
            assert oldLeft.u1 != null;

            // Sub condition 3: Only one region above and left to right upward cusp
            // TODO: Can 'oldRegion.u1.d1' really be null?
            if(oldLeft.u1.d1 != null && oldLeft.u1.d2 != null) {
                TrapezoidTree.EdgeSplitNode seg = oldLeft.u1.d1.rSeg;
                if(seg != null && !seg.pointIsLeft(edgeSplit.lower)) {
                    newRight.u1 = oldLeft.u1;
                    oldLeft.u1.d2 = newRight;
                    oldLeft.u1 = null;
                }
            }
            // Sub condition 5: Fresh segment
            else {
                newRight.u1 = oldLeft.u1;
                newRight.u1.d1 = newRight;
            }
        }
    }


    private static void updateDownOld(Trapezoid oldTrapez, Trapezoid newTrapez, TrapezoidTree.EdgeSplitNode edgeSplit) {
        // Two trapezoids below
        if(oldTrapez.d1 != null && oldTrapez.d2 != null) {
            assert oldTrapez.d1.low.isSimilar(oldTrapez.d2.low, EPSILON);

            // Connect edges?
            if(edgeSplit.lower.isSimilar(oldTrapez.d1.high, EPSILON)) {
                // nextTrapNode = l1->node;	// Either one will do; segment threading ends here.
                newTrapez.d1 = oldTrapez.d2;
                newTrapez.d1.u1 = newTrapez;
                oldTrapez.d2 = null;
            }
            else {
                if(edgeSplit.pointIsLeft(oldTrapez.d1.high)) {
                    // nextTrapNode = oldRegion.d2->node;
                    newTrapez.d1 = oldTrapez.d2;
                    newTrapez.d1.u2 = newTrapez;
                } else {
                    // nextTrapNode = oldRegion.d1->node;
                    newTrapez.d1 = oldTrapez.d1;
                    newTrapez.d2 = oldTrapez.d2;
                    newTrapez.d1.u2 = newTrapez;
                    newTrapez.d2.u1 = newTrapez;
                    oldTrapez.d2 = null;
                }
            }
        }
        // Only one trapezoid below (can't have zero below)
        else {
            assert oldTrapez.d2 == null;
            assert oldTrapez.d1 != null;
            // nextTrapNode = d1->node;

            Trapezoid du1 = oldTrapez.d1.u1;
            Trapezoid du2 = oldTrapez.d1.u2;

            // The trapezoid below has two upper trapezoids
            if(du1 != null && du2 != null) {
                if(edgeSplit.lower.isSimilar(oldTrapez.d1.high, EPSILON)) {
                    if(du1.rSeg != null && !du1.rSeg.pointIsLeft(edgeSplit.higher)) {
                        newTrapez.d1 = oldTrapez.d1;
                        newTrapez.d1.u2 = newTrapez;
                        oldTrapez.d1 = null;
                    }
                }
                else {
                    if(oldTrapez == du1) {
                        // oldRegion is left, introduce 3rd region to the right
                        oldTrapez.d1.u2 = newTrapez;
                        oldTrapez.d1.u3 = du2;
                        oldTrapez.d1.u3Side = Side.Right;
                        newTrapez.d1 = oldTrapez.d1;
                    }
                    else {
                        // oldRegion is right, introduce 3rd region to the left
                        // TODO: Enable assertion again:
                        //assert oldTrapez == du2;

                        oldTrapez.d1.u1 = oldTrapez;
                        oldTrapez.d1.u2 = newTrapez;
                        oldTrapez.d1.u3 = du1;
                        oldTrapez.d1.u3Side = Side.Left;
                        newTrapez.d1 = oldTrapez.d1;
                    }
                }
            }
            // Fresh segment
            else {
                newTrapez.d1 = oldTrapez.d1;
                newTrapez.d1.u2 = newTrapez;
            }
        }
    }


    public boolean mergeUp() {
        // Only u1 must be set
        if(u1 == null || u2 != null || u3 != null)
            return false;

        Trapezoid up = u1;

        // u1 must be bound by same segments
        if(lSeg != up.lSeg || rSeg != up.rSeg)
            return false;

        up.d1 = d1;
        up.d2 = d2;

        if(d1.u1 == this)
            d1.u1 = up;
        if(d1.u2 == this)
            d1.u2 = up;
        if(d1.u3 == this)
            d1.u3 = up;

        if(d2.u1 == this)
            d2.u1 = up;
        if(d2.u2 == this)
            d2.u2 = up;
        if(d2.u3 == this)
            d2.u3 = up;

        // 'this' disappears
        return true;
    }


    public boolean mergeDown() {
        // Only d1 must be set
        if(d1 == null || d2 != null)
            return false;

        Trapezoid down = d1;

        // d1 must be bound by same segments
        if(lSeg != down.lSeg || rSeg != down.rSeg)
            return false;

        return true;
    }


    private static boolean fEq(float a, float b) {
        return Math.abs(a-b) < EPSILON;
    }


    private void printInfo() {
        System.out.println(">>>>>>>> Info: " + this);
        System.out.println("  high: " + high);
        System.out.println("  low:  " + low);

        if(u1 != null) System.out.println("  u1: " + u1);
        if(u2 != null) System.out.println("  u2: " + u2);
        if(u3 != null) {
            System.out.println("  u3: " + u3 + " (side: " + u3Side.name() + ")");
        }

        if(d1 != null) System.out.println("  d1: " + d1);
        if(d2 != null) System.out.println("  d2: " + d2);

        if(lSeg != null) System.out.println("  lSeg: " + lSeg);
        if(rSeg != null) System.out.println("  rSeg: " + rSeg);
    }

    public void addDebugVis(int regionNr) {
        Vector3f[] corners = getCorners();

        DebugVisual dbg = DebugVisual.get("Seidel");
        dbg.addFace(DebugVisual.getColor(regionNr), corners);

        // Add text
        float scale = 4;
        /*final float scaleFactor = 20;
        if(lSeg != null) {
            v1.multLocal(scaleFactor);
            v2.multLocal(scaleFactor);
            scale += 2*scaleFactor;
        }
        if(rSeg != null) {
            v3.multLocal(scaleFactor);
            v4.multLocal(scaleFactor);
            scale += 2*scaleFactor;
        }*/

        /*Vector3f p = corners[0].add(corners[1]).addLocal(corners[2]).addLocal(corners[3]);
        p.divideLocal(scale);*/
        Vector3f p = getMidPoint();
        dbg.addPoint(p);
        dbg.addText(new Vector3f(p.x, p.y, 0), Integer.toString(regionNr));

        Vector3f off = new Vector3f(0.05f, 0.05f, 0);

        if(u1 != null)
            dbg.addLine(p.add(off), u1.getMidPoint().add(off));
        if(u2 != null)
            dbg.addLine(p.add(off), u2.getMidPoint().add(off));
        if(u3 != null)
            dbg.addLine(p.add(off), u3.getMidPoint().add(off));

        if(d1 != null)
            dbg.addLine(p.subtract(off), d1.getMidPoint().subtract(off));
        if(d2 != null)
            dbg.addLine(p.subtract(off), d2.getMidPoint().subtract(off));
    }


    public Vector3f getMidPoint() {
        Vector3f[] corners = getCorners();
        Vector3f p = corners[0];
        for(int i=1; i<corners.length; ++i)
            p.addLocal(corners[i]);
        p.divideLocal(corners.length);
        return p;
    }


    public Vector3f[] getCorners() {
        // Limit y values
        float highY = high.y;
        if(Float.isInfinite(highY))
            highY = 100;

        float lowY = low.y;
        if(Float.isInfinite(lowY))
            lowY = -100;

        // Calc corner points for face
        Vector3f v1 = new Vector3f(-100, highY, 0);
        Vector3f v2 = new Vector3f(-100, lowY, 0);
        if(lSeg != null) {
            v1.x = lSeg.xCoordsAt(v1.y);
            v2.x = lSeg.xCoordsAt(v2.y);
        }

        Vector3f v3 = new Vector3f(100, lowY, 0);
        Vector3f v4 = new Vector3f(100, highY, 0);
        if(rSeg != null) {
            v3.x = rSeg.xCoordsAt(v3.y);
            v4.x = rSeg.xCoordsAt(v4.y);
        }

        return new Vector3f[] {v1, v2, v3, v4};
    }
}
