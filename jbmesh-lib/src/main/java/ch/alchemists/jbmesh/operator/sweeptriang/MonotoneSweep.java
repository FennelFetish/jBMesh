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
    }


    public SweepVertex getLastVertex() {
        return stack.peek();
    }


    public void processLeft(SweepVertex v) {
        if(stack.size() < 2)
            stack.push(v);
        else if(lastLeft)
            processSameSide(v);
        else
            processOtherSide(v);

        lastLeft = true;
    }


    public void processRight(SweepVertex v) {
        if(stack.size() < 2)
            stack.push(v);
        else if(lastLeft)
            processOtherSide(v);
        else
            processSameSide(v);

        lastLeft = false;
    }


    public void processEnd(SweepVertex v) {
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
        SweepVertex keep = stack.pop();

        Vector2f dir = new Vector2f();
        Vector2f base = keep.p.subtract(v.p);
        float side = (lastLeft) ? 1 : -1;

        while(!stack.isEmpty()) {
            SweepVertex o = stack.peek();
            dir.set(o.p).subtractLocal(v.p);

            // Ensure that we can see 'o' from 'v'
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
}
