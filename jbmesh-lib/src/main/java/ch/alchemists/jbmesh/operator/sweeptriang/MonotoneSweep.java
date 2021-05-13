package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.util.DebugVisual;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.ArrayDeque;

class MonotoneSweep {
    private final ArrayDeque<SweepVertex> stack = new ArrayDeque<>();
    private boolean lastLeft = false;
    private boolean lastReflex = false;


    public MonotoneSweep(SweepVertex v) {
        stack.push(v);
        System.out.println("Stack push start: " + v.p);
        printStack();
    }


    public void processLeft(SweepVertex v) {
        System.out.println("Stack push left: " + v.p);

        if(stack.size() < 2)
            stack.push(v);
        else if(lastLeft)
            processSameSide(v);
        else
            processOtherSide(v);

        lastLeft = true;

        printStack();
    }


    public void processRight(SweepVertex v) {
        System.out.println("Stack push right: " + v.p);

        if(stack.size() < 2)
            stack.push(v);
        else if(lastLeft)
            processOtherSide(v);
        else
            processSameSide(v);

        lastLeft = false;

        printStack();
    }


    public void processEnd(SweepVertex v) {
        System.out.println("Stack push end: " + v.p);

        SweepVertex last = stack.peek();
        assert last.prev==v || last.next==v || last.monotonePath==v;

        // TODO: Implement, don't connect to n-1 and n-2
        // TODO: Process full stack but don't add diagonals to v.prev / v.next (or monotonePath sources)
        /*while(stack.size() > 2) {
            SweepVertex o = stack.pop();
            drawLine(v, o);
        }*/

        processOtherSide(v);
        //processSameSide(v);

        //stack.push(v);
        printStack();
    }


    private void processSameSide(SweepVertex v) {
        System.out.println("Process same side");
        SweepVertex keep = stack.pop();

        Vector2f dir = new Vector2f();
        Vector2f base = keep.p.subtract(v.p);
        float side = (lastLeft) ? 1 : -1;

        while(!stack.isEmpty()) {
            SweepVertex o = stack.peek();
            dir.set(o.p).subtractLocal(v.p);

            float det = base.determinant(dir);
            if(det * side <= 0)
                break;

            base.set(dir);
            keep = stack.pop();
            drawLine(v, o);
        }

        stack.push(keep);
        stack.push(v);
    }


    private void processOtherSide(SweepVertex v) {
        System.out.println("Process other side");

        SweepVertex keep = stack.peek();

        while(!stack.isEmpty()) {
            SweepVertex o = stack.pop();
            drawLine(v, o);
        }

        stack.push(keep);
        stack.push(v);
    }



    private void printStack() {
        System.out.println("Stack:");
        for(SweepVertex v : stack) {
            System.out.println("  - " + v.p);
        }
    }


    private void drawLine(SweepVertex v1, SweepVertex v2) {
        Vector3f p1 = new Vector3f(v1.p.x, v1.p.y, 0);
        Vector3f p2 = new Vector3f(v2.p.x, v2.p.y, 0);
        DebugVisual.get("SweepTriangles").addLine(p1, p2);
        System.out.println("Triangle line: " + p1 + " -> " + p2);
    }
}
