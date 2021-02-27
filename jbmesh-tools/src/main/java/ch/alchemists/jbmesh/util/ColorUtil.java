package ch.alchemists.jbmesh.util;

import com.jme3.math.ColorRGBA;

public class ColorUtil {
    public static ColorRGBA hsb(float h, float s, float b) {
        ColorRGBA color = new ColorRGBA();
        color.a = 1.0f;

        if (s == 0) {
            // achromatic ( grey )
            color.r = b;
            color.g = b;
            color.b = b;
            return color;
        }

        //float hh = h / 60.0f;
        float hh = h * 6f;
        int i = (int) hh;
        float f = hh - i;
        float p = b * (1 - s);
        float q = b * (1 - s * f);
        float t = b * (1 - s * (1 - f));

        switch(i) {
            case 0:
                color.r = b;
                color.g = t;
                color.b = p;
                break;
            case 1:
                color.r = q;
                color.g = b;
                color.b = p;
                break;
            case 2:
                color.r = p;
                color.g = b;
                color.b = t;
                break;
            case 3:
                color.r = p;
                color.g = q;
                color.b = b;
                break;
            case 4:
                color.r = t;
                color.g = p;
                color.b = b;
                break;
            default:
                color.r = b;
                color.g = p;
                color.b = q;
                break;
        }

        return color;
    }
}
