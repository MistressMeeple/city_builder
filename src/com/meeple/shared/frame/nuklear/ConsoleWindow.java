package com.meeple.shared.frame.nuklear;

import static org.lwjgl.nuklear.Nuklear.*;

import java.util.function.BiConsumer;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkColorf;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.frame.window.Window;

public class ConsoleWindow extends NuklearUIComponent {
	public ConsoleWindow(Window window) {
		title = "Console";
		render = new BiConsumer<NkContext, MemoryStack>() {

			NkColorf background = NkColorf
				.create()
				.r(0.10f)
				.g(0.18f)
				.b(0.24f)
				.a(1.0f);

			private float master = 50;
			private float game = 50;
			private float music = 50;

			@Override
			public void accept(NkContext context, MemoryStack stack) {
				nk_layout_row_static(context, 30, 80, 3);
				if (nk_button_label(context, "button")) {
					System.out.println("button2 pressed");
				}
				nk_layout_row_static(context, 30, 80, 1);
				nk_text(context, "Sound", Nuklear.NK_TEXT_ALIGN_CENTERED);
				nk_layout_row_dynamic(context, 25, 3);
				nk_text(context, "Master:", NK_TEXT_ALIGN_LEFT);
				master = nk_slide_float(context, 0, master, 100, 10);
				nk_text(context, master + "%", NK_TEXT_ALIGN_RIGHT);

				nk_text(context, "Game:", NK_TEXT_ALIGN_LEFT);
				game = nk_slide_float(context, 0, game, 100, 10);
				nk_text(context, game + "%", NK_TEXT_ALIGN_RIGHT);

				nk_text(context, "Music:", NK_TEXT_ALIGN_LEFT);
				music = nk_slide_float(context, 0, music, 100, 10);
				nk_text(context, music + "%", NK_TEXT_ALIGN_RIGHT);

				nk_layout_row_dynamic(context, 20, 1);
				nk_label(context, "background:", NK_TEXT_LEFT);
				nk_layout_row_dynamic(context, 25, 1);
				if (nk_combo_begin_color(context, nk_rgb_cf(background, NkColor.mallocStack(stack)), NkVec2.mallocStack(stack).set(nk_widget_width(context), 400))) {
					nk_layout_row_dynamic(context, 120, 1);
					nk_color_picker(context, background, NK_RGBA);
					nk_layout_row_dynamic(context, 25, 1);
					background
						.r(nk_propertyf(context, "#R:", 0, background.r(), 1.0f, 0.01f, 0.005f))
						.g(nk_propertyf(context, "#G:", 0, background.g(), 1.0f, 0.01f, 0.005f))
						.b(nk_propertyf(context, "#B:", 0, background.b(), 1.0f, 0.01f, 0.005f))
						.a(nk_propertyf(context, "#A:", 0, background.a(), 1.0f, 0.01f, 0.005f));
					nk_combo_end(context);
				}
			}
		};
		bounds = window.bounds;
	}
}
