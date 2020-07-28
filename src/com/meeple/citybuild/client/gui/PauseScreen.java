package com.meeple.citybuild.client.gui;

import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_BORDER;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_NO_SCROLLBAR;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_TITLE;
import static org.lwjgl.nuklear.Nuklear.nk_begin;
import static org.lwjgl.nuklear.Nuklear.nk_button_label;
import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_rect;
import static org.lwjgl.system.MemoryStack.stackPush;

import org.apache.log4j.Logger;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.system.MemoryStack;

import com.meeple.citybuild.client.render.Screen;
import com.meeple.shared.Delta;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;
import com.meeple.shared.frame.window.ClientWindowSystem.WindowEvent;

public class PauseScreen extends Screen {

	public static Logger logger = Logger.getLogger(PauseScreen.class);

	@Override
	public void render(ClientWindow window, Delta delta) {

		NkContext ctx = window.nkContext.context;

		try (MemoryStack stack = stackPush()) {
			int width = (int) (window.bounds.width * 0.25f);
			int height = (int) (window.bounds.height * 0.5f);
			int x = (int) (window.bounds.width - width) / 2;
			int y = (int) (window.bounds.height - height) / 2;
			NkRect rect = NkRect.mallocStack(stack);
			if (nk_begin(ctx, "Pause", nk_rect(x, y, width, height, rect), NK_WINDOW_BORDER | NK_WINDOW_NO_SCROLLBAR | NK_WINDOW_TITLE)) {
				nk_layout_row_dynamic(ctx, 35, 1);

				int size = 50;

				nk_layout_row_dynamic(ctx, size + 25, 1);

				if (nk_button_label(ctx, "Resume")) {
					window.sendEvent(WindowEvent.GameResume);
				}
				if (nk_button_label(ctx, "Options")) {
					window.sendEvent(WindowEvent.OptionsOpen);
				}
				if (nk_button_label(ctx, "Main Menu")) {
					window.sendEvent(WindowEvent.GameSave);
					window.sendEvent(WindowEvent.GoToMainMenu);
				}

			}
			nk_end(ctx);
		} /*
			try (MemoryStack stack = stackPush()) {
			NkRect rect = NkRect.mallocStack(stack);
			NkColor alphad = NkColor.mallocStack(stack);
			NuklearManager
				.setNkColour(alphad, (int) (this.colour.x * Byte.MAX_VALUE), (int) (this.colour.y * Byte.MAX_VALUE), (int) (this.colour.z * Byte.MAX_VALUE), (int) (this.colour.w * Byte.MAX_VALUE));
			
			nk_style_push_color(ctx, ctx.style().window().background(), alphad);
			if (nk_begin(ctx, "bg", nk_rect(0, 0, 100, 100, rect), NK_WINDOW_NO_SCROLLBAR)) {
			
				//				nk_style_push_color(ctx, ctx.style().window().fixed_background().data().color(), alphad);
				nk_label(ctx, "", 0);
			
			}
			nk_style_pop_color(ctx);
			}*/
	}

}
