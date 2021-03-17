package ch.alchemists.jbmesh.tools.polygoneditor;

import com.jme3.math.Vector2f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PolygonEditorTool {
    private final String name;
    private final List<ToolProperty<?>> properties = new ArrayList<>(2);


    protected PolygonEditorTool(String name) {
        this.name = name;
    }


    public String getName() {
        return name;
    }


    protected void addProperty(ToolProperty<?> property) {
        properties.add(property);
    }

    public List<ToolProperty<?>> getProperties() {
        return Collections.unmodifiableList(properties);
    }


    public void onActivate() {}
    public void onDeactivate() {}


    public void mouseDown(Vector2f cursor, int button) {}
    public void mouseUp(Vector2f cursor, int button) {}
    public void mouseScroll(int amount) {}


    public final void update(float tpf) {
        updateTool(tpf);

        for(ToolProperty prop : properties)
            prop.update(tpf);
    }

    public void updateTool(float tpf) {}
}
