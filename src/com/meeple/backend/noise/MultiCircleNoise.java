package com.meeple.backend.noise;

import static org.lwjgl.nuklear.Nuklear.nk_button_text;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.joml.Vector2f;
import org.joml.Vector4f;

import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.temp.IslandOrig.IslandSize;

public class MultiCircleNoise extends Noise<MultiCircleNoise> {

	private List<Vector4f> circles = new CollectionSuppliers.ListSupplier<Vector4f>().get();

	public MultiCircleNoise() {

	}

	public MultiCircleNoise addCircle(int index, Vector4f circle) {
		circles.add(index, circle);
		return this;
	}

	float max = Float.MIN_VALUE;

	@Override
	protected float rawValue(float x, float y) {

		float result = 0;
		synchronized (circles) {

			for (Iterator<Vector4f> iterator = circles.iterator(); iterator.hasNext();) {
				Vector4f n = iterator.next();
				//float nSample = 0;//n.sample(x, y);

				float dist = new Vector2f(x, y).distance(n.x, n.y);
				float mths = (dist * 2) / n.z * -1.0F + 1.0F;
				float nSample = mths - n.w;

				result = Math.max(result, nSample);
				/*float infl = n.center.distance(x, y);
				float inflRT = (float) Math.sqrt(infl);
				if (inflRT > max) {
					max = inflRT;
				}*/

				//TODO better under 0 curving
				//what this does is choose the furthest circle and use that as its curve. this creates bad clipping on all the other circles
				//*
				/*if (result <= 0)
					result = Math.max(0, Math.min(result, nSample) / (100f));
				//*/

			}
		}

		return result;
	}

	@Override
	public float sample(float x, float y) {
		return Math.max(0, super.sample(x, y));
	}

	@Override
	public boolean drawMenu(NkContextSingleton nkc, DecimalFormat format) {

		boolean update = false;
		update |= super.drawMenu(nkc, format);
		nk_layout_row_dynamic(nkc.context, 25, 1);
		if (nk_button_text(nkc.context, "regen")) {

			circles.clear();

			int max = 10 + nextInt(20) + nextInt(20);
			for (int i = 0; i < max; i++) {
				float x = ((nextFloat() * 2) - 1) * (1250 / 2);
				float y = ((nextFloat() * 2) - 1) * (1250 / 2);
				int maxSizes = IslandSize.values().length * 10;
				int sizeIndex = nextInt(maxSizes - 1) / 10;

				IslandSize size = IslandSize.values()[sizeIndex];

				addCircle(
					i,
					new Vector4f(x, y, size.pythag * 2f, nextFloat() * 2f - 1f));

			}
			update |= true;
		}
		return update;
	}

	@Override
	protected void store(String prefix,Properties properties) {
		super.store(prefix, properties);
		
	}
}