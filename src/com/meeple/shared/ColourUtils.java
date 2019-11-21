package com.meeple.shared;

import org.joml.Vector4f;

public class ColourUtils {
	private static float hue2rgb(float p, float q, float t) {

		if (t < 1f / 6f)
			return p + (q - p) * 6f * t;
		if (t < 1f / 2f)
			return q;
		if (t < 2f / 3f)
			return p + (q - p) * (2f / 3f - t) * 6;
		return p;
	}

	public static Vector4f hslToRgb(float h, float s, float l) {
		float r, g, b;

		if (s == 0) {
			// achromatic
			r = 1;
			g = 1;
			b = l;
		} else {
			float q = 0;
			if (l < 0.5f) {
				q = l * (1f + s);
			} else {
				q = l + s - l * s;
			}
			float p = 2f * l - q;
			r = hue2rgb(p, q, h + 1f / 3f);
			g = hue2rgb(p, q, h);
			b = hue2rgb(p, q, h - 1f / 3f);
		}

		return new Vector4f(r, g, b, 1);
	}

}
