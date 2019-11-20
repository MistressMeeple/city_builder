package com.meeple.citybuild.client.gui;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;

import org.apache.log4j.Logger;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.system.MemoryStack;

import com.meeple.citybuild.client.render.Renderable;
import com.meeple.shared.Delta;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;
import com.meeple.shared.frame.window.ClientWindowSystem.WindowEvent;

public class PauseScreen extends Renderable {

	public static Logger logger = Logger.getLogger(PauseScreen.class);

	@Override
	public void render(NkContextSingleton nkContext, ClientWindow window,Delta delta) {
		NkContext ctx = nkContext.context;
		int width = (int) (window.bounds.width * 0.25f);
		int height = (int) (window.bounds.height * 0.5f);
		int x = (int) (window.bounds.width - width) / 2;
		int y = (int) (window.bounds.height - height) / 2;

		try (MemoryStack stack = stackPush()) {
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
		}
	}

}
