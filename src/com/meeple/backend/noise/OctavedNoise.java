package com.meeple.backend.noise;

import static org.lwjgl.nuklear.Nuklear.nk_button_text;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

import java.text.DecimalFormat;
import java.util.Properties;

import com.meeple.shared.frame.nuklear.NkContextSingleton;

public class OctavedNoise extends Noise<OctavedNoise> {

	private float[] frequencys;
	private float[] amplitudes;

	private float sampleRate;
	private int largestFeature;
	private float persistence;
	private int numberOfOctaves;
	private int numberOfOctaves2;

	private int mode1 = 0;
	int mode2 = 0;
	private float z1Calc = 0;
	private float z2Calc = 0;

	public OctavedNoise(

		//octaved

		float sampleRate,
		int largest,
		float persistence) {
		this.sampleRate = sampleRate;
		this.persistence = persistence;
		this.largestFeature = largest;

		gen();

	}

	public OctavedNoise() {
		this(6f, 500, 0.5f);
	}

	private void gen() {
		//recieves a number (eg 128) and calculates what power of 2 it is (eg 2^7)
		numberOfOctaves = (int) Math.ceil(Math.log10(largestFeature) / Math.log10(2));

		numberOfOctaves2 = (int) Math.ceil(Math.log10(largestFeature) / Math.log10(2));

		frequencys = new float[numberOfOctaves + numberOfOctaves2];
		amplitudes = new float[numberOfOctaves + numberOfOctaves2];

		for (int i = 0; i < numberOfOctaves; i++) {

			frequencys[i] = sampleRate * (float) Math.pow(2, i);
			amplitudes[i] = (float) Math.pow(persistence, numberOfOctaves - i);

		}

		for (int i = 0; i < numberOfOctaves2; i++) {

			frequencys[numberOfOctaves + i] = sampleRate * (float) Math.pow(2, i);
			amplitudes[numberOfOctaves + i] = (float) Math.pow(persistence, numberOfOctaves2 - i);

		}

		z1Calc = nextFloat() * 10f;
		z2Calc = nextFloat() * 10f;
		mode1 = nextInt(4);
		mode2 = nextInt(4);

	}

	@Override
	protected float rawValue(float x, float y) {
		float result = 0;

		for (int i = 0; i < numberOfOctaves; i++) {
			switch (mode1) {
				case (0): {
					result = result + org.joml.SimplexNoise.noise(z1Calc, x / frequencys[i], y / frequencys[i]) * amplitudes[i];
					break;
				}
				case (1): {
					result = result + org.joml.SimplexNoise.noise(x / frequencys[i], z1Calc, y / frequencys[i]) * amplitudes[i];
					break;
				}
				case (2): {
					result = result + org.joml.SimplexNoise.noise(x / frequencys[i], y / frequencys[i], z1Calc) * amplitudes[i];
					break;
				}
				case (3): {
					result = result + org.joml.SimplexNoise.noise(x / frequencys[i], y / frequencys[i]) * amplitudes[i];
					break;
				}
			}
		}

		for (int i = 0; i < numberOfOctaves2; i++) {
			switch (mode2) {
				case (0): {
					result = result + org.joml.SimplexNoise.noise(z2Calc, x / frequencys[numberOfOctaves + i], y / frequencys[numberOfOctaves + i]) * amplitudes[numberOfOctaves + i];
					break;
				}
				case (1): {
					result = result + org.joml.SimplexNoise.noise(x / frequencys[numberOfOctaves + i], z2Calc, y / frequencys[numberOfOctaves + i]) * amplitudes[numberOfOctaves + i];
					break;
				}
				case (2): {
					result = result + org.joml.SimplexNoise.noise(10f, x / frequencys[numberOfOctaves + i], y / frequencys[numberOfOctaves + i], z2Calc) * amplitudes[numberOfOctaves + i];
					break;
				}
				case (3): {
					result = result + org.joml.SimplexNoise.noise(x / frequencys[numberOfOctaves + i], y / frequencys[numberOfOctaves + i]) * amplitudes[numberOfOctaves + i];
					break;
				}
			}
		}

		return result;
	}

	@Override
	public boolean drawMenu(NkContextSingleton nkc, DecimalFormat format) {

		boolean update = false;
		update |= super.drawMenu(nkc, format);
		nk_layout_row_dynamic(nkc.context, 25, 1);
		if (nk_button_text(nkc.context, "regen")) {
			gen();
			update |= true;
		}
		return update;
	}

	@Override
	protected void store(String prefix, Properties properties) {
		super.store(prefix, properties);

	}
}
