package meshlib.data;

import meshlib.structure.BMeshProperty;
import meshlib.structure.Element;

public class FloatProperty extends BMeshProperty<float[]> {
    FloatProperty(String name) {
        super(name, BMeshProperty.Type.Float, 1);
    }

    protected FloatProperty(String name, int numComponents) {
        super(name, BMeshProperty.Type.Float, numComponents);
    }


    public float getFloat(Element element) {
        return data[element.getIndex()];
    }

    public void setFloat(Element element, float f) {
        data[element.getIndex()] = f;
    }


    // Call compact() first.
    public float[] getFloatArray() {
        return data;
    }
}
