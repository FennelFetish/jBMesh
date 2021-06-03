// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.ObjectTupleAttribute;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Edge;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.util.List;

public class LineExport extends Export<Edge> {
    private final ObjectTupleAttribute<Edge, Vertex> attrEdgeVertex;
    private final Indices<Edge> indices;


    public LineExport(BMesh bmesh) {
        super(bmesh, Mesh.Mode.Lines);

        useVertexAttribute(VertexBuffer.Type.Position, BMeshAttribute.Position);

        attrEdgeVertex = ObjectTupleAttribute.getOrCreate(BMeshAttribute.VertexMap, 2, bmesh.edges(), Vertex[].class, Vertex[]::new);
        attrEdgeVertex.setComparable(false);

        indices = new Indices<>(bmesh.edges(), 2);
    }


    public static Mesh apply(BMesh bmesh) {
        LineExport export = new LineExport(bmesh);
        return export.update();
    }


    @Override
    protected void applyIndexBuffer(Mesh mesh) {
        int maxVertexIndex = bmesh.vertices().totalSize()-1;
        indices.prepare(maxVertexIndex);

        indices.updateIndices((Edge edge, int[] indices) -> {
            indices[0] = attrEdgeVertex.getComponent(edge, 0).getIndex();
            indices[1] = attrEdgeVertex.getComponent(edge, 1).getIndex();
        });

        indices.applyIndexBuffer(mesh);
    }


    @Override
    protected void getVertexNeighborhood(Vertex vertex, List<Edge> dest) {
        for(Edge edge : vertex.edges())
            dest.add(edge);
    }


    @Override
    protected void setVertexReference(Vertex contactPoint, Edge element, Vertex ref) {
        if(element.vertex0 == contactPoint)
            attrEdgeVertex.setComponent(element, 0, ref);
        else {
            assert element.vertex1 == contactPoint;
            attrEdgeVertex.setComponent(element, 1, ref);
        }
    }


    @Override
    protected Vertex getVertexReference(Vertex contactPoint, Edge element) {
        if(element.vertex0 == contactPoint)
            return attrEdgeVertex.getComponent(element, 0);
        else {
            assert element.vertex1 == contactPoint;
            return attrEdgeVertex.getComponent(element, 1);
        }
    }
}
