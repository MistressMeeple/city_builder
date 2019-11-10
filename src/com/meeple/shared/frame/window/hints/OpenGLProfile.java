package com.meeple.shared.frame.window.hints;

import org.lwjgl.glfw.GLFW;

public enum OpenGLProfile implements HasID<Integer> {
	OPENGL_ANY_PROFILE(GLFW.GLFW_OPENGL_ANY_PROFILE), OPENGL_CORE_PROFILE(GLFW.GLFW_OPENGL_CORE_PROFILE), OPENGL_COMPAT_PROFILE(GLFW.GLFW_OPENGL_COMPAT_PROFILE);
	int id;

	private OpenGLProfile(int id) {
		this.id = id;
	}

	@Override
	public Integer getID() {
		return id;
	}
}
