// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.structure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class VertexTest {
    @Test
    public void testCtor() {
        Vertex v = new Vertex();
        assertNull(v.edge);
    }

    @Test
    public void testDiskCycleAdd() {
        Vertex center = new Vertex();
        Vertex v1 = new Vertex();
        Vertex v2 = new Vertex();
        Vertex v3 = new Vertex();

        Edge e1 = new Edge();

        assertThrows(IllegalArgumentException.class, () -> {
            center.addEdge(e1);
        }, "Edge is not adjacent to Vertex");

        e1.vertex1 = v1;

        assertThrows(IllegalArgumentException.class, () -> {
            center.addEdge(e1);
        }, "Edge is not adjacent to Vertex");

        e1.vertex0 = center;
        center.addEdge(e1);
        assertEquals(e1, center.edge);
        assertDiskCycleNext(center, e1);
        assertDiskCyclePrev(center, e1);

        Edge e2 = new Edge();
        e2.vertex0 = center;
        e2.vertex1 = v2;
        center.addEdge(e2);
        assertDiskCycleNext(center, e1, e2, e1);
        assertDiskCyclePrev(center, e1, e2, e1);

        Edge e3 = new Edge();
        e3.vertex0 = center;
        e3.vertex1 = v3;
        center.addEdge(e3);
        assertDiskCycleNext(center, e1, e2, e3, e1);
        assertDiskCyclePrev(center, e1, e3, e2, e1);

        center.removeEdge(e2);
        assertEquals(e2, e2.getNextEdge(center));
        assertEquals(e2, e2.getNextEdge(v2));
        assertDiskCycleNext(center, e1, e3, e1);
        assertDiskCyclePrev(center, e1, e3, e1);

        assertThrows(IllegalArgumentException.class, () -> {
            center.removeEdge(e2);
        }, "Edge does not exists in disk cycle for Vertex");

        center.addEdge(e2);
        assertDiskCycleNext(center, e1, e3, e2, e1);
        assertDiskCyclePrev(center, e1, e2, e3, e1);

        assertThrows(IllegalArgumentException.class, () -> {
            center.addEdge(e2);
        }, "Edge already associated with a disk cycle for this Vertex");

        // Test Vertex.getEdgeTo(Vertex)
        assertEquals(e1, center.getEdgeTo(v1));
        assertEquals(e2, center.getEdgeTo(v2));
        assertEquals(e3, center.getEdgeTo(v3));

        Vertex v4 = new Vertex();
        assertNull(center.getEdgeTo(v4));
    }


    @Test
    public void testDiskCycleRemove() {
        Vertex center = new Vertex();
        Vertex v1 = new Vertex();
        Vertex v2 = new Vertex();
        Vertex v3 = new Vertex();

        Edge e1 = new Edge();
        e1.vertex0 = center;
        e1.vertex1 = v1;
        center.addEdge(e1);

        Edge e2 = new Edge();
        e2.vertex0 = center;
        e2.vertex1 = v2;
        center.addEdge(e2);

        Edge e3 = new Edge();
        e3.vertex0 = center;
        e3.vertex1 = v3;
        center.addEdge(e3);

        // Test removal one-by-one
        center.removeEdge(e1);
        assertDiskCycleNext(center, e2, e3, e2);
        assertDiskCyclePrev(center, e2, e3, e2);

        assertThrows(IllegalArgumentException.class, () -> {
            center.removeEdge(e1);
        }, "Edge does not exists in disk cycle for Vertex");

        center.removeEdge(e3);
        assertDiskCycleNext(center, e2, e2);
        assertDiskCyclePrev(center, e2, e2);

        center.removeEdge(e2);
        assertNull(center.edge);

        // Throw same IllegalArgumentException and not NPE when center.edge == null
        assertThrows(IllegalArgumentException.class, () -> {
            center.removeEdge(e1);
        }, "Edge does not exists in disk cycle for Vertex");
    }


    @Test
    public void testEdgeNull() {
        Vertex v = new Vertex();

        assertThrows(NullPointerException.class, () -> {
            v.addEdge(null);
        });

        assertThrows(NullPointerException.class, () -> {
            v.removeEdge(null);
        });
    }


    private static void assertDiskCycleNext(Vertex vert, Edge... expectedEdges) {
        Edge[] actual = new Edge[expectedEdges.length];

        Edge edge = vert.edge;
        for(int i=0; i<expectedEdges.length; ++i) {
            actual[i] = edge;
            edge = edge.getNextEdge(vert);
        }

        assertArrayEquals(expectedEdges, actual);
    }

    private static void assertDiskCyclePrev(Vertex vert, Edge... expectedEdges) {
        Edge[] actual = new Edge[expectedEdges.length];

        Edge edge = vert.edge;
        for(int i=0; i<expectedEdges.length; ++i) {
            actual[i] = edge;
            edge = edge.getPrevEdge(vert);
        }

        assertArrayEquals(expectedEdges, actual);
    }
}
