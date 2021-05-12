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

    private static abstract class SplitNode implements Node {
        public Node rightHigh; // above
        public Node leftLow;  // below

        protected SplitNode() {}

        public final void changeChild(RegionNode from, SplitNode to) {
            if(from == leftLow)
                leftLow = to;
            else {
                assert from == rightHigh;
                rightHigh = to;
            }
        }

        @Override
        public void print(int level) {
            TrapezoidTree.print(level, getClass().getSimpleName());
            leftLow.print(level + 1);
            rightHigh.print(level + 1);
        }
    }

    static class VertexSplitNode extends SplitNode {
        final Vector2f vertex = new Vector2f();

        public VertexSplitNode(Vector2f v) {
            vertex.set(v);
        }

        @Override
        public Node addVertex(Vector2f v) {
            /*if(v.isSimilar(vertex, EPSILON))
                return this;*/

            /*if(v.y > vertex.y)
                return rightHigh.addVertex(v);
            else
                return leftLow.addVertex(v);*/

            if(v.y > vertex.y)
                return rightHigh.addVertex(v);
            if(v.y < vertex.y)
                return leftLow.addVertex(v);

            if(v.x > vertex.x)
                return rightHigh.addVertex(v);
            else if(v.x < vertex.x)
                return leftLow.addVertex(v);

            return this;
        }

        @Override
        public void addEdge(Vector2f higher, Vector2f lower) {
            if(higher.y > vertex.y)
                rightHigh.addEdge(higher, lower);
            if(lower.y < vertex.y)
                leftLow.addEdge(higher, lower);
        }

        @Override
        public void print(int level) {
            super.print(level);
            DebugVisual.get("Seidel").addLine(new Vector3f(-100, vertex.y, 0), new Vector3f(100, vertex.y, 0));
        }
    }

    static class EdgeSplitNode extends SplitNode {
        final Vector2f higher = new Vector2f(); // Start
        final Vector2f lower  = new Vector2f(); // End

        public EdgeSplitNode(Vector2f higher, Vector2f lower) {
            this.higher.set(higher);
            this.lower.set(lower);
        }

        @Override
        public Node addVertex(Vector2f v) {
            Vector2f dir = lower.subtract(higher);
            Vector2f rel = v.subtract(higher);

            float det = dir.determinant(rel);
            /*if(det <= 0)
                return leftLow.addVertex(v);
            else
                return rightHigh.addVertex(v);*/

            if(det < 0)
                return leftLow.addVertex(v);
            else if(det > 0)
                return rightHigh.addVertex(v);

            return null; // TODO: null?
        }

        @Override
        public void addEdge(Vector2f higher, Vector2f lower) {
            /*Vector2f nodeDir   = this.lower.subtract(this.higher);
            Vector2f relHigher = higher.subtract(this.higher);
            Vector2f relLower  = lower.subtract(this.higher);

            float detHigher = nodeDir.determinant(relHigher);
            float detLower  = nodeDir.determinant(relLower);

            //if(detHigher <= 0 || detLower <= 0)
            if(detHigher < 0 || detLower < 0)
                leftLow.addEdge(higher, lower);
            if(detHigher > 0 || detLower > 0)
                rightHigh.addEdge(higher, lower);*/

            Vector2f highCut = new Vector2f();
            highCut.y = Math.min(higher.y, this.higher.y);
            highCut.x = xCoordsAt(highCut.y, higher, lower);

            Vector2f lowCut = new Vector2f();
            lowCut.y = Math.max(lower.y, this.lower.y);
            lowCut.x = xCoordsAt(lowCut.y, higher, lower);

            Vector2f nodeDir   = this.lower.subtract(this.higher);
            Vector2f relHigher = highCut.subtract(this.higher);
            Vector2f relLower  = lowCut.subtract(this.higher);

            float detHigher = nodeDir.determinant(relHigher);
            float detLower  = nodeDir.determinant(relLower);

            if(detHigher < 0 || detLower < 0)
                leftLow.addEdge(higher, lower);
            if(detHigher > 0 || detLower > 0)
                rightHigh.addEdge(higher, lower);
        }

        public boolean pointIsLeft(Vector2f p) {
            Vector2f nodeDir = lower.subtract(higher);
            Vector2f rel = p.subtract(higher);
            float det = nodeDir.determinant(rel);
            return det <= 0; // Left = smaller x coords
        }

        public float xCoordsAt(float y) {
            return xCoordsAt(y, higher, lower);
        }

        public static float xCoordsAt(float y, Vector2f higher, Vector2f lower) {
            Vector2f segDiff = lower.subtract(higher);
            if(Math.abs(segDiff.y) < 0.0001f) {
                return higher.x;
            }

            float xChange = segDiff.x / -segDiff.y;
            float yDiff = higher.y - y;
            float x = higher.x + yDiff*xChange;
            return x;
        }

        @Override
        public void print(int level) {
            super.print(level);
            DebugVisual.get("Seidel").addLine(new Vector3f(higher.x, higher.y, 0), new Vector3f(lower.x, lower.y, 0));
        }

        @Override
        public String toString() {
            return "EdgeSplitNode[" + higher + " -> " + lower + "]";
        }
    }

    class RegionNode implements Node {
        private final int regionNr;
        private SplitNode parent = null;

        private final Trapezoid trapezoid;


        public RegionNode(SplitNode parent, Trapezoid trapezoid) {
            regionNr = nextRegionNr++;
            this.parent = parent;
            this.trapezoid = trapezoid;
        }

        @Override
        public Node addVertex(Vector2f v) {
            if(trapezoid.vertexExists(v))
                return this;

            Vector3f trans = DebugVisual.get("Seidel").transform(new Vector3f(v.x, v.y, 0));
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Add Vertex " + trans + " to " + this);

            VertexSplitNode split = new VertexSplitNode(v);
            Trapezoid newTrapez = trapezoid.splitByVertex(v);

            split.leftLow = new RegionNode(split, newTrapez); // below
            split.rightHigh = this; // above

            if(parent == null)
                root = split;
            else
                parent.changeChild(this, split);

            parent = split;
            return split.leftLow; // Return new region
        }

        @Override
        public void addEdge(Vector2f higher, Vector2f lower) {
            Vector3f highTrans = DebugVisual.get("Seidel").transform(new Vector3f(higher.x, higher.y, 0));
            Vector3f lowTrans  = DebugVisual.get("Seidel").transform(new Vector3f(lower.x, lower.y, 0));
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Add Edge " + highTrans + " -> " + lowTrans + " to " + this);

            EdgeSplitNode split = new EdgeSplitNode(higher, lower);
            Trapezoid newTrapez = trapezoid.splitByEdge(split);

            split.leftLow = this;
            split.rightHigh = new RegionNode(split, newTrapez);

            if(trapezoid.mergeUp()) {
                System.out.println("........................................................................ merged 1");
            }
            if(newTrapez.mergeUp()) {
                System.out.println("........................................................................ merged 2");
            }

            if(parent == null)
                root = split;
            else
                parent.changeChild(this, split);

            parent = split;
        }

        @Override
        public void print(int level) {
            TrapezoidTree.print(level, "RegionNode[" + regionNr + "]");
            trapezoid.addDebugVis(regionNr);
        }

        @Override
        public String toString() {
            return "RegionNode[" + regionNr + "]";
        }
    }



    private Node root;
    private int nextRegionNr = 1;

    public TrapezoidTree() {
        Trapezoid trapezoid = new Trapezoid();
        trapezoid.makeInfinite();

        this.root = new RegionNode(null, trapezoid);
    }

    public void addEdge(Vector2f v1, Vector2f v2) {
        System.out.println("TrapezoidTree.addEdge() ---------------------------------------------------------------------------");

        if(v1.y == v2.y) {
            System.out.println("Ignoring edge: Equal Y values");
            return;
        }

        if(v1.y > v2.y)
            addEdgeInternal(v1, v2);
        else if(v1.y < v2.y)
            addEdgeInternal(v2, v1);
        else {
            if(v1.x > v2.x)
                addEdgeInternal(v1, v2);
            else
                addEdgeInternal(v2, v1);
        }
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
