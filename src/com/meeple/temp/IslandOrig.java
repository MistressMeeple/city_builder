package com.meeple.temp;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joml.SimplexNoise;
import org.joml.Vector2f;
import org.joml.Vector4f;

import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.ColourUtils;
import com.meeple.shared.MathHelper.RandomCollection;
import com.meeple.shared.frame.FrameUtils;

public class IslandOrig {

	public enum IslandSize {
		TINY(0), SMALL(1), MEDIUM(2), BIG(3);
		public float radius, radius10, diameter, diameter10, pythag;

		private IslandSize(int ord) {
			radius = ((ord + 1) * 10);
			diameter = radius * 2;
			radius10 = radius * 10;
			diameter10 = radius10 * 2;
			pythag = (float) Math.sqrt((radius10 * radius10) + (radius10 * radius10));

		}

		@Override
		public String toString() {
			String n = this.name().toLowerCase();
			n = n.substring(0, 1).toUpperCase() + n.substring(1);
			return n + ": " + radius;
		}
	}

	public final IslandSize size;
	final float sampleRate;
	final float sampleOffsetX, sampleOffsetY;
	final float sampleRotation;
	final String name;
	public Map<Vector2f, Float> map = new CollectionSuppliers.MapSupplier<Vector2f, Float>().get();
	private Map<Vector2f, Float[]> noises = new CollectionSuppliers.MapSupplier<Vector2f, Float[]>().get();

	private boolean storeAll = false;
	private Vector2f store = new Vector2f();

	private static final int maxNoises = 4;
	static RandomCollection<IslandSize> rc;
	static {
		rc = new RandomCollection<>();
		rc.add(1, IslandSize.TINY).add(3, IslandSize.SMALL).add(5, IslandSize.MEDIUM).add(2, IslandSize.BIG);
	}

	public IslandOrig(String name, IslandSize size, float sampleRotation, float sampleRate, float sampleOffsetX, float sampleOffsetY) {
		this.size = size;
		this.name = name;
		this.sampleRotation = (float) Math.PI * sampleRotation;
		this.sampleRate = sampleRate;
		this.sampleOffsetX = sampleOffsetX;
		this.sampleOffsetY = sampleOffsetY;
	}

	private static float circleSampleA = 0.0125f, circleSampleB = -(0.125f * 2f), circleSampleC = 0.5f, circleSampleD = 1.5f;

	public static void setCircleSample(float a, float b, float c, float d) {
		circleSampleA = a;
		circleSampleB = b;
		circleSampleC = c;
		circleSampleD = d;
	}

	public float sampleCircleMap(float x, float y, Vector2f center, float radius) {
		float dist = (new Vector2f(x, y)).distance(center);
		float mths = (dist * 2) / radius * -1.0F + 1.0F;
		float sample =
			//0.5f + (0.125f * 
			((circleSampleA * (mths * mths * mths)) + (circleSampleB * (mths * mths)) + (circleSampleC * mths) + circleSampleD)
		//)
		;
		if (storeAll) {
			Float[] noiseArr = this.noises.getOrDefault(store, new Float[maxNoises]);
			noiseArr[0] = sample;
			this.noises.put(store, noiseArr);
		}
		return sample;
	}

	private static float sampleNoise(float frequency, float x, float y, float offsetX, float offsetY) {

		float rx = (x + offsetX) * frequency;
		float ry = (y + offsetY) * frequency;

		float ret = SimplexNoise.noise(rx, ry);
		return (ret + 1f) / 2f;

	}

	public float sampleNoise(float x, float y) {
		float sample = sampleNoise((this.size.radius / sampleRate) / size.diameter10, x + this.size.radius10, y + this.size.radius10, sampleOffsetX, sampleOffsetY);
		if (storeAll) {
			Float[] noiseArr = this.noises.getOrDefault(store, new Float[maxNoises]);
			noiseArr[1] = sample;
			this.noises.put(store, noiseArr);
		}
		return sample;
	}

