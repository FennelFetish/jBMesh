package ch.alchemists.jbmesh.operator.triangulation;

import ch.alchemists.jbmesh.util.DebugVisual;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

class TrapezoidTree {
    private static final float EPSILON = 0.0001f;

    interface Node {
        Node addVertex(Vector2f v);
        void addEdge(Vector2f higher, Vector2f lower);

        default void print(int level) {
            TrapezoidTree.print(level, getClass().getSimpleName());
        }
    }

    private abstract class SplitNode implements Node {
        public Node left;  // below
        public Node right; // above

        protected SplitNode() {}

        public final void changeChild(RegionNode from, SplitNode to) {
            if(from == left)
                left = to;
            else {
                assert from == right;
                right = to;
            }
        }

        @Override
        public void print(int level) {
            TrapezoidTree.print(level, getClass().getSimpleName());
            left.print(level + 1);
            right.print(level + 1);
        }
    }

    class VertexSplitNode extends SplitNode {
        private final Vector2f vertex = new Vector2f();

        public VertexSplitNode(Vector2f v) {
            vertex.set(v);
        }

        @Override
        public Node addVertex(Vector2f v) {
            if(v.y <= vertex.y)
                return left.addVertex(v);
            else
                return right.addVertex(v);
        }

        @Override
        public void addEdge(Vector2f higher, Vector2f lower) {
            if(lower.y < vertex.y)
                left.addEdge(higher, lower);
            if(higher.y > vertex.y)
                right.addEdge(higher, lower);
        }

        @Override
        public void print(int level) {
            super.print(level);
            DebugVisual.get("Seidel").addLine(new Vector3f(-100, vertex.y, 0), new Vector3f(100, vertex.y, 0));
        }
    }

    class EdgeSplitNode extends SplitNode {
        private final Vector2f higher = new Vector2f(); // Start
        private final Vector2f lower  = new Vector2f(); // End

        public EdgeSplitNode(Vector2f higher, Vector2f lower) {
            this.higher.set(higher);
            this.lower.set(lower);
        }

        @Override
        public Node addVertex(Vector2f v) {
            Vector2f dir = lower.subtract(higher);
            Vector2f rel = v.subtract(higher);

            float det = dir.determinant(rel);
            if(det <= 0)
                return left.addVertex(v);
            else
                return right.addVertex(v);
        }

        @Override
        public void addEdge(Vector2f higher, Vector2f lower) {
            Vector2f nodeDir   = this.lower.subtract(this.higher);
            Vector2f relHigher = higher.subtract(this.higher);
            Vector2f relLower  = lower.subtract(this.higher);

            float detHigher = nodeDir.determinant(relHigher);
            float detLower  = nodeDir.determinant(relLower);

            if(detHigher < 0 || detLower < 0)
                left.addEdge(higher, lower);
            if(detHigher > 0 || detLower > 0)
                right.addEdge(higher, lower);
        }

        @Override
        public void print(int level) {
            super.print(level);
            DebugVisual.get("Seidel").addLine(new Vector3f(higher.x, higher.y, 0), new Vector3f(lower.x, lower.y, 0));
        }

        public boolean pointIsLeft(Vector2f p) {
            Vector2f nodeDir = this.lower.subtract(this.higher);
            Vector2f rel = p.subtract(this.higher);
            float det = nodeDir.determinant(rel);
            return det < 0; // Left = smaller x coords
        }

        @Override
        public String toString() {
            return "EdgeSplitNode[" + higher + " -> " + lower + "]";
        }
    }

    class RegionNode implements Node {
        private final int regionNr;
        private SplitNode parent = null;

        private final Vector2f high = new Vector2f(); // only y is relevant
        private final Vector2f low  = new Vector2f(); // only y is relevant
        private RegionNode u1, u2; // Regions above (up)
        private RegionNode d1, d2; // Regions below (down)

        private RegionNode u3; // Third region above
        private boolean u3Side; // false = left, true = right

        private EdgeSplitNode lSeg, rSeg; // left and right edges of trapezoid

        //private sink
        private boolean insideState = false;


        public RegionNode(SplitNode parent) {
            regionNr = nextRegionNr++;
            this.parent = parent;
        }

