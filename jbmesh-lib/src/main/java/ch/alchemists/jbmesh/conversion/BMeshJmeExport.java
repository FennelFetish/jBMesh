// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.conversion;

import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.BMeshData;
import ch.alchemists.jbmesh.data.Element;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.util.Map;

public class BMeshJmeExport {
    public static Mesh exportTriangles(BMesh bmesh) {
        TriangleExport export = new TriangleExport(bmesh);
        setupExport(export, bmesh, bmesh.loops());
        return export.update();
    }


    public static Mesh exportLines(BMesh bmesh) {
        LineExport export = new LineExport(bmesh);
        setupExport(export, bmesh, bmesh.edges());
        return export.update();
    }


    @SuppressWarnings("unchecked")
    public static <E extends Element> void setupExport(Export<E> export, BMesh bmesh, BMeshData<E> meshData) {
        for(Map.Entry<String, VertexBuffer.Type> entry : VertexBufferUtils.EXPORT_ATTRIBUTE_MAP.entrySet()) {
            String attrName = entry.getKey();
            VertexBuffer.Type bufferType = entry.getValue();

            // Export already uses Position
            if(bufferType == VertexBuffer.Type.Position)
                continue;

            BMeshAttribute elementAttribute = meshData.getAttribute(attrName);
            BMeshAttribute vertexAttribute  = bmesh.vertices().getAttribute(attrName);

            if(elementAttribute == null) {
                if(vertexAttribute != null)
                    export.useVertexAttribute(bufferType, vertexAttribute);
                continue;
            }

            if(vertexAttribute == null) {
                vertexAttribute = VertexBufferUtils.createBMeshAttribute(bufferType, elementAttribute.numComponents, Vertex.class);
                bmesh.vertices().addAttribute(vertexAttribute);
            }

            export.mapAttribute(bufferType, elementAttribute, vertexAttribute);
        }
    }
}
