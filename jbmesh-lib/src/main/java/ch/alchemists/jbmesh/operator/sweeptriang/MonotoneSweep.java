package ch.alchemists.jbmesh.operator.sweeptriang;

import com.jme3.math.Vector2f;
import java.util.ArrayDeque;

class MonotoneSweep {
    private final ArrayDeque<SweepVertex> stack = new ArrayDeque<>();
    private boolean lastLeft = false;

    private final SweepTriangulation.TriangleCallback cb;


    public MonotoneSweep(SweepVertex v, SweepTriangulation.TriangleCallback cb) {
        this.cb = cb;
        stack.push(v);
        //System.out.println("Stack push start: " + v.p);
        //printStack();
    }


    public void processLeft(SweepVertex v) {
        //System.out.println("Stack push left: " + v.p);

        if(stack.size() < 2)
            stack.push(v);
        else if(lastLeft)
            processSameSide(v);
        else
            processOtherSide(v);

        lastLeft = true;
        //printStack();
    }


    public void processRight(SweepVertex v) {
        //System.out.println("Stack push right: " + v.p);

        if(stack.size() < 2)
            stack.push(v);
        else if(lastLeft)
            processOtherSide(v);
        else
            processSameSide(v);

        lastLeft = false;
        //printStack();
    }


    public void processEnd(SweepVertex v) {
        //System.out.println("Stack push end: " + v.p);
        assert stack.size() >= 2;

        SweepVertex last = stack.pop();
        while(!stack.isEmpty()) {
            SweepVertex o = stack.pop();

            if(lastLeft)
                cb.handleTriangleIndices(v.index, last.index, o.index);
            else
                cb.handleTriangleIndices(v.index, o.index, last.index);

            last = o;
        }
    }


    private void processSameSide(SweepVertex v) {
        //System.out.println("Process same side");
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

            if(lastLeft)
                cb.handleTriangleIndices(v.index, keep.index, o.index);
            else
                cb.handleTriangleIndices(v.index, o.index, keep.index);

            base.set(dir);
            keep = stack.pop();
        }

        stack.push(keep);
        stack.push(v);
    }


    private void processOtherSide(SweepVertex v) {
        //System.out.println("Process other side");

        SweepVertex keep = stack.pop();
        SweepVertex last = keep;

        while(!stack.isEmpty()) {
            SweepVertex o = stack.pop();

            // v is on other side
            if(lastLeft)
                cb.handleTriangleIndices(v.index, last.index, o.index);
            else
                cb.handleTriangleIndices(v.index, o.index, last.index);

            last = o;
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
}
