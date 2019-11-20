package com.meeple.citybuild.client.gui;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import com.meeple.citybuild.client.render.Renderable;
import com.meeple.shared.Delta;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.nuklear.NuklearManager;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;

public class GameUI extends Renderable {


	String string = "";

	public void render(NkContextSingleton context, ClientWindow window,Delta delta) {
		NkContext ctx = context.context;
		int width = (int) (window.bounds.width * 0.75f);
		int height = (int) (window.bounds.height * 0.5f);
		int x = (int) (window.bounds.width - width) / 2;
		int y = (int) (window.bounds.height - height);

		try (MemoryStack stack = stackPush()) {
			NkRect rect = NkRect.mallocStack(stack);
			if (nk_begin(ctx, "test", nk_rect(x, y, width, height, rect), NK_WINDOW_BORDER | NK_WINDOW_NO_SCROLLBAR | NK_WINDOW_TITLE | NK_WINDOW_SCALABLE)) {
				nk_layout_row_dynamic(ctx, 35, 1);

				string = NuklearManager.textArea(ctx, stack, string, 256, NK_EDIT_SIMPLE, Nuklear::nnk_filter_ascii);

				int size = 50;
				int buttons = 3;
				int spacing = 10;

				nk_layout_row_dynamic(ctx, size + 25, 1);

				if (nk_group_begin(ctx, "", 0)) {
					nk_layout_space_begin(ctx, Nuklear.NK_STATIC, size, buttons);
					for (int i = 0; i < buttons; i++) {

						nk_layout_space_push(ctx, nk_rect((size + spacing) * i, 0, size, size, NkRect.create()));
						if (nk_button_label(ctx, "Btn" + i)) {
							System.out.println("Btn " + i);
						}

					}
					nk_layout_space_end(ctx);
					nk_group_end(ctx);
				}

			}
			nk_end(ctx);
		}
	}

}
