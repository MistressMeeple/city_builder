package com.meeple.backend.noise;

import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_propertyf;

import java.text.DecimalFormat;
import java.util.Properties;

import org.joml.Rectanglef;
import org.joml.Vector2f;

import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.nuklear.NuklearManager;

public class CircleNoise extends Noise<CircleNoise> {
	protected float radius;
	protected Vector2f center;

	public CircleNoise(Vector2f center, float radi) {
		//			super(0.0125f, -(0.125f * 2f), 0.5f, 1.5f);
		setCubic(0.683f, -0.994f, 0.420f, 0.586f);
		this.radius = radi;
		this.center = center;
	}

	@Override
	protected float rawValue(float x, float y) {
		float dist = (new Vector2f(x, y)).distance(center.mul(100, new Vector2f()));
		float mths = (dist * 2) / radius * -1.0F + 1.0F;
		return Math.max(0, mths);
	}

	@Override
	public boolean drawMenu(NkContextSingleton nkc, DecimalFormat format) {
		boolean update = false;

		nk_layout_row_dynamic(nkc.context, 200, 1);
		update |= NuklearManager.nk_graph(nkc.context, center, new Rectanglef(-10, -10, 10, 10));
		nk_layout_row_dynamic(nkc.context, 25, 1);
		float nRadi = nk_propertyf(nkc.context, "Radius", 100, radius, 10000, 100, 1f);
		if (nRadi != radius) {
			radius = nRadi;
			update |= true;

		}
		return update | super.drawMenu(nkc, format);
	}

	@Override
	protected void store(String prefix, Properties properties) {
		super.store(prefix, properties);
		properties.put(prefix + ".radius", radius);
		properties.put(prefix + ".center.x", center.x);
		properties.put(prefix + ".center.y", center.y);
	}
}
