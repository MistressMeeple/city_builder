package com.meeple.shared.frame.nuklear;

import java.util.EnumSet;

import org.lwjgl.nuklear.Nuklear;

public enum NkWindowProperties {
	BORDER(Nuklear.NK_WINDOW_BORDER),
	MOVABLE(Nuklear.NK_WINDOW_MOVABLE),
	SCALABLE(Nuklear.NK_WINDOW_SCALABLE),
	CLOSABLE(Nuklear.NK_WINDOW_CLOSABLE),
	MINIMIZABLE(Nuklear.NK_WINDOW_MINIMIZABLE),
	NO_SCROLLBAR(Nuklear.NK_WINDOW_NO_SCROLLBAR),
	TITLE(Nuklear.NK_WINDOW_TITLE),
	SCROLL_AUTO_HIDE(Nuklear.NK_WINDOW_SCROLL_AUTO_HIDE),
	BACKGROUND(Nuklear.NK_WINDOW_BACKGROUND),
	SCALE_LEFT(Nuklear.NK_WINDOW_SCALE_LEFT),
	NO_INPUT(Nuklear.NK_WINDOW_NO_INPUT);

	int flag;

	private NkWindowProperties(int flag) {
		this.flag = flag;
	}

	public static int flags(EnumSet<NkWindowProperties> set) {
		int result = 0;
		if (set != null) {
			for (NkWindowProperties prop : set) {
				result += prop.flag;
			}
		}
		return result;

	}

}
