# jBMesh
A Java mesh processing library for the programmatic and procedural creation and modification of meshes for jMonkeyEngine.  
It implements the BMesh data structure that holds adjacency information for its elements: Vertex, Edge, Face and Loop.

The BMesh data structure is similar to the Half-Edge structure or DCELs. Loops are much like Half-Edges and can be seen as fragments of a face. The structure adds an explicit elements for Edge, which allows for the modelling of wireframe meshes. More than 2 faces can be adjacent to an edge: BMesh supports non-manifold surfaces.

This documentation is work in progress. Meanwhile, the code examples can act as a starting point for using this library:
https://github.com/FennelFetish/jBMesh/tree/master/jbmesh-tools/src/main/java/ch/alchemists/jbmesh/examples

## Features:
 - Functions for working with the BMesh data structure (add, remove and iterate elements)
 - Setting predefined or custom attributes to all the elements of the BMesh data structure
 - Conversion of a Mesh from jMonkeyEngine to jBMesh (mesh import)
   - Duplicate vertices at the same position are combined into one Vertex element
 - Conversion from jBMesh to jMonkeyEngine (mesh export)
   - Quads and N-gons are triangulated
   - The exporter duplicates vertices where needed (e.g. for flat shading or UV seams)
   - A matching index buffer is created in short or integer format
 - Generating normals with crease angle parameter: Smooth areas or sharp edges are recognized
 - Marching Cubes surface construction with signed distance functions
 - Sweep-line triangulation:  
   [![YouTube Video of Sweep-Line Triangulation](https://img.youtube.com/vi/OAiuNZTQXJo/0.jpg "Video")](http://www.youtube.com/watch?v=OAiuNZTQXJo)
 - 2D Straight Skeleton / Polygon Offsetting (for growing and shrinking faces):  
   [![YouTube Video of 2D Straight Skeleton](https://img.youtube.com/vi/hZc-YBwF4kw/0.jpg "Video")](http://www.youtube.com/watch?v=hZc-YBwF4kw)
 - Catmull-Clark subdivision surface  
   ![Smooth](https://i.imgur.com/aDRQH9d.png)
 - Operators such as ScaleFace, SubdivideFace, Inset, ExtrudeFace
 - Extrusion along a path:  
   ![Extrude Helix](https://i.imgur.com/jTgCJzx.png)
 - Utilities for debug visualizations

## License
Mozilla Public License, Version 2.0  
https://mozilla.org/MPL/2.0/
