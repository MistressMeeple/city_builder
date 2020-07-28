package com.meeple.backend.noise;

import static org.lwjgl.nuklear.Nuklear.NK_BUTTON_MIDDLE;
import static org.lwjgl.nuklear.Nuklear.NK_EDIT_FIELD;
import static org.lwjgl.nuklear.Nuklear.NK_TEXT_ALIGN_CENTERED;
import static org.lwjgl.nuklear.Nuklear.NK_TEXT_ALIGN_LEFT;
import static org.lwjgl.nuklear.Nuklear.NK_TEXT_ALIGN_RIGHT;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_NO_SCROLLBAR;
import static org.lwjgl.nuklear.Nuklear.nk_group_begin;
import static org.lwjgl.nuklear.Nuklear.nk_group_end;
import static org.lwjgl.nuklear.Nuklear.nk_label;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_template_begin;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_template_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_template_push_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_template_push_static;
import static org.lwjgl.nuklear.Nuklear.nk_propertyf;
import static org.lwjgl.nuklear.Nuklear.nk_slide_float;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.Random;

import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.nuklear.NuklearManager;

/**
 * 
 * @author Megan
 *
 * @param <T> Self reference to the extending class. <br>for example <code>CircleNoise extends Noise<CircleNoise></code>
 */
public abstract class Noise<T extends Noise<T>> {

	protected float preScale = 1f, postScale = 1f;
	protected float cubeRate = 0f, squareRate = 0f, flatRate = 1f, additionalRate = 0f;

	protected String seed = "";
	private Random random = new Random(0);

