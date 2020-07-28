package com.meeple.temp;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.DoubleConsumer;
import java.util.stream.DoubleStream;

import javax.imageio.ImageIO;

import org.joml.SimplexNoise;
import org.joml.Vector2f;
import org.joml.Vector4f;

import com.meeple.shared.ColourUtils;
import com.meeple.shared.MathHelper;
import com.meeple.shared.MathHelper.RandomCollection;

public class Island {

	public enum IslandSize {
		TINY(1), SMALL(2), MEDIUM(3), BIG(4), HUGE(5);
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

	final Random random;
	final IslandSize size;
	final String name;
	public Map<Vector2f, Float> map = new HashMap<>();

	static RandomCollection<IslandSize> rc;
	static {
		rc = new RandomCollection<>();
		rc.add(1, IslandSize.TINY).add(3, IslandSize.SMALL).add(5, IslandSize.MEDIUM).add(2, IslandSize.BIG);
	}

	public static void main(String[] args) {
		System.out.println("starting");
		Random r = new Random(1);

		TreeMap<Float, IslandSize> map = new TreeMap<>();
		float count = 0;
		map.put(count += 1, IslandSize.TINY);
		map.put(count += 3, IslandSize.SMALL);
		map.put(count += 5, IslandSize.MEDIUM);
		map.put(count += 2, IslandSize.BIG);
		rc.setRandom(r);
		for (int x = 0; x < 25; x++) {
			System.out.println("starting i");

			IslandSize size = rc.next();

			Island i = new Island(r.nextLong(), "name" + x, size);

			i.generate(6, 10);

			i.print();
		}

	}

	public Island(long seed, String name, IslandSize size) {
		this.random = new Random(seed);
		this.size = size;
		this.name = name;
	}

	public static double cubicInterpolate(double[] points, double x, double scale) {
		x /= scale;

		double inBetweenPoint = x;
		int xInHeightmap = (int) x;
		inBetweenPoint -= xInHeightmap;

		double beforePoint1 = safe(points, xInHeightmap - 1);
		double point1 = safe(points, xInHeightmap);
		double point2 = safe(points, xInHeightmap + 1);
		double afterPoint2 = safe(points, xInHeightmap + 2);

		double p = (afterPoint2 - point2) - (beforePoint1 - point1);
		double q = (beforePoint1 - point1) - p;
		double r = point2 - beforePoint1;
		double s = point1;

		return (p * Math.pow(inBetweenPoint, 3)) + (q * Math.pow(inBetweenPoint, 2)) + (r * inBetweenPoint) + s;
	}

	public static double[] safe(double[][] p, int i) {
		return p[Math.max(0, Math.min(i, p.length - 1))];
	}

	public static double safe(double[] p, int i) {
		return p[Math.max(0, Math.min(i, p.length - 1))];
	}

	private float[][] circleHeightMap2(float a, float b, float c) {
		float[][] ret = new float[(int) size.diameter10][(int) size.diameter10];
		for (int x = 0; x < size.diameter10; x++) {
			for (int y = 0; y < size.diameter10; y++) {

				float dist = new Vector2f(x, y).distance(size.radius10, size.radius10);

				//finds the distance between xy and mid. divides by max dist. mults by -1 then adds 1
				float hRad = size.radius / 2;
				float dh = (dist + hRad);
				float dhp = (dh / size.pythag);
				float dhpi = (dhp * -1);
				float mths = (float) (dhpi + 1);

				//				if (dist < size.radius10) {
				ret[x][y] = Math.max((float) (a * Math.pow(mths, 2)) + (b * mths) + c, 0);
				//				} else {
				//					ret[x][y] /= 2;
				//				}
			}
		}
		return ret;
	}

	private float[][] circleHeightMap() {
		float[][] ret = new float[(int) size.diameter10][(int) size.diameter10];
		for (int x = 0; x < size.diameter10; x++) {
			for (int y = 0; y < size.diameter10; y++) {

				float dist = new Vector2f(x, y).distance(size.radius10, size.radius10);

				//finds the distance between xy and mid. divides by max dist. mults by -1 then adds 1
				float hRad = size.radius / 2;
				float dh = (dist + hRad);
				float dhp = (dh / size.pythag);
				float dhpi = (dhp * -1);
				float mths = (float) (dhpi + 1);

				//				if (dist < size.radius10) {
				float a = -0.125f, b = 2, c = (-1);
				ret[x][y] = Math.max((float) (a * Math.pow(mths, 2)) + (b * mths) + c, 0);
				//				} else {
				//					ret[x][y] /= 2;
				//				}
			}
		}
		return ret;
	}

