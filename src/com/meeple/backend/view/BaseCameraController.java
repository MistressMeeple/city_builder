package com.meeple.backend.view;

import org.joml.Matrix4f;
import org.lwjgl.system.NativeType;

import com.meeple.backend.Client;
import com.meeple.backend.FrameTimings;
import com.meeple.shared.frame.window.UserInput;

public abstract class BaseCameraController {

	protected Matrix4f operateOn;

	public BaseCameraController() {
	}

	public void operateOn(Matrix4f operateOn) {
		this.operateOn = operateOn;
	}

	public abstract boolean tick(FrameTimings delta, Client client);

	public void register(long window, UserInput callbacks) {
		callbacks.cursorPosCallbackSet.add(this::cursorCallback);
		callbacks.keyCallbackSet.add(this::keyCallback);
		callbacks.scrollCallbackSet.add(this::scrollCallback);
	}

	protected void cursorCallback(@NativeType("GLFWwindow *") long window, double xpos, double ypos) {

	}

	protected void keyCallback(@NativeType("GLFWwindow *") long window, int key, int scancode, int action, int mods) {

	}

	protected void scrollCallback(@NativeType("GLFWwindow *") long window, double xoffset, double yoffset) {

	}

}
