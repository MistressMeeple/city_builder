package com.meeple.shared.frame.exception;

import com.meeple.shared.frame.window.Window;

public class WindowNotCreatedException extends Exception {

	private static final long serialVersionUID = 4446806714041248800L;

	Window window;

	public WindowNotCreatedException(Window window) {
		super("Window hasnt been created. ");
		this.window = window;
	}

	public Window getWindow() {
		return window;
	}
}
