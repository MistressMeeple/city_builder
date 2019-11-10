package com.meeple.shared.frame.window.hints;

import org.lwjgl.glfw.GLFW;

public enum ClientAPI implements HasID<Integer> {
	NO_API(GLFW.GLFW_NO_API), OPENGL_API(GLFW.GLFW_OPENGL_API), OPENGL_ES_API(GLFW.GLFW_OPENGL_ES_API);
	int id;

	private ClientAPI(int id) {
		this.id = id;
	}

	@Override
	public Integer getID() {
		return id;
	}

}