        @Override
        public Node addVertex(Vector2f v) {
            if(v.isSimilar(high, EPSILON) || v.isSimilar(low, EPSILON))
                return this;

            System.out.println("OOOOO Add Vertex " + v + " to " + this);

            SplitNode split = new VertexSplitNode(v);
            RegionNode newRegion = new RegionNode(split);
            RegionNode oldRegion = this;
            split.left  = newRegion; // below
            split.right = oldRegion; // above

            if(parent == null)
                root = split;
            else {
                parent.changeChild(this, split);
                parent = split;
            }

            // Update new region
            newRegion.high.set(v);
            newRegion.low.set(oldRegion.low);
            newRegion.u1 = oldRegion;
            newRegion.d1 = oldRegion.d1;
            newRegion.d2 = oldRegion.d2;
            newRegion.lSeg = oldRegion.lSeg;
            newRegion.rSeg = oldRegion.rSeg;

            // Update old region
            if(oldRegion.d1 != null) {
                if(oldRegion.d1.u1 == oldRegion)
                    oldRegion.d1.u1 = newRegion;
                else if(oldRegion.d1.u2 == oldRegion)
                    oldRegion.d1.u2 = newRegion;
            }

            if(oldRegion.d2 != null) {
                if(oldRegion.d2.u1 == oldRegion)
                    oldRegion.d2.u1 = newRegion;
                else if(oldRegion.d2.u2 == oldRegion)
                    oldRegion.d2.u2 = newRegion;
            }

            oldRegion.low.set(v);
            oldRegion.d1 = newRegion;
            oldRegion.d2 = null;

            System.out.println("=== oldRegion:");
            oldRegion.printInfo();
            System.out.println("=== newRegion:");
            newRegion.printInfo();

            return newRegion;
        }

        @Override
        public void addEdge(Vector2f higher, Vector2f lower) {
            System.out.println("OOOOO Add Edge " + higher + " -> " + lower + " to " + this);

            EdgeSplitNode split = new EdgeSplitNode(higher, lower);
            RegionNode newRegion = new RegionNode(split);
            RegionNode oldRegion = this;
            split.left = oldRegion;
            split.right = newRegion;

            if(parent == null)
                root = split;
            else {
                parent.changeChild(this, split);
                parent = split;
            }

            if(u1 != null && u2 != null) {
                // Sub condition 1: Two trapezoids above
                if(u3 == null) {
                    newRegion.u1 = oldRegion.u2;
                    oldRegion.u2.d1 = newRegion;
                    oldRegion.u2 = null;
                }
                // Sub condition 2: Three trapezoids above
                else {
                    // u3 right
                    if(u3Side) {
                        newRegion.u1 = oldRegion.u2;
                        newRegion.u2 = oldRegion.u3;
                        oldRegion.u2.d1 = newRegion;
                        oldRegion.u3.d1 = newRegion;
                        oldRegion.u2 = null;
                        oldRegion.u3 = null;
                    }
                    // u3 left
                    else {
                        newRegion.u1 = oldRegion.u2;
                        oldRegion.u2.d1 = newRegion;
                        oldRegion.u2 = oldRegion.u1;
                        oldRegion.u1 = oldRegion.u3;
                        oldRegion.u3 = null;
                    }
                }
            }
            else {
                assert u1 != null;

                // Sub condition 3: Only one region above and left to right upward cusp
                // TODO: Can 'oldRegion.u1.d1' really be null?
                if(oldRegion.u1.d1 != null && oldRegion.u1.d2 != null) {
                    EdgeSplitNode edgeSplit = oldRegion.u1.d1.rSeg;
                    if(edgeSplit != null && !edgeSplit.pointIsLeft(lower)) {
                        newRegion.u1 = oldRegion.u1;
                        oldRegion.u1.d2 = newRegion;
                        oldRegion.u1 = null;
                    }
                }
                // Sub condition 5: Fresh segment
                else {
                    newRegion.u1 = oldRegion.u1;
                    newRegion.u1.d1 = newRegion;
                }
            }

            // Two trapezoids below
            if(d1 != null && d2 != null) {
                assert d1.low.isSimilar(d2.low, EPSILON);

                // Connect edges?
                if(lower.isSimilar(d1.high, EPSILON)) {
                    // nextTrapNode = l1->node;	// Either one will do; segment threading ends here.
                    newRegion.d1 = oldRegion.d2;
                    newRegion.d1.u1 = newRegion;
                    oldRegion.d2 = null;
                }
                else {
                    if(split.pointIsLeft(oldRegion.d1.high)) {
                        // nextTrapNode = oldRegion.d2->node;
                        newRegion.d1 = oldRegion.d2;
                        newRegion.d1.u2 = newRegion;
                    } else {
                        // nextTrapNode = oldRegion.d1->node;
                        newRegion.d1 = oldRegion.d1;
                        newRegion.d2 = oldRegion.d2;
                        newRegion.d1.u2 = newRegion;
                        newRegion.d2.u1 = newRegion;
                        oldRegion.d2 = null;
                    }
                }
            }
            // Only one trapezoid below (can't have zero below)
            else {
                assert oldRegion.d1 != null;
                // nextTrapNode = d1->node;

                RegionNode du1 = d1.u1;
                RegionNode du2 = d1.u2;

                // The trapezoid below has two upper trapezoids
                if(du1 != null && du2 != null) {
                    if(lower.isSimilar(d1.high, EPSILON)) {
                        if(du1.rSeg != null && !du1.rSeg.pointIsLeft(higher)) {
                            newRegion.d1 = oldRegion.d1;
                            newRegion.d1.u2 = newRegion;
                            oldRegion.d1 = null;
                        }
                    }
                    else {
                        if(oldRegion == du1) {
                            // oldRegion is left, introduce 3rd region to the right
                            oldRegion.d1.u2 = newRegion;
                            oldRegion.d1.u3 = du2;
                            oldRegion.d1.u3Side = true; // true = right
                            newRegion.d1 = oldRegion.d1;
                        }
                        else {
                            // oldRegion is right, introduce 3rd region to the left
                            assert oldRegion == du2;

                            oldRegion.d1.u1 = oldRegion;
                            oldRegion.d1.u2 = newRegion;
                            oldRegion.d1.u3 = du1;
                            oldRegion.d1.u3Side = false;
                            newRegion.d1 = oldRegion.d1;
                        }
                    }
                }
                // Fresh segment
                else {
                    newRegion.d1 = oldRegion.d1;
                    newRegion.d1.u2 = newRegion;
                }
            }

            newRegion.lSeg = split;
            newRegion.rSeg = oldRegion.rSeg;
            newRegion.high.set(oldRegion.high);
            newRegion.low.set(oldRegion.low);

            oldRegion.rSeg = split;

            System.out.println("=== oldRegion:");
            oldRegion.printInfo();
            System.out.println("=== newRegion:");
            newRegion.printInfo();
        }

