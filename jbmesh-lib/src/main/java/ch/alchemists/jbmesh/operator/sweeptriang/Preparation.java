package ch.alchemists.jbmesh.operator.sweeptriang;

import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.PlanarCoordinateSystem;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.Collection;
import java.util.Iterator;

class Preparation {
    interface Extractor<T> {
        Vector3f position(T element, Vector3f store);
        Vertex vertex(T element);
    }

    private static final float MIN_VERTEX_DISTANCE = 0.00001f;
    private static final float MIN_VERTEX_DISTANCE_SQUARED = MIN_VERTEX_DISTANCE * MIN_VERTEX_DISTANCE;

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

    public void setCoordinateSystem(PlanarCoordinateSystem coordSys) {
        this.coordSys = coordSys;
    }


    private static <T> PlanarCoordinateSystem createCoordSystem(Iterable<T> face, Extractor<T> extract) {
        Iterator<T> it = face.iterator();
        if(!it.hasNext())
            throw new IllegalArgumentException(INVALID_FACE);

        Vector3f first = extract.position(it.next(), new Vector3f());
        Vector3f last  = first.clone();
        Vector3f valid = new Vector3f();
        Vector3f p     = new Vector3f();

        // Accumulate general direction of polygon
        Vector3f dirSum = new Vector3f();

        // Calculate face normal using Newell's Method
        Vector3f n = new Vector3f();

        int numVertices = 1;
        while(it.hasNext()) {
            extract.position(it.next(), p);
            addToNormal(n, last, p);
            last.set(p);

            p.subtractLocal(first);
            dirSum.addLocal(p);

            // Count only vertices that are different from 'first'
            if(p.lengthSquared() > MIN_VERTEX_DISTANCE_SQUARED) {
                valid.set(p);
                numVertices++;
            }
        }

        if(numVertices < 3)
            throw new IllegalArgumentException(INVALID_FACE);

        // Add last segment from last to first
        addToNormal(n, last, first);
        n.normalizeLocal();

        // Dir sum may be very near at 'first'. In this case use any valid vertex.
        if(dirSum.distanceSquared(first) <= MIN_VERTEX_DISTANCE_SQUARED)
            dirSum.set(valid).addLocal(first);

        PlanarCoordinateSystem coordSys = PlanarCoordinateSystem.withY(first, dirSum, n);
        //coordSys.rotate(-45 * FastMath.DEG_TO_RAD);
        return coordSys;
    }


    private static void addToNormal(Vector3f n, Vector3f last, Vector3f p) {
        n.x += (last.y - p.y) * (last.z + p.z);
        n.y += (last.z - p.z) * (last.x + p.x);
        n.z += (last.x - p.x) * (last.y + p.y);
    }


    <T> void addFace(Iterable<T> face, Extractor<T> extract) {
        if(coordSys == null)
            coordSys = createCoordSystem(face, extract);

        Iterator<T> it = face.iterator();
        if(!it.hasNext())
            throw new IllegalArgumentException(INVALID_FACE);
        T ele = it.next();

        Vector3f p = new Vector3f();
        SweepVertex first = new SweepVertex(extract.vertex(ele), 0, nextFaceIndex);
        coordSys.project(extract.position(ele, p), first.p);

        SweepVertex prev = first;
        int numVertices = 1;

        while(it.hasNext()) {
            ele = it.next();

            SweepVertex current = new SweepVertex(extract.vertex(ele), numVertices, nextFaceIndex);
            coordSys.project(extract.position(ele, p), current.p);

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
        if(v.p.isSimilar(v.prev.p, MIN_VERTEX_DISTANCE))
            return false;

        Vector2f vPrev = v.prev.p.subtract(v.p);
        Vector2f vNext = v.next.p.subtract(v.p);
        float det = vPrev.determinant(vNext);

        // Degenerate because neighbor edges are collinear and point to same side.
        // The dot() is rarely executed and hence not inlined, which prevents scalar replacement of above Vector2f objects.
        // Therefore do a manual dot().
        if(Math.abs(det) < 0.000001f && dot(vPrev.x, vPrev.y, vNext.x, vNext.y) > 0)
            return false;

        v.leftTurn = (det <= 0);
        return true;
    }

    private static float dot(float x1, float y1, float x2, float y2) {
        return (x1 * x2) + (y1 * y2);
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
