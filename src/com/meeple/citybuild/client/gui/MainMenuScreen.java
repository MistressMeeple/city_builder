package com.meeple.citybuild.client.gui;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;

import org.apache.log4j.Logger;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.system.MemoryStack;

import com.meeple.citybuild.client.render.Renderable;
import com.meeple.shared.Delta;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;
import com.meeple.shared.frame.window.ClientWindowSystem.WindowEvent;

public class MainMenuScreen extends Renderable {
	public static Logger logger = Logger.getLogger(MainMenuScreen.class);

	@Override
	public void render(NkContextSingleton nkContext, ClientWindow window, Delta delta) {
		try (MemoryStack stack = stackPush()) {
			NkContext ctx = nkContext.context;
			Long width = (long) (window.bounds.width * 0.25f) - 5;
			Long height = (long) (window.bounds.height * 0.4f);
			long x = (long) (window.bounds.width / 2 - width) / 2;
			long y = 0;// (long) (window.bounds.height - height);

			NkRect rect = NkRect.mallocStack(stack);
			NkColor alpha = NkColor.mallocStack(stack);
			alpha.set((byte) 0, (byte) 0, (byte) 0, (byte) 0);
			nk_style_push_color(ctx, ctx.style().window().fixed_background().data().color(), alpha);
			if (nk_begin(ctx, "Main Menu", nk_rect(x, y, width, window.bounds.height, rect), 0)) {

				int size = 50;

				nk_layout_row_static(ctx, (long) (window.bounds.height - height) / 2, 1, 1);
				nk_spacing(ctx, 1);
				nk_layout_row_dynamic(ctx, size, 1);

				if (nk_button_label(ctx, "Play")) {
					window.sendEvent(WindowEvent.GameLoad);

					window.sendEvent(WindowEvent.GameStart);
				}
				if (nk_button_label(ctx, "Options")) {
					window.sendEvent(WindowEvent.OptionsOpen);
				}
				if (nk_button_label(ctx, "Quit")) {
					window.sendEvent(WindowEvent.ClientClose);

				}

			}
			nk_end(ctx);
			nk_style_pop_color(ctx);
		}
	}

}
