package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.util.DebugVisual;
import com.jme3.math.Vector3f;
import java.util.ArrayList;

class IntervalSet {
    private final ArrayList<SweepInterval> intervals = new ArrayList<>(8);


    public IntervalSet() {}


    public boolean isEmpty() {
        return intervals.isEmpty();
    }


    public SweepInterval addInterval(SweepVertex left) {
        SweepInterval interval = new SweepInterval();

        for(int i=0; i<intervals.size(); ++i) {
            SweepInterval current = intervals.get(i);
            current.updateX(left.p.y);

            if(left.p.x <= current.xLeft) {
                intervals.add(i, interval);
                return interval;
            }
        }

        intervals.add(interval);
        return interval;
    }


    public SweepInterval getInterval(SweepVertex v) {
        for(int i=0; i<intervals.size(); ++i) {
            SweepInterval current = intervals.get(i);
            current.updateX(v.p.y);

            if(v.p.x >= current.xLeft && v.p.x <= current.xRight)
                return current;
        }

        return null;
    }


    public SweepInterval getIntervalOther(SweepVertex v) {
        for(int i=0; i<intervals.size(); ++i) {
            SweepInterval current = intervals.get(i);
            if(current.leftEdge.end == v || current.rightEdge.end == v)
                return current;
        }

        return null;
    }


    /*public void removeInterval(SweepVertex v) {
        for(int i=0; i<intervals.size(); ++i) {
            SweepInterval current = intervals.get(i);
            if(current.leftEdge.low == v && current.rightEdge.low == v) {
                intervals.remove(i);
                return;
            }
        }
    }*/

    public SweepInterval getWhereLeftEndpoint(SweepVertex v) {
        for(int i=0; i<intervals.size(); ++i) {
            SweepInterval current = intervals.get(i);
            if(current.leftEdge.end == v)
                return current;
        }

        return null;
    }

    public SweepInterval getWhereRightEndpoint(SweepVertex v) {
        for(int i=0; i<intervals.size(); ++i) {
            SweepInterval current = intervals.get(i);
            if(current.rightEdge.end == v)
                return current;
        }

        return null;
    }

    public void removeInterval(SweepInterval interval) {
        intervals.remove(interval);
    }


    public void debug(float y) {
        DebugVisual dbg = DebugVisual.get("SweepTriangulation");

        for(int i=0; i<intervals.size(); ++i) {
            SweepInterval current = intervals.get(i);
            current.updateX(y);

            Vector3f start = new Vector3f(current.xLeft, y, 0);
            Vector3f end   = new Vector3f(current.xRight, y, 0);

            dbg.addLine(start, end);
            dbg.addText(start, ""+i);
            //System.out.println("added line: " + start + " -> " + end);
        }
    }

    /*private static class IntervalComparator implements Comparator<SweepInterval> {
        private float y = 0;

        public void setY(float y) {
            this.y = y;
        }

        @Override
        public int compare(SweepInterval o1, SweepInterval o2) {
            return 0;
        }
    }*/
}
