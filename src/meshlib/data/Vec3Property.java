package meshlib.data;

import com.jme3.math.Vector3f;
import meshlib.structure.Element;

public class Vec3Property extends FloatProperty {
    public Vec3Property(String name) {
        super(name, 3);
    }

    
    public void getVec3(Element element, Vector3f store) {
        int eleIndex = element.getIndex() * numComponents;
        store.x = data[eleIndex];
        store.y = data[eleIndex+1];
        store.z = data[eleIndex+2];
    }


    public void setVec3(Element element, Vector3f vec) {
        setVec3(element, vec.x, vec.y, vec.z);
    }

    
    public void setVec3(Element element, float x, float y, float z) {
        int eleIndex = element.getIndex() * numComponents;
        data[eleIndex]   = x;
        data[eleIndex+1] = y;
        data[eleIndex+2] = z;
    }
}
