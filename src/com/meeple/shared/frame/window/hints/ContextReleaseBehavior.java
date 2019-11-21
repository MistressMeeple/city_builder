package com.meeple.shared.frame.window.hints;

import org.lwjgl.glfw.GLFW;

public enum ContextReleaseBehavior implements HasID<Integer> {
	ANY_RELEASE_BEHAVIOR(GLFW.GLFW_ANY_RELEASE_BEHAVIOR), RELEASE_BEHAVIOR_FLUSH(GLFW.GLFW_RELEASE_BEHAVIOR_FLUSH), RELEASE_BEHAVIOR_NONE(GLFW.GLFW_RELEASE_BEHAVIOR_NONE);
	int id;

	private ContextReleaseBehavior(int id) {
		this.id = id;
	}

	@Override
	public Integer getID() {
		return id;
	}
}
