// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.examples;

import ch.alchemists.jbmesh.conversion.BMeshJmeExport;
import ch.alchemists.jbmesh.data.BMeshAttribute;
import ch.alchemists.jbmesh.data.property.Vec3Attribute;
import ch.alchemists.jbmesh.operator.FaceOps;
import ch.alchemists.jbmesh.operator.normalgen.NormalGenerator;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Face;
import ch.alchemists.jbmesh.structure.Loop;
import ch.alchemists.jbmesh.structure.Vertex;
import ch.alchemists.jbmesh.util.DebugNormals;
import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;

public class Normals extends SimpleApplication {
    private BMesh createMesh() {
        BMesh bmesh = new BMesh();

        // Create front quad, counterclockwise vertex order
        Vertex v1 = bmesh.createVertex(0, 0, 0);
        Vertex v2 = bmesh.createVertex(1, 0, 0);
        Vertex v3 = bmesh.createVertex(1, 1, 0);
        Vertex v4 = bmesh.createVertex(0, 1, 0);
        bmesh.createFace(v1, v2, v3, v4);

        // Create left side, consisting of 2 quads
        Vertex left1 = bmesh.createVertex(-0.33f, 0, -0.15f);
        Vertex left2 = bmesh.createVertex(-0.33f, 1, -0.15f);
        bmesh.createFace(left1, v1, v4, left2);

        Vertex left3 = bmesh.createVertex(-0.5f, 0, -0.5f);
        Vertex left4 = bmesh.createVertex(-0.5f, 1, -0.5f);
        bmesh.createFace(left3, left1, left2, left4);

        // Create right side, 1 quad
        Vertex right1 = bmesh.createVertex(1.5f, 0, -0.5f);
        Vertex right2 = bmesh.createVertex(1.5f, 1, -0.5f);
        bmesh.createFace(v2, right1, right2, v3);

        return bmesh;
    }


    private void generateNormalsFlat(BMesh bmesh) {
        // FaceOps provides geometric functions that operate on vertex positions.
        // In contrast, BMesh and its elements only provide topological functions that operate on connections.
        FaceOps faceOps = new FaceOps(bmesh);

        // Loops are elements of the BMesh data structure. They act like fragments of a Face and can store
        // vertex attributes that are different for each adjacent face (in this case: normals).
        // During export, Loops for the same Vertex but with differing attributes will lead to duplicated vertices, and therefore flat shading.
        Vec3Attribute<Loop> loopNormals = Vec3Attribute.getOrCreate(BMeshAttribute.Normal, bmesh.loops());

        for(Face face : bmesh.faces()) {
            Vector3f normal = faceOps.normal(face);

            // Flat shading: All Loops of a Face have the same normal
            for(Loop loop : face.loops())
                loopNormals.set(loop, normal);
        }
    }


    private void generateNormalsAuto(BMesh bmesh) {
        // At edges with an angle of less than 40°, it will produce smooth shading with shared vertices (between front and left faces).
        // At sharper edges it will produce flat shading. The Exporter will duplicate vertices where necessary (between front and right face).
        float creaseAngle = 40;
        NormalGenerator normalGenerator = new NormalGenerator(bmesh, creaseAngle);
        normalGenerator.apply();
    }


    @Override
    public void simpleInitApp() {
        BMesh bmesh = createMesh();

        //generateNormalsFlat(bmesh);
        generateNormalsAuto(bmesh);

        // Show Loop normals
        rootNode.attachChild( DebugNormals.loopNormals(assetManager, bmesh, 0.1f) );

        // Convert BMesh to jme Mesh
        Mesh mesh = BMeshJmeExport.exportTriangles(bmesh);

        // Create & attach Geometry
        Geometry geom = new Geometry("Geom", mesh);
        geom.setMaterial(new Material(assetManager, Materials.LIGHTING));
        rootNode.attachChild(geom);

        setupLight();
        cam.setLocation(new Vector3f(0.5f, 0.5f, 3f));
        cam.setFrustumPerspective(45, (float)cam.getWidth()/cam.getHeight(), 0.1f, 100f);
    }


    private void setupLight() {
        ColorRGBA ambientColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootNode.addLight(ambient);

        Vector3f lightDirection = new Vector3f(-0.5f, 0, -1f).normalizeLocal();
        ColorRGBA lightColor = new ColorRGBA(0.2f, 0.6f, 0.2f, 1.0f);
        DirectionalLight directional = new DirectionalLight(lightDirection, lightColor);
        rootNode.addLight(directional);
    }


    public static void main(String[] args) {
        Normals app = new Normals();
        app.start();
    }
}
