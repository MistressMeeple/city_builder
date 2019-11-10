package com.meeple.shared.frame.window.hints;

import org.lwjgl.glfw.GLFW;

public enum ContextRobustness implements HasID<Integer> {
	NO_ROBUSTNESS(GLFW.GLFW_NO_ROBUSTNESS), NO_RESET_NOTIFICATION(GLFW.GLFW_NO_RESET_NOTIFICATION), LOSE_CONTEXT_ON_RESET(GLFW.GLFW_LOSE_CONTEXT_ON_RESET);
	int id;

	private ContextRobustness(int id) {
		this.id = id;
	}

	@Override
	public Integer getID() {
		return id;
	}
}

