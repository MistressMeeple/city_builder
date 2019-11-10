package com.meeple.shared.frame;

import static org.lwjgl.glfw.GLFW.*;

import java.util.Objects;

import org.apache.log4j.Logger;
import org.lwjgl.glfw.GLFWErrorCallback;

public class GLFWManager implements AutoCloseable {

	private static Logger logger = Logger.getLogger(GLFWManager.class);

	public GLFWManager() {
		create();
	}

	public void create() {

		logger.debug("Initialising GLFW");
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

	}

	@Override
	public void close() {
		logger.debug("Terminating GLFW");
		// Terminate GLFW and free the error callback
		glfwTerminate();
		Objects.requireNonNull(glfwSetErrorCallback(null)).free();
	}
}
