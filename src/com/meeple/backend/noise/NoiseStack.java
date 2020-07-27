package com.meeple.backend.noise;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.joml.Vector3f;

public class NoiseStack {
	protected static final float epsilon = 0.0001f;

	private List<Noise<?>> noises = new ArrayList<>();

	Function<Vector3f, Float> preSampler;

	public void setPresample(Function<Vector3f, Float> sampler) {
		this.preSampler = sampler;
	}

	public void setSeed(Random random) {
		for (Noise<?> row : noises) {
			row.seed = Long.toString(random.nextLong());
		}
	}

	public void setSeed(String seed) {
		for (Noise<?> row : noises) {
			row.seed = seed;
		}
	}

	public NoiseStack addNoise(Noise<?> noise) {
		noises.add(noise);
		return this;
	}

	public float sample(float x, float y) {
		return this.sample(x, y, 1f, 1f);
	}

	public float sample(float x, float y, float xyScale, float zScale) {
		x = x * xyScale;
		y = y * xyScale;
		float[] samples = new float[noises.size()];
		int i = 0;
		for (Noise<?> row : noises) {
			samples[i] = row.sample(x, y);

			i += 1;
		}

		// average the 2 circle samples
		float val1 = ((samples[0] + samples[1]) / 2f);
		// square the circle
		float val2 = samples[0] * samples[0];
		// combine with octaved
		float multVal = val1 * (samples[2] + val2);

		float result = multVal * zScale;
		if (preSampler != null) {
			result = preSampler.apply(new Vector3f(x, y, result));
		}
		return result;

	}

}
