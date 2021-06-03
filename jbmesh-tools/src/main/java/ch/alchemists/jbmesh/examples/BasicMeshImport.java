// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.examples;

import ch.alchemists.jbmesh.conversion.BMeshJmeExport;
import ch.alchemists.jbmesh.conversion.Import;
import ch.alchemists.jbmesh.structure.BMesh;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Torus;

public class BasicMeshImport extends SimpleApplication {
    @Override
    public void simpleInitApp() {
        // Create a jme Mesh. Alternatively, loaded assets could be used.
        Mesh mesh = new Torus(32, 32, 0.5f, 1.0f);

        // "Import" the jme Mesh into a BMesh data structure.
        BMesh bmesh = Import.convert(mesh);

        // Convert BMesh to jme Mesh again
        Mesh convertedMesh = BMeshJmeExport.exportTriangles(bmesh);

        // Create Material
        Material mat = new Material(assetManager, Materials.UNSHADED);
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Red);

        // Create & attach Geometry
        Geometry geom = new Geometry("Geom", convertedMesh);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);
    }


    public static void main(String[] args) {
        BasicMeshImport app = new BasicMeshImport();
        app.start();
    }
}
