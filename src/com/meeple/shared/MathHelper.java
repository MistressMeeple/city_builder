package com.meeple.shared;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class MathHelper {

	public static final float epsilon = 0.0001f;

	/**
	 * Runs pythagorus on given co-ordinates from origin
	 * @param x
	 * @param y
	 * @return pythag 
	 */
	public static double pythag(double x, double y) {
		return Math.sqrt((x * x) + (y * y));
	}

	/**
	 * Runs pythagorus on given co-ordinates
	 * @param x1 - first point x
	 * @param x2 - second point x
	 * @param y1 - first point y
	 * @param y2 - second point y
	 * @return pythag of the difference
	 */
	public static double pythag(double x1, double x2, double y1, double y2) {
		double d1 = Math.max(x1, x2);
		double d2 = Math.min(x1, x2);
		double d3 = d1 - d2;
		double d4 = Math.max(y1, y2);
		double d5 = Math.min(y1, y2);
		double d6 = d4 - d5;
		return pythag(d3, d6);
	}

	/**
	 * Rounds the numer to the given number of decimal places using decimal format
	 * @param d - number to format
	 * @param decimalPlaces - how many decimal places to go to
	 * @return number with given decimal places
	 */
	public static float round(float f, int decimalPlaces) {
		String s = "#.";
		for (int i = 0; i < decimalPlaces; i++) {
			s += "#";
		}
		DecimalFormat df = new DecimalFormat(s);
		df.setRoundingMode(RoundingMode.CEILING);
		return Float.parseFloat(df.format(f));
	}

	/**
	 * Checks if a number is between the limits. lower <=test < upper
	 * @param lower - lower range
	 * @param test - number to test
	 * @param upper - upper range
	 * @return whether or not number is in the range
	 */
	public static boolean isBetween(float lower, float test, float upper) {
		return (test >= lower && test < upper);
	}

	/**
	 * Checks if a number is between the limits. uses ValueRange
	 * @param test - number to test
	 * @param range - ValueRange with the defined range
	 * @return whether or not number is in the range
	 */
	public static boolean isBetween(float test, ValueRange range) {
		return range.isValidValue((long) test);
	}

	public static boolean compareDoubles(float a, float b) {
		return (Math.abs(a - b) < epsilon);
	}

	public static float getRandomDoubleBetween(Random random, float lower, float upper) {
		return (random.nextFloat() * (upper - lower)) + lower;
	}

	public static float getRandomFloatBetween(Random random, float lower, float upper) {
		return (random.nextFloat() * (upper - lower)) + lower;
	}

	public static int getRandomIntBetween(Random random, int lower, int upper) {
		return (random.nextInt((upper - lower) + 1) + lower);
	}

	public static float getRandomDoubleBetween(Random random, ValueRange range) {
		return getRandomDoubleBetween(random, range.getMinimum(), range.getMaximum());
	}

	public static long getRandomLongBetween(Random random, long lower, long upper) {
		return (long) getRandomIntBetween(random, (int) lower, (int) upper);
	}

	/**
	 * Returns a random element from the arraylist
	 * @param random - The random used to find which element
	 * @param list - The list of elements 
	 * @return randomly selected element
	 */
	public static <T> T getRandomElementFromList(Random random, ArrayList<T> list) {
		return list.get(getRandomIntBetween(random, 0, list.size()));
	}

	/**
	 * Finds which side of the line a point is. 
	 * @param pointA - The first point of the line
	 * @param pointB - the second point of the line
	 * @param test - The point to test
	 * @return Which side of the line (-1 0 +1), where 0 is on the line, -1 is below, + 1 is above
	 */
	public static int sideOfLine(Vector2f pointA, Vector2f pointB, Vector2f test) {
		float value = (pointB.x - pointA.x) * (test.y - pointA.y) - (pointB.y - pointA.y) * (test.x - pointA.x);
		if (value < 0) {
			return -1;
		}
		if (value > 0) {
			return 1;
		}
		return 0;
	}

	/**
	 * Returns if check is between lower (inclusive) and upper(non inclusive)
	 * */
	public static boolean inRange(float check, float lower, float upper) {
		if (check >= lower && check < upper) {
			return true;
		}
		return false;
	}

	public static List<Vector2f> rotationSort(List<Vector2f> plist, Vector2f mid) {

		plist.sort(new Comparator<Vector2f>() {
			// sort into a rotational order (0 - 360)
			@Override
			public int compare(Vector2f a, Vector2f b) {
				float angleA = (float) (Math.toDegrees(Math.atan2(a.x - mid.x, a.y - mid.y)) + 180);
				float angleB = (float) (Math.toDegrees(Math.atan2(b.x - mid.x, b.y - mid.y)) + 180);

				if (angleA < angleB) {
					return -1;
				} else if (angleA > angleB) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		return plist;
	}

	/**
	 * Returns the average of all values given
	 * @param args - all the numbers
	 * @return the average number
	 */
	public static float average(float... args) {
		float average = 0;
		for (float d : args) {
			average += d;
		}
		return average / args.length;
	}

	/**
	 * Returns the average of all VPoint2D's given
	 * @param args - all the VPoint2D's
	 * @return the average VPoint2D
	 */
	public static Vector2f average(Vector2f... args) {
		float averageX = 0;
		float averageY = 0;
		for (Vector2f p : args) {
			averageX += p.x;
			averageY += p.y;
		}
		averageX = averageX / args.length;
		averageY = averageY / args.length;

		return new Vector2f(averageX, averageY);
	}

	/**
	 * Returns the average of all VPoint3D's given
	 * @param args - all the VPoint3D's
	 * @return the average VPoint3D
	 */
	public static Vector3f average(Vector3f... args) {
		float averageX = 0;
		float averageY = 0;
		float averageZ = 0;
		for (Vector3f p : args) {
			averageX += p.x;
			averageY += p.y;
			averageZ += p.z;
		}
		averageX = averageX / args.length;
		averageY = averageY / args.length;
		averageZ = averageZ / args.length;

		return new Vector3f(averageX, averageY, averageZ);
	}

	/**
	 * Generate a weighted random
	 * @see <a href="https://stackoverflow.com/a/6409791">StackOverflow: Random weighted selection in Java</a>
	 * */
	public static class RandomCollection<E> extends TreeMap<Float, E> {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5362851153932507525L;

		private Random random;
		private float total = 0;

		public RandomCollection() {
			this(new Random());
		}

		public RandomCollection(Random random) {
			this.random = random;
		}

		public RandomCollection<E> add(float weight, E result) {
			if (weight <= 0)
				return this;
			total += weight;
			this.put(total, result);
			return this;
		}

		public E next() {
			float value = random.nextFloat() * total;
			return this.higherEntry(value).getValue();
		}

		public RandomCollection<E> setRandom(Random random) {
			this.random = random;
			return this;
		}

		public Random getRandom() {
			return random;
		}
	}

}
