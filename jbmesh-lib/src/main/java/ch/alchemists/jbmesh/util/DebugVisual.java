package ch.alchemists.jbmesh.util;

import ch.alchemists.jbmesh.conversion.LineExport;
import ch.alchemists.jbmesh.conversion.TriangleExport;
import ch.alchemists.jbmesh.lookup.ExactHashDeduplication;
import ch.alchemists.jbmesh.structure.BMesh;
import ch.alchemists.jbmesh.structure.Vertex;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DebugVisual {
    public interface PointTransformation {
        Vector3f transform(Vector3f p);
    }


    private class Line {
        public final Vector3f start = new Vector3f();
        public final Vector3f end = new Vector3f();
    }

    private class Text {
        public final Vector3f p = new Vector3f();
        public String text = "";
    }


    private static final ConcurrentHashMap<String, List<DebugVisual>> visuals = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, PointTransformation> transforms = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ColorRGBA> colors = new ConcurrentHashMap<>();

    private final List<Vector3f> points = new ArrayList<>();
    public ColorRGBA pointColor = ColorRGBA.Red.clone();
    public float pointSize = 0.1f;

    private final List<Line> lines = new ArrayList<>();
    public ColorRGBA lineColor = ColorRGBA.Cyan.clone();

    private final List<Text> texts = new ArrayList<>();
    public ColorRGBA textColor = ColorRGBA.Yellow.clone();
    public float textSize = 0.4f;

    private final List<Vector3f[]> faces = new ArrayList<>();
    private final List<ColorRGBA> faceColors = new ArrayList<>();

    private final String name;


    private DebugVisual(String name) {
        this.name = name;
    }


    // Static Functions

    public static DebugVisual get(String name) {
        List<DebugVisual> list = visuals.computeIfAbsent(name, k -> new ArrayList<>());
        synchronized(list) {
            if(list.isEmpty()) {
                DebugVisual vis = new DebugVisual(name);
                list.add(vis);
                return vis;
            }

            return list.get( list.size()-1 );
        }
    }

    public static DebugVisual get(String name, int index) {
        List<DebugVisual> list = visuals.computeIfAbsent(name, k -> new ArrayList<>());
        synchronized(list) {
            if(index < list.size())
                return list.get(index);
            else
                return null;
        }
    }

    public static List<DebugVisual> getAll(String name) {
        return visuals.get(name);
    }

    public static DebugVisual next(String name) {
        List<DebugVisual> list = visuals.computeIfAbsent(name, k -> new ArrayList<>());
        synchronized(list) {
            DebugVisual vis = new DebugVisual(name);
            list.add(vis);
            return vis;
        }
    }

    public static void clear(String name) {
        visuals.remove(name);
    }


    public static void setPointTransformation(String name, PointTransformation transform) {
        transforms.put(name, transform);
    }


    public static ColorRGBA getColor(int index) {
        return colors.computeIfAbsent(index, k -> {
            ColorRGBA color = ColorRGBA.randomColor();
            color.a *= 0.5f;
            return color;
        });
    }


    // Member Functions

    public void addPoint(Vector3f p) {
        PointTransformation transform = transforms.get(name);
        if(transform != null)
            p = transform.transform(p);

        points.add(p.clone());
    }

    public void addLine(Vector3f start, Vector3f end) {
        PointTransformation transform = transforms.get(name);
        if(transform != null) {
            start = transform.transform(start);
            end   = transform.transform(end);
        }

        Line line = new Line();
        line.start.set(start);
        line.end.set(end);
        lines.add(line);
    }

    public void addText(Vector3f p, String text) {
        PointTransformation transform = transforms.get(name);
        if(transform != null)
            p = transform.transform(p);

        Text textObj = new Text();
        textObj.p.set(p);
        textObj.text = text;
        texts.add(textObj);
    }

    public void addFace(Vector3f... vertices) {
        addFace(null, vertices);
    }

    public void addFace(ColorRGBA color, Vector3f... vertices) {
        PointTransformation transform = transforms.get(name);
        if(transform != null) {
            for(int i=0; i<vertices.length; ++i)
                vertices[i] = transform.transform(vertices[i]);
        }

        Vector3f[] v = new Vector3f[vertices.length];
        for(int i=0; i<vertices.length; ++i)
            v[i] = vertices[i].clone();

        faces.add(v);
        faceColors.add(color);
    }


    public Node createNode(AssetManager assetManager) {
        Node node = new Node("DebugVisual " + name);
        if(!points.isEmpty())
            createPoints(assetManager, node);
        if(!lines.isEmpty())
            createLines(assetManager, node);
        if(!texts.isEmpty())
            createTexts(assetManager, node);
        if(!faces.isEmpty())
            createFaces(assetManager, node);
        return node;
    }


    private void createPoints(AssetManager assetManager, Node node) {
        Material mat = new Material(assetManager, Materials.UNSHADED);
        mat.setColor("Color", pointColor);

        Mesh mesh = new Sphere(12, 12, pointSize);

        for(int i=0; i<points.size(); ++i) {
            Geometry geom = new Geometry("Point " + i, mesh);
            geom.setMaterial(mat);
            geom.setLocalTranslation(points.get(i));
            node.attachChild(geom);
        }
    }

    private void createLines(AssetManager assetManager, Node node) {
        BMesh bmesh = new BMesh();
        ExactHashDeduplication dedup = new ExactHashDeduplication(bmesh);

        for(Line line : lines) {
            Vertex v0 = dedup.getOrCreateVertex(line.start);
            Vertex v1 = dedup.getOrCreateVertex(line.end);

            if(v0 != v1)
                bmesh.createEdge(v0, v1);
        }

        Material mat = new Material(assetManager, Materials.UNSHADED);
        mat.setColor("Color", lineColor);

        Geometry geom = new Geometry("Lines", LineExport.apply(bmesh));
        geom.setMaterial(mat);
        node.attachChild(geom);
    }

    private void createTexts(AssetManager assetManager, Node node) {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        for(Text t : texts) {
            BitmapText text = new BitmapText(font);
            text.setText(t.text);
            text.setSize(textSize);
            text.setColor(textColor);
            text.setLocalTranslation(t.p);
            node.attachChild(text);
        }
    }

    private void createFaces(AssetManager assetManager, Node node) {
        for(int f=0; f<faces.size(); ++f) {
            Vector3f[] vertices = faces.get(f);
            ColorRGBA color = faceColors.get(f);
            if(color == null)
                color = getColor(f);

            Material mat = new Material(assetManager, Materials.UNSHADED);
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
            mat.setColor("Color", color);

            BMesh bmesh = new BMesh();
            Vertex[] verts = new Vertex[vertices.length];
            for(int i=0; i<vertices.length; ++i)
                verts[i] = bmesh.createVertex(vertices[i]);
            bmesh.createFace(verts);

            Geometry geom = new Geometry("Face", TriangleExport.apply(bmesh));
            geom.setMaterial(mat);
            geom.setQueueBucket(RenderQueue.Bucket.Transparent);
            node.attachChild(geom);
        }
    }


    public Vector3f transform(Vector3f p) {
        PointTransformation transform = transforms.get(name);
        if(transform != null)
            return transform.transform(p);

        return p;
    }
}
