package com.meeple.shared.frame.window.hints;

import org.lwjgl.glfw.GLFW;

public enum ContextCreationAPI implements HasID<Integer> {
	NATIVE_CONTEXT_API(GLFW.GLFW_NATIVE_CONTEXT_API), EGL_CONTEXT_API(GLFW.GLFW_EGL_CONTEXT_API), OSMESA_CONTEXT_API(GLFW.GLFW_OSMESA_CONTEXT_API);
	int id;

	private ContextCreationAPI(int id) {
		this.id = id;
	}

	@Override
	public Integer getID() {
		return id;
	}
};
