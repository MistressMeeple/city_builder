package com.meeple.shared.frame.window;

import org.joml.Vector2d;
import org.lwjgl.glfw.GLFW;

public class GLFWWrapper {

	public static enum ButtonPressState {
		Press(GLFW.GLFW_PRESS), Release(GLFW.GLFW_RELEASE), Repeat(GLFW.GLFW_REPEAT);
		final int id;

		private ButtonPressState(int id) {
			this.id = id;
		}

		public int getID() {
			return id;
		}

		public static ButtonPressState fromID(int id) {
			for (ButtonPressState b : values()) {
				if (b.id == id) {
					return b;
				}
			}
			return null;
		}
	}

	private long window;

	public void bind(long window) {
		this.window = window;
	}

	public void setInputMode(int mode, int value) {
		GLFW.glfwSetInputMode(window, mode, value);
	}

	public int getInputMode(int mode) {
		return GLFW.glfwGetInputMode(window, mode);
	}

	public Vector2d getCursorPos() {
		double[] xpos = new double[1], ypos = new double[1];
		GLFW.glfwGetCursorPos(window, xpos, ypos);
		return new Vector2d(xpos[0], ypos[0]);
	}

	public void setCursorPos(Vector2d pos) {
		setCursorPos(pos.x, pos.y);
	}

	public void setCursorPos(double x, double y) {
		GLFW.glfwSetCursorPos(window, x, y);
	}

}