	public float sample(float x, float y) {
		try {
			float sample = sampleNoise(x, y);

			float a1 = 1.5f;
			float mult = (a1 * sample) * 0.5f ;

			return mult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1f;
	}

	public void generate() {
		map.clear();
		storeAll = true;

		float radi = size.diameter10;
		if (true) {
			for (float x = -(radi); x < (radi); x++) {
				for (float y = -(radi); y < (radi); y++) {
					Vector2f rotated = FrameUtils.rotate(x, y, sampleRotation);
					store = new Vector2f((x / radi), (y / radi));

					float sample = sample(rotated.x, rotated.y);
					//					this.map.put(store, sample);
				}
			}
		} else {
		}

		//		stack.pregen(radi);
		/*
				for (Entry<Vector2f, Float> entry : stack.mult.entrySet()) {
					Vector2f key = entry.getKey();
					System.out.println(key + "\n\t" + entry.getValue() + "\n\t" + map.get(key.mul(radi, new Vector2f())));
				}*/

		storeAll = false;
	}

	public int convertToMesh(FloatBuffer pointPositions, FloatBuffer pointColours, float scale, float zScale) {
		int actualCount = 0;
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		for (Map.Entry<Vector2f, Float> entry : this.map.entrySet()) {
			Vector2f key = (Vector2f) entry.getKey();
			float value = ((Float) entry.getValue()).floatValue();
			if (false) {
				Color c = (Color) WorldHeights.heightColor.get(WorldHeights.getRange(value * WorldHeights.mountainLevel.getMaximum()));
				if (c != null && c != WorldHeights.heightColor.get(WorldHeights.getRange(0.0D))) {

					//NOTE convert to 0 -> 1 coords
					float x = key.x;
					float y = key.y;
					//NOTE idk.. scale it back?
					float z = value;

					//NOTE -0.5 -> +0.5
					float x1 = x;
					float y1 = y;
					//NOTE idk again
					float z1 = z;

					//NOTE mult by scale param
					float x2 = x1 * scale;
					float y2 = y1 * scale;
					float z2 = (z1);

					pointPositions.put(x2);
					pointPositions.put(y2);
					pointPositions.put(z2);

					float r = c.getRed() / 255.0F;
					float g = c.getGreen() / 255.0F;
					float b = c.getBlue() / 255.0F;
					float a = c.getAlpha() / 255.0F;
					pointColours.put(r);
					pointColours.put(g);
					pointColours.put(b);
					pointColours.put(a);
					actualCount++;
				}
			} else {
				float rVal = 1f - value / 2f;
				if (rVal > max) {
					max = rVal;
				}
				if (rVal < min) {
					min = rVal;
				}
				Vector4f c = ColourUtils.hslToRgb(-1f / value + 2f, 1, 0.5f);
				//				c = c.add(0.1f, 0.1f, 0.1f, 0.1f);
				//				if (value > zScale) {
				float x = key.x;
				float y = key.y;
				float z = value;
				float x1 = x;
				float y1 = y;
				float z1 = z;

				float x2 = x1 * scale;
				float y2 = y1 * scale;
				float z2 = z1;

				pointPositions.put(x2);
				pointPositions.put(y2);
				pointPositions.put(z2);

				float r = c.x;
				float g = c.y;
				float b = c.z;
				float a = c.w;
				pointColours.put(r);
				pointColours.put(g);
				pointColours.put(b);
				pointColours.put(a);
				actualCount += 1;
				//				}
			}
			//			}
		}
		System.out.println(min + " " + max);
		return actualCount;
	}

	public int circleMesh(List<Number> pointPositions, List<Number> pointColours, float scale, float zScale) {
		int actualCount = 0;
		for (Entry<Vector2f, Float[]> entry : this.noises.entrySet()) {
			Vector2f key = (Vector2f) entry.getKey();
			Float[] valueArr = entry.getValue();
			float value = valueArr[0];

			Vector4f c = ColourUtils.hslToRgb(-1f / value + 2f, 1, 0.5f);

			float z = value;
			float x1 = key.x;
			float y1 = key.y;

			float x2 = x1 * scale;
			float y2 = y1 * scale;

			pointPositions.add(x2);
			pointPositions.add(y2);
			pointPositions.add(z);

			float r = c.x;
			float g = c.y;
			float b = c.z;
			float a = c.w;
			pointColours.add(r);
			pointColours.add(g);
			pointColours.add(b);
			pointColours.add(a);
			actualCount += 1;
		}

		return actualCount;
	}

	public int noiseMesh(List<Number> pointPositions, List<Number> pointColours, float scale, float zScale) {
		int actualCount = 0;
		for (Entry<Vector2f, Float[]> entry : this.noises.entrySet()) {
			Vector2f key = (Vector2f) entry.getKey();
			Float[] valueArr = entry.getValue();
			float value = valueArr[1];

			Vector4f c = ColourUtils.hslToRgb(-1f / value + 2f, 1, 0.5f);

			float z = value;
			float x1 = key.x;
			float y1 = key.y;

			float x2 = x1 * scale;
			float y2 = y1 * scale;

			pointPositions.add(x2);
			pointPositions.add(y2);
			pointPositions.add(z);

			float r = c.x;
			float g = c.y;
			float b = c.z;
			float a = c.w;
			pointColours.add(r);
			pointColours.add(g);
			pointColours.add(b);
			pointColours.add(a);
			actualCount += 1;
		}

		return actualCount;
	}

}
