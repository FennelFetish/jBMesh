package meshlib.operator;

import meshlib.structure.BMesh;
import meshlib.structure.Edge;
import meshlib.structure.Face;

public class MeshOps {
    public static void invert(BMesh bmesh) {
        for(Face f : bmesh.faces())
            bmesh.invertFace(f);
    }


    public static void mergePlanarFaces(BMesh bmesh) {
        FaceOps faceOps = new FaceOps(bmesh);
        for(Edge e : bmesh.edges().getAll()) {
            Face f1 = e.loop.face;
            Face f2 = e.loop.nextEdgeLoop.face;

            if(f1 != f2 && faceOps.coplanar(f1, f2) && f1.numCommonEdges(f2) == 1)
                bmesh.joinFace(f1, f2, e);
        }
    }
}
