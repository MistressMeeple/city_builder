package com.meeple.temp;

import java.awt.Color;
import java.time.temporal.ValueRange;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.meeple.shared.MathHelper;
import com.meeple.shared.MathHelper.RandomCollection;

public class WorldHeights {
	public static ValueRange deepSeaLevel;//range of deep sea
	public static ValueRange seaLevel;//range of normal sea
	public static ValueRange shoreLevel;//range of the normal land
	public static ValueRange grassLevel;//range of hills
	public static ValueRange mountainLevel;//range of mountains
	public static ValueRange snowCapLevel;//range of mountains
	public static Map<ValueRange, Color> heightColor = new HashMap<ValueRange, Color>();

	static {
		create(100);
		heightColor.put(seaLevel, Color.cyan);
		heightColor.put(shoreLevel, Color.yellow.darker());
		heightColor.put(grassLevel, Color.green.darker());
		heightColor.put(mountainLevel, Color.gray);
		heightColor.put(snowCapLevel, Color.white);
	}

	public static void create(double scale) {
		//-
		//setup levels for heightmap detection
		//-
		/*
		deepSeaLevel = ValueRange.of((long) (-1 * scale), 0);
		seaLevel = ValueRange.of(0, (long) (0.1 * scale));
		shoreLevel = ValueRange.of(seaLevel.getMaximum(), seaLevel.getMaximum() + (long) (0.01 * scale));
		grassLevel = ValueRange.of(shoreLevel.getMaximum(), shoreLevel.getMaximum() + (long) (0.1 * scale));
		mountainLevel = ValueRange.of(grassLevel.getMaximum(), grassLevel.getMaximum() + (long) (0.05 * scale));
		//we dont use max for this so the actual generation isnt affected
		snowCapLevel = ValueRange.of(mountainLevel.getMaximum(), mountainLevel.getMaximum() + (long) (1 * scale));*/

		deepSeaLevel = ValueRange.of((long) (-1 * scale), 0);
		seaLevel = ValueRange.of(0, 10);
		shoreLevel = ValueRange.of(10, 11);
		grassLevel = ValueRange.of(11, 21);
		mountainLevel = ValueRange.of(21, 22);
		//we dont use max for this so the actual generation isnt affected
		snowCapLevel = ValueRange.of(22, 23);

		System.out.println("");
	}

	public static ValueRange getRange(double number) {
		if (seaLevel.isValidValue((int) number)) {
			return seaLevel;
		}
		if (shoreLevel.isValidValue((int) number)) {
			return shoreLevel;
		}
		if (grassLevel.isValidValue((int) number)) {
			return grassLevel;
		}
		if (mountainLevel.isValidValue((int) number)) {
			return mountainLevel;
		}
		//we actually want solid land above this to always be snowcapped, regardless of generate cap
		if (number >= snowCapLevel.getMinimum()) {
			return snowCapLevel;
		}
		return deepSeaLevel;
	}

	public static ValueRange getEntireWorldHeight() {
		return ValueRange.of(deepSeaLevel.getMinimum(), snowCapLevel.getMaximum());
	}

	public static double getRandomWeightedTowards(ValueRange weight, Random random) {
		MathHelper.RandomCollection<ValueRange> a = new RandomCollection<ValueRange>(random);
		a.add(1, deepSeaLevel);
		a.add(1, seaLevel);
		a.add(1, shoreLevel);
		a.add(1, grassLevel);
		a.add(1, mountainLevel);
		a.add(.1f, snowCapLevel);
		a.add(5, weight);
		ValueRange ran = a.next();
		//		double mid = MathHelper.average(ran.getMinimum(), ran.getMaximum());
		double gen = MathHelper.getRandomDoubleBetween(random, ran);

		return gen;

	}
}
