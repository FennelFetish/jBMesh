// Copyright (c) 2020-2021 Rolf Müri
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package ch.alchemists.jbmesh.util;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;

public class ColorUtil {
    /**
     * Converts from HSV/HSB color space to RGB.<br>
     *
     * @param h Hue (0.0 - 1.0)
     * @param s Saturation (0.0 - 1.0)
     * @param v Value/Brightness (0.0 - 1.0)
     * @return ColorRGBA with default alpha value
     */
    public static ColorRGBA hsv(float h, float s, float v) {
        return hsv(h, s, v, new ColorRGBA());
    }

    /**
     * Converts from HSV/HSB color space to RGB.<br>
     * The alpha value of <code>store</code> remains unaltered.
     *
     * @param h Hue (0.0 - 1.0)
     * @param s Saturation (0.0 - 1.0)
     * @param v Value/Brightness (0.0 - 1.0)
     * @param store Destination color
     * @return store
     */
    public static ColorRGBA hsv(float h, float s, float v, ColorRGBA store) {
        h %= 1f;
        if(h < 0)
            h += 1f;

        s = FastMath.saturate(s);
        v = FastMath.saturate(v);

        h *= 6f;
        int interval = (int) h;
        float f = h - interval;

        float p = v * (1f - s);
        float q = v * (1f - (s*f));
        float t = v * (1f - (s*(1f-f)));
        float a = store.a;

        switch(interval) {
            case 1:
                return store.set(q, v, p, a);
            case 2:
                return store.set(p, v, t, a);
            case 3:
                return store.set(p, q, v, a);
            case 4:
                return store.set(t, p, v, a);
            case 5:
                return store.set(v, p, q, a);
            // interval 0 & 6
            default:
                return store.set(v, t, p, a);
        }
    }


    public static ColorRGBA getRandomColor(float hue, float hueVariance, float saturation, float value) {
        float rnd = (FastMath.nextRandomFloat() * 2.0f) - 1.0f;
        hue += rnd * (hueVariance*0.5f);
        return hsv(hue, saturation, value);
    }
}
