package ch.alchemists.jbmesh.operator.normalgen;

import ch.alchemists.jbmesh.structure.Loop;
import com.jme3.math.Vector3f;
import java.util.ArrayList;

class NormalAccumulator {
    /**
     * This Pool keeps and reuses existing NormalAccumulator objects.
     * Accumulators can be added to the back. When NormalGenerator has to change its direction, it can also add accumulators to the front.
     */
    static class Pool {
        private final ArrayList<NormalAccumulator> accumulators = new ArrayList<>(6);
        private int numActive = 0;

        public NormalAccumulator pushBack(Loop loop) {
            NormalAccumulator acc;
            if(accumulators.size() > numActive) {
                acc = accumulators.get(numActive);
                acc.normal.zero();
            }
            else {
                acc = new NormalAccumulator();
                accumulators.add(acc);
            }

            acc.firstLoop = loop;
            numActive++;
            return acc;
        }

        public NormalAccumulator pushFront(Loop loop) {
            NormalAccumulator acc = pushBack(loop);

            // Move elements to the right
            for(int i=numActive-1; i>0; --i)
                accumulators.set(i, accumulators.get(i-1));

            accumulators.set(0, acc);
            return acc;
        }

        public NormalAccumulator get(int index) {
            return accumulators.get(index);
        }

        public int size() {
            return numActive;
        }

        public void clear() {
            numActive = 0;
        }
    }


    public final Vector3f normal = new Vector3f();
    public Loop firstLoop;
}
