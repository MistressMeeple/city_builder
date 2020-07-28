package com.meeple.backend.noise;

import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_propertyf;

import java.text.DecimalFormat;
import java.util.Properties;

import com.meeple.shared.frame.nuklear.NkContextSingleton;

public class SimplexNoise extends Noise<SimplexNoise> {
	//(this.size.radius / sampleRate) / size.diameter10, x + this.size.radius10, y + this.size.radius10, sampleOffsetX, sampleOffsetY
	float radius;
	float sampleRate;
	float sampleOffsetX;
	float sampleOffsetY;

	public SimplexNoise(float radius, float sampleRate, float sampleOffsetX, float sampleOffsetY) {

		this.radius = radius;
		this.sampleRate = sampleRate;
		this.sampleOffsetX = sampleOffsetX;
		this.sampleOffsetY = sampleOffsetY;
	}

	@Override
	protected float rawValue(float x, float y) {

		float frequency = (radius / sampleRate) / (radius * 2 * 10);
		float rx = ((x + (radius * 10)) + sampleOffsetX) * frequency;
		float ry = ((y + (radius * 10)) + sampleOffsetY) * frequency;
		float actual = (org.joml.SimplexNoise.noise(rx, ry, frequency, radius) + 1f) / 2f;
		return actual;
	}

	@Override
	public boolean drawMenu(NkContextSingleton nkc, DecimalFormat format) {
		boolean update = false;

		nk_layout_row_dynamic(nkc.context, 25, 1);
		float nSampleRate = nk_propertyf(nkc.context, "sample rate", 0, sampleRate, 100, 100, 5f);
		if (nSampleRate != sampleRate) {
			sampleRate = nSampleRate;
			update |= true;
		}

		nk_layout_row_dynamic(nkc.context, 25, 1);
		float nRadi = nk_propertyf(nkc.context, "Radius", 0, radius, 1000, 100, 5f);
		if (nRadi != radius) {
			radius = nRadi;
			update |= true;

		}
		return update | super.drawMenu(nkc, format);
	}

	@Override
	protected void store(String prefix, Properties properties) {
		super.store(prefix, properties);
	}
}
