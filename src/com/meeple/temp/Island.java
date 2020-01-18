package com.meeple.temp;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.joml.SimplexNoise;
import org.joml.Vector2f;

import com.meeple.shared.MathHelper;
import com.meeple.shared.MathHelper.RandomCollection;

public class Island {

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

	private float[][] circleHeightMap() {
		float[][] ret = new float[(int) size.diameter10][(int) size.diameter10];
		for (int x = 0; x < size.diameter10; x++) {
			for (int y = 0; y < size.diameter10; y++) {

				float dist = new Vector2f(x, y).distance(size.radius10, size.radius10);

				//finds the distance between xy and mid. divides by max dist. mults by -1 then adds 1
				float mths = (float) ((((dist + (size.radius / 2)) / size.pythag) * -1) + 1);

				if (dist < size.radius10) {
					ret[x][y] = (float) (mths * 1.5);
				} else {
					ret[x][y] /= 2;
				}
			}
		}
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

	public Island generate(int lower, int higher) {

		float[][] noise =
			generateSimplexNoise(
				(float) (size.radius / MathHelper.getRandomDoubleBetween(random, lower, higher)),
				(int) (size.diameter10),
				(int) (size.diameter10),
				(int) (random.nextInt((int) size.radius10) * size.radius10),
				(int) (random.nextInt((int) size.radius10) * size.radius10));

		float[][] circle = circleHeightMap();

		for (int x = 0; x < size.diameter10; x++) {
			for (int y = 0; y < size.diameter10; y++) {

				float value1 = noise[x][y] / 1.5f;
				float value2 = noise[noise.length - x - 1][noise[0].length - y - 1] / 3;
				float value3 = noise[noise.length - y - 1][noise[0].length - x - 1] / 3;
				float value4 = noise[y][x] / 1.5f;
				//get the value and average from neighbours 
				float tot = value1 + (value2 + value3 + value4) / 3;
				//mult with circle cutoff and maximum world hieght
				float mult = tot * circle[x][y] * WorldHeights.mountainLevel.getMaximum();
				map.put(new Vector2f(x, y), mult);

			}
		}

		return this;
	}

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

	public int convertToMesh(FloatBuffer pointPositions, FloatBuffer pointColours, float scale, float zScale) {
		int actualCount = 0;
		for (Entry<Vector2f, Float> entry : map.entrySet()) {

			Vector2f key = entry.getKey();
			float value = entry.getValue();
			Color c = (WorldHeights.heightColor.get(WorldHeights.getRange(value)));
			if (c != WorldHeights.heightColor.get(WorldHeights.getRange(0))) {
				//TODO add point to mesh
				float x = key.x / size.diameter10;
				float y = key.y / size.diameter10;
				float z = 10f / value;
				float x1 = x - 0.5f;
				float y1 = y - 0.5f;
				float z1 = 1 - z;

				float x2 = x1 * scale;
				float y2 = y1 * scale;
				float z2 = z1 * zScale;

				pointPositions.put(x2);
				pointPositions.put(y2);
				pointPositions.put(z2);

				float r = c.getRed() / 255f;
				float g = c.getGreen() / 255f;
				float b = c.getBlue() / 255f;
				float a = c.getAlpha() / 255f;
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
