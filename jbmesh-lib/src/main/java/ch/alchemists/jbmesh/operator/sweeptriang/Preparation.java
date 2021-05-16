package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.DebugVisual;
import ch.alchemists.jbmesh.util.PlanarCoordinateSystem;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

class Preparation {
    private static final String INVALID_FACE = "Face needs at least 3 valid vertices.";

    private final Collection<SweepVertex> sweepVertices;

    private PlanarCoordinateSystem coordSys;
    private int nextFaceIndex = 0;


    Preparation(Collection<SweepVertex> sweepVertices) {
        this.sweepVertices = sweepVertices;
    }


    void reset() {
        coordSys = null;
        nextFaceIndex = 0;
    }


    private static <T> PlanarCoordinateSystem createCoordSystem(Iterable<T> face, Function<T, Vector3f> fpos) {
        Iterator<T> it = face.iterator();
        if(!it.hasNext())
            throw new IllegalArgumentException(INVALID_FACE);

        Vector3f first = fpos.apply(it.next());
        Vector3f last  = first.clone();

        // Accumulate general direction of polygon
        Vector3f dirSum = new Vector3f();

        // Calculate face normal using Newell's Method
        Vector3f n = new Vector3f();

        int numVertices = 1;
        while(it.hasNext()) {
            Vector3f p = fpos.apply(it.next());
            n.x += (last.y - p.y) * (last.z + p.z);
            n.y += (last.z - p.z) * (last.x + p.x);
            n.z += (last.x - p.x) * (last.y + p.y);
            last.set(p);

            p.subtractLocal(first);
            dirSum.addLocal(p);

            // Count only vertices that are different from 'first'
            if(p.lengthSquared() > 0.00001f)
                numVertices++;
        }

        if(numVertices < 3)
            throw new IllegalArgumentException(INVALID_FACE);

        // Add last segment from last to first
        n.x += (last.y - first.y) * (last.z + first.z);
        n.y += (last.z - first.z) * (last.x + first.x);
        n.z += (last.x - first.x) * (last.y + first.y);
        n.normalizeLocal();

        // TODO: Dir sum could be very near at 'first'
        PlanarCoordinateSystem coordSys = PlanarCoordinateSystem.withY(first, dirSum, n);
        //PlanarCoordinateSystem coordSys = PlanarCoordinateSystem.withX(Vector3f.UNIT_X, Vector3f.UNIT_Z);

        final PlanarCoordinateSystem ref = coordSys;
        DebugVisual.setPointTransformation("SweepTriangulation", p -> ref.unproject(new Vector2f(p.x, p.y)));

        return coordSys;
    }


    <T> void addFace(Iterable<T> face, Function<T, Vector3f> fpos, Function <T, Vertex> fvert) {
        if(coordSys == null)
            coordSys = createCoordSystem(face, fpos);

        Iterator<T> it = face.iterator();
        if(!it.hasNext())
            throw new IllegalArgumentException(INVALID_FACE);
        T ele = it.next();

        SweepVertex first = new SweepVertex(fvert.apply(ele), 0, nextFaceIndex);
        coordSys.project(fpos.apply(ele), first.p);

        SweepVertex prev = first;
        int numVertices = 1;

        while(it.hasNext()) {
            ele = it.next();

            SweepVertex current = new SweepVertex(fvert.apply(ele), numVertices, nextFaceIndex);
            coordSys.project(fpos.apply(ele), current.p);

            current.prev = prev;
            prev.next = current;
            prev = current;

            numVertices++;
        }

        first.prev = prev;
        prev.next = first;

        if(numVertices < 3)
            throw new IllegalArgumentException(INVALID_FACE);

        prepareVertices(first);
        nextFaceIndex++;
    }


    /**
     * Checks vertices for degeneracy. This will add at least 3 vertices to 'sweepVertices' or none at all.
     * (If any vertex is valid, there must be at least 3 valid vertices)
     * @param first of circular list
     */
    private void prepareVertices(SweepVertex first) {
        SweepVertex v = first;
        int added = 0;

        while(true) {
            if(isValid(v)) {
                //System.out.println("Add vertex " + (v.index+1));
                sweepVertices.add(v);
                added++;
                v = v.next;
            }
            else {
                SweepVertex vRemoved = v;
                v = removeLink(v);

                // When the sentinel ('first') was removed, we have to set a new one
                if(vRemoved == first) {
                    first = v;

                    // Skip break condition below. But if only one vertex remains, it will break below.
                    if(v != v.next)
                        continue;
                }
            }

            // Loop condition: Check if first element is reached again
            if(v == first)
                break;
        }

        if(added < 3) {
            assert added == 0; // TODO: This can fail, see bug9
            throw new IllegalArgumentException(INVALID_FACE);
        }
    }


    private static boolean isValid(SweepVertex v) {
        // Degenerate because at same position
        if(v.p.isSimilar(v.prev.p, 0.0001f)) {
            return false;
        }

        Vector2f vPrev = v.prev.p.subtract(v.p);
        Vector2f vNext = v.next.p.subtract(v.p);
        float det = vPrev.determinant(vNext);

        // Degenerate because neighbor edges are collinear and point to same side
        if(Math.abs(det) < 0.0001f && vPrev.dot(vNext) > 0) {
            return false;
        }

        v.leftTurn = (det <= 0);
        return true;
    }


    private static SweepVertex removeLink(SweepVertex v) {
        v.prev.next = v.next;
        v.next.prev = v.prev;

        // Recalculate reflex of prev
        SweepVertex prev = v.prev;
        Vector2f vPrev = prev.prev.p.subtract(prev.p);
        Vector2f vNext = prev.next.p.subtract(prev.p);
        float det = vPrev.determinant(vNext);
        prev.leftTurn = (det <= 0);

        return v.next;
    }
}