        /*private boolean dividedByEdge(Vector2f higher, Vector2f lower) {
            if(lSeg == null && rSeg == null)
                return true;

            boolean lSegSide = false;
            if(lSeg != null)
                lSegSide = lSeg.pointIsLeft(higher);

            boolean rSegSide = false;
            if(rSeg != null)
                rSegSide = rSeg.pointIsLeft(higher);
        }*/

        @Override
        public void print(int level) {
            TrapezoidTree.print(level, "RegionNode[" + regionNr + "]");

            float highY = high.y;
            if(Float.isInfinite(highY))
                highY = 100;

            float lowY = low.y;
            if(Float.isInfinite(lowY))
                lowY = -100;

            Vector3f v1 = new Vector3f(-100, highY, 0);
            Vector3f v2 = new Vector3f(-100, lowY, 0);
            if(lSeg != null) {
                v1.x = xIntersect(v1.y, lSeg);
                v2.x = xIntersect(v2.y, lSeg);
            }

            Vector3f v3 = new Vector3f(100, lowY, 0);
            Vector3f v4 = new Vector3f(100, highY, 0);
            if(rSeg != null) {
                v3.x = xIntersect(v3.y, rSeg);
                v4.x = xIntersect(v4.y, rSeg);
            }

            DebugVisual dbg = DebugVisual.get("Seidel");
            dbg.addFace(v1, v2, v3, v4);

            Vector3f p = v1.add(v2).addLocal(v3).addLocal(v4);
            p.divideLocal(4f);
            dbg.addText(new Vector3f(p.x, p.y, 0), Integer.toString(regionNr));
        }

        private void printInfo() {
            System.out.println(">>>>>>>> Info: " + this);
            System.out.println("  high: " + high);
            System.out.println("  low:  " + low);

            if(u1 != null) System.out.println("  u1: " + u1);
            if(u2 != null) System.out.println("  u2: " + u2);
            if(u3 != null) {
                System.out.println("  u3: " + u3 + " (side: " + (u3Side ? "right" : "left") + ")");
            }

            if(d1 != null) System.out.println("  d1: " + d1);
            if(d2 != null) System.out.println("  d2: " + d2);

            if(lSeg != null) System.out.println("  lSeg: " + lSeg);
            if(rSeg != null) System.out.println("  rSeg: " + rSeg);
        }

        private float xIntersect(float y, EdgeSplitNode seg) {
            Vector2f segDiff = seg.lower.subtract(seg.higher);
            float xChange = segDiff.x / -segDiff.y;
            float yDiff = seg.higher.y - y;
            float x = seg.higher.x + yDiff*xChange;
            return x;
        }

        @Override
        public String toString() {
            return "RegionNode[" + regionNr + "]";
        }
    }



    private Node root;
    private int nextRegionNr = 1;

    public TrapezoidTree() {
        RegionNode root = new RegionNode(null);
        root.high.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        root.low.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

        this.root = root;
    }

    public void addEdge(Vector2f v1, Vector2f v2) {
        System.out.println("----------------------------");
        if(v1.y >= v2.y)
            addEdgeInternal(v1, v2);
        else
            addEdgeInternal(v2, v1);
    }

    private void addEdgeInternal(Vector2f higher, Vector2f lower) {
        Node upperBound = root.addVertex(higher);
        //System.out.println("=== Added higher: " + higher);
        //printTree();

        Node lowerBound = root.addVertex(lower);
        //System.out.println("=== Added lower: " + lower);
        //printTree();

        root.addEdge(higher, lower);
        //System.out.println("=== Added edge");
        //printTree();
    }


    public void printTree() {
        root.print(0);
    }

    private static void print(int level, String name) {
        String str = "";
        for(int i=0; i<level; ++i)
            str = "·   " + str;
        str += name;

        System.out.println(str);
    }
}
