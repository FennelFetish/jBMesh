// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.examples;

import ch.alchemists.jbmesh.conversion.BMeshJmeExport;
import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.operator.ExtrudeFace;
import ch.alchemists.jbmesh.operator.normalgen.NormalGenerator;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.BasicShapes;
import ch.alchemists.jbmesh.util.DebugNormals;
import ch.alchemists.jbmesh.util.PlanarCoordinateSystem;
import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;

public class BasicMeshBuilding3D extends SimpleApplication {
    /**
     * Makes a box by manually creating all 8 vertices.
     */
    private BMesh createBox(float sizeX, float sizeY, float sizeZ) {
        BMesh bmesh = new BMesh();

        // Bottom
        Vertex vBot1 = bmesh.createVertex(0,     0, sizeZ); // Front left
        Vertex vBot2 = bmesh.createVertex(sizeX, 0, sizeZ); // Front right
        Vertex vBot3 = bmesh.createVertex(sizeX, 0, 0);     // Back right
        Vertex vBot4 = bmesh.createVertex(0,     0, 0);     // Back left

        // Top, same order
        Vertex vTop1 = bmesh.createVertex(0,     sizeY, sizeZ);
        Vertex vTop2 = bmesh.createVertex(sizeX, sizeY, sizeZ);
        Vertex vTop3 = bmesh.createVertex(sizeX, sizeY, 0);
        Vertex vTop4 = bmesh.createVertex(0,     sizeY, 0);

        bmesh.createFace(vBot4, vBot3, vBot2, vBot1);   // Bottom
        bmesh.createFace(vTop1, vTop2, vTop3, vTop4);   // Top

        bmesh.createFace(vBot1, vBot2, vTop2, vTop1);   // Side +Z
        bmesh.createFace(vBot2, vBot3, vTop3, vTop2);   // Side +X
        bmesh.createFace(vBot3, vBot4, vTop4, vTop3);   // Side -Z
        bmesh.createFace(vBot4, vBot1, vTop1, vTop4);   // Side -X

        return bmesh;
    }


    /**
     * Alternative method which extrudes the bottom face to create the volume.
     */
    private BMesh createExtrudedCylinder(float radius, float height) {
        BMesh bmesh = new BMesh();

        // Create bottom face on the X/Z plane. It will face upwards (normal = [0, 1, 0]).
        Face bottomFace = BasicShapes.createDisc(bmesh, PlanarCoordinateSystem.XZ(), 24, radius);

        // Extrude bottom face and copy position attribute from the original vertices to the new vertices.
        ExtrudeFace extrudeFace = new ExtrudeFace(bmesh);
        extrudeFace.apply(bottomFace);
        extrudeFace.copyVertexAttributes();

        // Move extruded face upwards to create a cylinder.
        // Bottom face becomes the top face.
        Vec3Attribute<Vertex> positions = Vec3Attribute.get(BMeshAttribute.Position, bmesh.vertices());
        positions.forEachModify(bottomFace.vertices(), p -> {
            p.y += height;
        });

        // Recreate the face at the bottom.
        // Invert it (reverse vertex winding order) because it should face downwards.
        bmesh.invertFace( extrudeFace.recreateOriginalFace() );

        return bmesh;
    }


    @Override
    public void simpleInitApp() {
        //BMesh bmesh = createBox(2f, 1f, 0.5f);
        BMesh bmesh = createExtrudedCylinder(1f, 2f);

        // Generate normals
        NormalGenerator normalGenerator = new NormalGenerator(bmesh, 30);
        normalGenerator.apply();

        // Show Loop normals
        rootNode.attachChild( DebugNormals.loopNormals(assetManager, bmesh, 0.1f) );

        // Convert BMesh to jme Mesh
        Mesh mesh = BMeshJmeExport.exportTriangles(bmesh);

        // Create Material
        Material mat = new Material(assetManager, Materials.LIGHTING);
        //mat.getAdditionalRenderState().setWireframe(true);
        //mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

        // Create & attach Geometry
        Geometry geom = new Geometry("Geom", mesh);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);

        setupLight();
        cam.setLocation(new Vector3f(0, 0, 5));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(5);
    }


    private void setupLight() {
        ColorRGBA ambientColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootNode.addLight(ambient);

        Vector3f lightDirection = new Vector3f(-0.1f, -0.3f, -1f).normalizeLocal();
        ColorRGBA lightColor = new ColorRGBA(0.2f, 0.6f, 0.2f, 1.0f);
        DirectionalLight directional = new DirectionalLight(lightDirection, lightColor);
        rootNode.addLight(directional);
    }


    public static void main(String[] args) {
        BasicMeshBuilding3D app = new BasicMeshBuilding3D();
        app.setShowSettings(false);
        app.start();
    }
}