	private float[][] circleMap(Vector2f center, float radi) {
		float[][] ret = new float[(int) size.diameter10][(int) size.diameter10];

		float maxDist = new Vector2f(center).add(radi, radi).distance(center);
		float entropy = radi / 100f;
		float max = 0;

		for (int x = 0; x < size.diameter10; x++) {
			for (int y = 0; y < size.diameter10; y++) {
				float dist = new Vector2f(x, y).distance(center);
				float dM = maxDist - dist;
				dM = (dM / (radi));
				if (dM < 0) {
					//										dM = dM * 2;
				}
				if (dM > max) {
					max = dM;
				}
				//				System.out.println(dM);
				ret[x][y] = ((float) (Math.pow(size.radius / entropy, dM)));
				//				if (ret[x][y] < 0)
				//					ret[x][y] = 0.65f - dM;
				float a = -0.2f, b = 2.9f, c = 0.1f, d = 0.9f, y1 = dist / maxDist;
				//				ret[x][y] = d * (float) (a - Math.cbrt(b * ((dist / maxDist) - 1)));
				ret[x][y] = (float) ((a * Math.pow(y1 - b, 3)) - (c * Math.pow(y1, 2)) + d);

				a = -0.1f;
				d = -0.4f;
				c=2.5f;

				float f = 2.1f;
				ret[x][y] = (float) ((a * Math.pow(y1 - c, 3)) + (d * y1) + f);

			}
		}
		System.out.println("=" + max);
		return ret;
	}

	private float[][] circleHeightMap(Vector2f center, float radi) {
		float[][] ret = new float[(int) size.diameter10][(int) size.diameter10];

		//dist to mid from corner is j
		//mid has value of j
		//j > rad10
		//

		float maxDist = new Vector2f(0, radi).distance(center);
		int lowestX = 0, lowestY = 0;
		float lowestVal = Float.MAX_VALUE;

		int hX = 0, hY = 0;
		float hVal = Float.MIN_VALUE;
		for (int x = 0; x < radi * 2; x++) {
			for (int y = 0; y < radi * 2; y++) {
				float dist = new Vector2f(x, y).distance(size.radius10, size.radius10);

				/*float dist = new Vector2f(x, y).distance(center);
				float val =
				
					(float) cubicInterpolate(new double[] { 0.9f, 0.75f, 0.5f, 0.4f, 0.1f, -0.1f }, (double) dist / maxDist, 0.15d);
				val = val / 1;
				ret[x][y] = val;*/
				/*
								//finds the distance between xy and mid. divides by max dist. mults by -1 then adds 1
								float hRad = size.radius10;
								float dh = hRad + dist;
								float dhp = dh / size.pythag;
								float invert = dhp * -1;
								float mths = invert + 1;
				
								if (dist < size.radius10) {
				
									ret[x][y] = (float) (mths * 1.5);
								} else {
									ret[x][y] /= 2;// (float) (mths * 1.5);
								}*/
				float mths = (float) ((((dist + (size.radius / 2)) / size.pythag) * -1) + 1);

				if (dist < size.radius10) {
					ret[x][y] = (float) (mths * 1.5);
				} else {
					ret[x][y] /= 2;
				}
				//				float cval = (float) ((float) Math.cos(dist / maxDist) + Math.acos(dist / maxDist) / 2) * 1;

				//				if (MathHelper.isBetween(radDist, dist, maxDist)) {

				//				}
				//				cval = 1f - (dist / maxDist);
				//				ret[x][y] = cval;
				if (ret[x][y] < lowestVal) {
					lowestVal = ret[x][y];
					lowestX = x;
					lowestY = y;
				}
				if (ret[x][y] > hVal) {
					hVal = ret[x][y];
					hX = x;
					hY = y;
				}

			}
		}
		System.out.println("L " + lowestX + " " + lowestY + ": " + lowestVal);
		System.out.println("H " + hX + " " + hY + ": " + hVal);
		return ret;

	}

	public static float[][] generateSimplexNoise(float frequency, int width, int height, float offsetX, float offsetY) {
		float[][] simplexnoise = new float[width][height];
		float f = frequency / (float) width;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				simplexnoise[x][y] = (float) SimplexNoise.noise((x + offsetX) * f, (y + offsetY) * f);
				simplexnoise[x][y] = (simplexnoise[x][y] + 1) / 2; //generate values between 0 and 1
			}
		}