	@SuppressWarnings("unchecked")
	public T setCubic(float cubeRate, float squareRate, float flateRate, float additionalRate) {
		this.cubeRate = cubeRate;
		this.squareRate = squareRate;
		this.flatRate = flateRate;
		this.additionalRate = additionalRate;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T setScale(float preScale, float postScale) {
		this.preScale = preScale;
		this.postScale = postScale;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T setSeed(String seed) {
		this.seed = seed;
		this.random = new Random(seedValue());
		return (T) this;
	}

	protected float nextFloat() {
		return random.nextFloat();
	}

	protected int nextInt() {
		return random.nextInt();
	}

	protected int nextInt(int bound) {
		return random.nextInt(bound);
	}

	private long seedValue() {
		long value = 0;
		try {
			value = Long.parseLong(seed.trim());
		} catch (NumberFormatException e) {
			e.printStackTrace();
			value = seed.hashCode();
		}

		return value;
	}

	protected abstract float rawValue(float x, float y);

	public float sample(float x, float y) {
		float nx = x * preScale;
		float ny = y * preScale;
		float raw = rawValue(nx, ny);
		float actual = plot(raw) * postScale;
		return actual;

	}

	public float plot(float value) {
		float raw = value;
		float a = 0;
		if (Math.abs(cubeRate) > NoiseStack.epsilon) {
			a = (cubeRate * (raw * raw * raw));
		}
		float b = 0;
		if (Math.abs(squareRate) > NoiseStack.epsilon) {
			b = squareRate * (raw * raw);
		}
		float c = 0;
		if (Math.abs(flatRate) > NoiseStack.epsilon) {
			c = flatRate * raw;
		}
		float actual = (a + b + c + additionalRate);
		return actual;
	}

	public boolean drawMenu(NkContextSingleton nkc, DecimalFormat format) {
		boolean update = false;
		nk_layout_row_dynamic(nkc.context, 25, 1);

		float nPre = nk_propertyf(nkc.context, "Pre scale", -1f, preScale, 1f, 0.01f, 0.01f);

		if (preScale != nPre) {
			preScale = nPre;
			update |= true;
		}
		float nPost = nk_propertyf(nkc.context, "Post scale", -1f, postScale, 1f, 0.01f, 0.01f);

		if (postScale != nPost) {
			postScale = nPost;
			update |= true;
		}
		nk_layout_row_dynamic(nkc.context, 25 * 6, 1);
		if (nk_group_begin(nkc.context, "main vars", NK_WINDOW_NO_SCROLLBAR)) {
			nk_layout_row_dynamic(nkc.context, 25, 1);
			nk_label(nkc.context, "f(x) = ax³+bx²+cx+d", NK_TEXT_ALIGN_CENTERED);
			nk_layout_row_template_begin(nkc.context, 25);
			nk_layout_row_template_push_static(nkc.context, 10);
			nk_layout_row_template_push_dynamic(nkc.context);
			nk_layout_row_template_push_static(nkc.context, 75);
			nk_layout_row_template_end(nkc.context);
			//				nk_layout_row_begin(nkc.ctx, NK_DYNAMIC, 25, 3);
			float step = 0.001f;
			float min = -2f;
			float max = -min;
			{

				nk_label(nkc.context, "A", NK_TEXT_ALIGN_LEFT);
				boolean reset = false;
				if (Nuklear.nk_widget_has_mouse_click_down(nkc.context, NK_BUTTON_MIDDLE, true)) {
					reset = true;
				}
				float oldA = nk_slide_float(nkc.context, min, cubeRate, max, step);
				if (oldA != cubeRate | reset) {
					if (reset) {
						oldA = 0;
					}
					cubeRate = oldA;
					update = true;
				}
				nk_label(nkc.context, format.format(cubeRate) + "", NK_TEXT_ALIGN_RIGHT);
			}

			{

				nk_label(nkc.context, "B", NK_TEXT_ALIGN_LEFT);

				boolean reset = false;
				if (Nuklear.nk_widget_has_mouse_click_down(nkc.context, NK_BUTTON_MIDDLE, true)) {
					reset = true;
				}
				float oldB = nk_slide_float(nkc.context, min, squareRate, max, step);
				if (oldB != squareRate | reset) {
					if (reset) {
						oldB = 0;
					}
					squareRate = oldB;
					update = true;
				}
				nk_label(nkc.context, format.format(squareRate) + "", NK_TEXT_ALIGN_RIGHT);
			}
			{

				nk_label(nkc.context, "C", NK_TEXT_ALIGN_LEFT);
				boolean reset = false;
				if (Nuklear.nk_widget_has_mouse_click_down(nkc.context, NK_BUTTON_MIDDLE, true)) {
					reset = true;
				}
				float old = nk_slide_float(nkc.context, min, flatRate, max, step);
				if (old != flatRate | reset) {
					if (reset) {
						old = 0;
					}
					flatRate = old;
					update = true;
				}
				nk_label(nkc.context, format.format(flatRate) + "", NK_TEXT_ALIGN_RIGHT);
			}
			{
				nk_label(nkc.context, "D", NK_TEXT_ALIGN_LEFT);

				boolean reset = false;
				if (Nuklear.nk_widget_has_mouse_click_down(nkc.context, NK_BUTTON_MIDDLE, true)) {
					reset = true;
				}
				float old = nk_slide_float(nkc.context, min, additionalRate, max, step);
				if (old != additionalRate | reset) {
					if (reset)
						old = 0;
					additionalRate = old;
					update = true;
				}
				nk_label(nkc.context, format.format(additionalRate) + "", NK_TEXT_ALIGN_RIGHT);
			}
			nk_group_end(nkc.context);

		}
		try (MemoryStack stack = MemoryStack.stackPush()) {

			nk_layout_row_template_begin(nkc.context, 25);
			nk_layout_row_template_push_static(nkc.context, 40);
			nk_layout_row_template_push_dynamic(nkc.context);
			nk_layout_row_template_end(nkc.context);
			nk_label(nkc.context, "Seed:", NK_TEXT_ALIGN_LEFT | NK_TEXT_ALIGN_CENTERED);
			seed = NuklearManager.textArea(nkc.context, stack, seed, 10, NK_EDIT_FIELD, Nuklear::nnk_filter_ascii);
		}

		return update;
	}

	public void appendConfig(String prefix, OutputStream out) throws IOException {
		Properties properties = new Properties();
		store(prefix, properties);
		properties.store(out, "Properties for the " + prefix + " noise.");
	}

	protected void store(String prefix, Properties properties) {

		properties.put(prefix + ".a", cubeRate);
		properties.put(prefix + ".b", squareRate);
		properties.put(prefix + ".c", flatRate);
		properties.put(prefix + ".d", additionalRate);

		properties.put(prefix + ".pre", preScale);
		properties.put(prefix + ".post", postScale);
		properties.put(prefix + ".seed", seed);
	}

	protected void read(String prefix, Properties properties) {
		try {
			cubeRate = Float.parseFloat(properties.getProperty(prefix + ".a", "0"));
		} catch (Exception e) {
			cubeRate = 0;
		}
		try {
			squareRate = Float.parseFloat(properties.getProperty(prefix + ".b", "0"));
		} catch (Exception e) {
			squareRate = 0;
		}
		try {
			flatRate = Float.parseFloat(properties.getProperty(prefix + ".c", "1"));
		} catch (Exception e) {
			flatRate = 1;
		}
		try {
			additionalRate = Float.parseFloat(properties.getProperty(prefix + ".d", "0"));
		} catch (Exception e) {
			additionalRate = 0;
		}

		try {
			preScale = Float.parseFloat(properties.getProperty(prefix + ".pre", "0"));
		} catch (Exception e) {
			preScale = 0;
		}
		try {
			postScale = Float.parseFloat(properties.getProperty(prefix + ".post", "0"));
		} catch (Exception e) {
			postScale = 0;
		}

		seed = properties.getProperty(prefix + ".d", "0");

	}

}