		return simplexnoise;
	}

	private double[] genArray(double mult) {

		DoubleStream stream = random.doubles((int) (size.radius / 2));
		int pad = 4;
		double[] ret = new double[(int) (size.radius / 2) + pad];
		ret[0] = 1;

		stream.sorted().forEachOrdered(new DoubleConsumer() {
			int i = ret.length - pad;

			@Override
			public void accept(double value) {
				ret[i--] = value * mult;
			}
		});
		return ret;

	}

	public Island generate(int lower, int higher) {
		float frequency = (float) (size.radius / MathHelper.getRandomDoubleBetween(random, lower, higher)) / (random.nextFloat() * 2 + 1);
		int offsetX = (int) (random.nextInt((int) size.radius10) * size.radius10);
		int offsetY = (int) (random.nextInt((int) size.radius10) * size.radius10);

		float f = frequency / (float) size.diameter10;

		try {
			Vector2f center = new Vector2f(size.radius10, size.radius10);
			float radi = size.radius10;
			float maxDist = new Vector2f(center).add(radi, radi).distance(center);
			double[] heights = {
				0.9d * (radi / size.radius10), 0.75d * (radi / size.radius10), 0.5d * (radi / size.radius10), 0.4d * (radi / size.radius10), 0.1d * (radi / size.radius10),
				-0.1d * (radi / size.radius10) };

			double[] ret = genArray(/*radi / size.radius10*/1);

			Vector2f center2 = new Vector2f(size.radius10 * random.nextFloat(), size.radius10 * random.nextFloat());
			float radi2 = size.radius10 * (1 + (random.nextFloat()));
			float maxDist2 = new Vector2f(center).add(radi2, radi2).distance(center);

			double[] heights2 = {
				1 * (radi2 / size.radius10), 0.5d * (radi2 / size.radius10), 0.25d * (radi2 / size.radius10), 0.125d * (radi2 / size.radius10), 0 };

			float[][] circle = circleHeightMap();
			float[][] circle3 = circleMap(new Vector2f(size.radius10, size.radius10), size.radius10 * 0.75f);

			//NOTE TODO play with these variables
			//			float a = 1f, b = 0.2f, c = -1f + (0.025f * size.radius);
			float a = 0f, b = 2f + (0.25f * size.ordinal()), c = -1f;
			float[][] circle2 = circleHeightMap2(a, b, c);
			//			Map<Vector2i,Float> noiseMap = new CollectionSuppliers.MapSupplier<Vector2i,Float>().get();
			for (int x = 0; x < size.diameter10; x++) {
				for (int y = 0; y < size.diameter10; y++) {
					float sample1 = (float) SimplexNoise.noise((x + offsetX) * f, (y + offsetY) * f);
					float sample2 = (float) SimplexNoise.noise((x + offsetY) * f, (y + offsetX) * f);

					float dist = new Vector2f(x, y).distance(center);
					float val = (float) cubicInterpolate(heights, (double) dist / maxDist, 0.15d);

					float dist2 = new Vector2f(x, y).distance(center2);
					float val2 = (float) cubicInterpolate(heights2, (double) dist2 / maxDist2, 0.15d);

					float dist3 = dist;
					float val3 = (float) cubicInterpolate(ret, (double) dist3 / maxDist, 0.15d / 5);

					//mult with circle cutoff and maximum world hieght
					float samples = ((sample1 + sample2) / 2);
					float vals = ((val + val2 + val3) / 3);
					float circs = (circle[x][y]) + circle2[x][y];
					float mult =

//						circle3[x][y] //- samples
//circs*
//						samples

											(samples) + (0.25f * (circs))
											* vals
					//											samples

//												* (0.5f*(circs))

					;

					//						((val2
					//													+val
					;
					/*
					if (dist > maxDist) {
						mult *= dist / maxDist;
					}*/

					/** WorldHeights.mountainLevel.getMaximum()*/

					map.put(new Vector2f(x, y), mult);

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return this;
	}

	static DecimalFormat fm = new DecimalFormat("0.00");

	public BufferedImage print() {
		System.out.println("Island: Writing island " + name + " to file");
		//		float radius = size.radius * 10;
		int offset = (int) size.radius;
		BufferedImage image = new BufferedImage((int) (size.diameter10) + (offset * 2), (int) (size.diameter10) + (offset * 2), BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.createGraphics();
		g.setColor(WorldHeights.heightColor.get(WorldHeights.getRange(0)));
		//g.fillRect(0, 0, (int) (radius * 2) + (offset * 2), (int) (radius * 2) + (offset * 2));
		for (Entry<Vector2f, Float> entry : map.entrySet()) {
			Vector2f key = entry.getKey();
			float value = entry.getValue();
			g.setColor(WorldHeights.heightColor.get(WorldHeights.getRange(value)));
			if (g.getColor() != WorldHeights.heightColor.get(WorldHeights.getRange(0))) {
				g.fillRect((int) key.x + offset, (int) key.y + offset, 1, 1);
			}

		}
		try {
			File outputfile = new File("islands/" + this.name + " (" + this.size.name() + ").png");
			if (!outputfile.exists()) {
				outputfile.createNewFile();
			}
			ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return image;
	}

	public int convertToMesh(FloatBuffer pointPositions, FloatBuffer pointColours, float scale, float zScale, boolean shifts, float zShift) {
		int actualCount = 0;
		for (Entry<Vector2f, Float> entry : map.entrySet()) {

			Vector2f key = entry.getKey();
			float value = entry.getValue();
			Vector4f c = ColourUtils.hslToRgb(0.75f - value, 1, 0.5f);
			c = c.add(0.1f, 0.1f, 0.1f, 0.1f);

			{
				float x = key.x / size.diameter10;
				float y = key.y / size.diameter10;
				float z = /*WorldHeights.snowCapLevel.getMaximum() / */value;
				float x1 = x - 0.5f;
				float y1 = y - 0.5f;
				float z1 = z;

				float x2 = x1 * scale;
				float y2 = y1 * scale;
				float z2 = z1 /** zScale*/
				;

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
			}

		}
		return actualCount;
	}

}
