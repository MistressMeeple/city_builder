package com.meeple.shared.frame.window;

import java.util.Iterator;
import java.util.Set;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWDropCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;
import org.lwjgl.glfw.GLFWWindowContentScaleCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;
import org.lwjgl.glfw.GLFWWindowIconifyCallbackI;
import org.lwjgl.glfw.GLFWWindowMaximizeCallbackI;
import org.lwjgl.glfw.GLFWWindowPosCallbackI;
import org.lwjgl.glfw.GLFWWindowRefreshCallbackI;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;
import org.lwjgl.opengl.GLDebugMessageCallbackI;

import com.meeple.shared.CollectionSuppliers;

public class MirroredWindowCallbacks {

	protected GLFWDropCallbackI dropCallback;
	protected GLFWFramebufferSizeCallbackI frameBufferSizeCallback;
	protected GLFWWindowCloseCallbackI windowCloseCallback;
	protected GLFWWindowContentScaleCallbackI windowContentScaleCallback;
	protected GLFWWindowFocusCallbackI windowFocusCallback;
	protected GLFWWindowIconifyCallbackI windowIconifyCallback;
	protected GLFWWindowMaximizeCallbackI windowMaximizeCallback;
	protected GLFWWindowPosCallbackI windowPosCallback;
	protected GLFWWindowRefreshCallbackI windowRefreshCallback;
	protected GLFWWindowSizeCallbackI windowSizeCallback;
	protected GLDebugMessageCallbackI debugMessageCallback;
	/**
	 * @deprecated
	 */
	protected GLFWKeyCallbackI keyCallback;
	/**
	 * @deprecated
	 */
	protected GLFWScrollCallbackI scrollCallback;
	/**
	 * @deprecated
	 */
	protected GLFWMouseButtonCallbackI mouseButtonCallback;
	/**
	 * @deprecated
	 */
	protected GLFWCursorPosCallbackI cursorPosCallback;

	public Set<GLFWDropCallbackI> dropCallbackSet = new CollectionSuppliers.SetSupplier<GLFWDropCallbackI>().get();
	public Set<GLFWFramebufferSizeCallbackI> frameBufferSizeCallbackSet = new CollectionSuppliers.SetSupplier<GLFWFramebufferSizeCallbackI>().get();

	public Set<GLFWWindowCloseCallbackI> windowCloseCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowCloseCallbackI>().get();
	public Set<GLFWWindowContentScaleCallbackI> windowContentScaleCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowContentScaleCallbackI>().get();
	public Set<GLFWWindowFocusCallbackI> windowFocusCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowFocusCallbackI>().get();
	public Set<GLFWWindowIconifyCallbackI> windowIconifyCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowIconifyCallbackI>().get();
	public Set<GLFWWindowMaximizeCallbackI> windowMaximizeCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowMaximizeCallbackI>().get();
	public Set<GLFWWindowPosCallbackI> windowPosCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowPosCallbackI>().get();
	public Set<GLFWWindowRefreshCallbackI> windowRefreshCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowRefreshCallbackI>().get();
	public Set<GLFWWindowSizeCallbackI> windowSizeCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowSizeCallbackI>().get();
	public Set<GLDebugMessageCallbackI> debugMessageCallbackSet = new CollectionSuppliers.SetSupplier<GLDebugMessageCallbackI>().get();

	/**
	 * @deprecated
	 */
	public Set<GLFWKeyCallbackI> keyCallbackSet = new CollectionSuppliers.SetSupplier<GLFWKeyCallbackI>().get();
	/**
	 * @deprecated
	 */
	public Set<GLFWScrollCallbackI> scrollCallbackSet = new CollectionSuppliers.SetSupplier<GLFWScrollCallbackI>().get();
	/**
	 * @deprecated
	 */
	public Set<GLFWMouseButtonCallbackI> mouseButtonCallbackSet = new CollectionSuppliers.SetSupplier<GLFWMouseButtonCallbackI>().get();
	/**
	 * @deprecated
	 */
	public Set<GLFWCursorPosCallbackI> cursorPosCallbackSet = new CollectionSuppliers.SetSupplier<GLFWCursorPosCallbackI>().get();

	public MirroredWindowCallbacks() {
		// NOTE setup the mouse and key handling part of the callbacks

		dropCallback = new GLFWDropCallbackI() {

			@Override
			public void invoke(long window, int count, long names) {
				if (dropCallbackSet != null && !dropCallbackSet.isEmpty()) {
					synchronized (dropCallbackSet) {

						Iterator<GLFWDropCallbackI> i = dropCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWDropCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, count, names);
							} else {
								i.remove();
							}
						}

					}
				}
			}
		};
		frameBufferSizeCallback = new GLFWFramebufferSizeCallbackI() {

			@Override
			public void invoke(long window, int width, int height) {
				if (frameBufferSizeCallbackSet != null && !frameBufferSizeCallbackSet.isEmpty()) {
					synchronized (frameBufferSizeCallbackSet) {

						Iterator<GLFWFramebufferSizeCallbackI> i = frameBufferSizeCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWFramebufferSizeCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, width, height);
							} else {
								i.remove();
							}
						}
					}
				}
			}
		};

		windowCloseCallback = new GLFWWindowCloseCallbackI() {

			@Override
			public void invoke(long window) {

				if (windowCloseCallbackSet != null && !windowCloseCallbackSet.isEmpty()) {
					synchronized (windowCloseCallbackSet) {

						Iterator<GLFWWindowCloseCallbackI> i = windowCloseCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWWindowCloseCallbackI c = i.next();
							if (c != null) {
								c.invoke(window);
							} else {
								i.remove();
							}
						}
					}
				}
			}
		};
		windowContentScaleCallback = new GLFWWindowContentScaleCallbackI() {

			@Override
			public void invoke(long window, float xscale, float yscale) {

				if (windowContentScaleCallbackSet != null && !windowContentScaleCallbackSet.isEmpty()) {
					synchronized (windowContentScaleCallbackSet) {

						Iterator<GLFWWindowContentScaleCallbackI> i = windowContentScaleCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWWindowContentScaleCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, xscale, yscale);
							} else {
								i.remove();
							}
						}

					}
				}
			}
		};
		windowFocusCallback = new GLFWWindowFocusCallbackI() {

			@Override
			public void invoke(long window, boolean focused) {

				if (windowFocusCallbackSet != null && !windowFocusCallbackSet.isEmpty()) {
					synchronized (windowFocusCallbackSet) {

						Iterator<GLFWWindowFocusCallbackI> i = windowFocusCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWWindowFocusCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, focused);
							} else {
								i.remove();
							}
						}

					}
				}
			}
		};
		windowIconifyCallback = new GLFWWindowIconifyCallbackI() {

			@Override
			public void invoke(long window, boolean iconified) {

				if (windowIconifyCallbackSet != null && !windowIconifyCallbackSet.isEmpty()) {
					synchronized (windowIconifyCallbackSet) {

						Iterator<GLFWWindowIconifyCallbackI> i = windowIconifyCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWWindowIconifyCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, iconified);
							} else {
								i.remove();
							}
						}
					}
				}
			}
		};
		windowMaximizeCallback = new GLFWWindowMaximizeCallbackI() {

			@Override
			public void invoke(long window, boolean maximized) {
				if (windowMaximizeCallbackSet != null && !windowMaximizeCallbackSet.isEmpty()) {
					synchronized (windowMaximizeCallbackSet) {

						Iterator<GLFWWindowMaximizeCallbackI> i = windowMaximizeCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWWindowMaximizeCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, maximized);
							} else {
								i.remove();
							}
						}
					}
				}
			}
		};
		windowPosCallback = new GLFWWindowPosCallbackI() {

			@Override
			public void invoke(long window, int xpos, int ypos) {
				if (windowPosCallbackSet != null && !windowPosCallbackSet.isEmpty()) {
					synchronized (windowPosCallbackSet) {
						Iterator<GLFWWindowPosCallbackI> i = windowPosCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWWindowPosCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, xpos, ypos);
							} else {
								i.remove();
							}

						}
					}
				}
			}
		};

		windowRefreshCallback = new GLFWWindowRefreshCallbackI() {

			@Override
			public void invoke(long window) {
				if (windowRefreshCallbackSet != null && !windowRefreshCallbackSet.isEmpty()) {
					synchronized (windowRefreshCallbackSet) {
						Iterator<GLFWWindowRefreshCallbackI> i = windowRefreshCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWWindowRefreshCallbackI c = i.next();
							if (c != null) {
								c.invoke(window);
							} else {
								i.remove();
							}
						}
					}
				}
			}
		};
		windowSizeCallback = new GLFWWindowSizeCallbackI() {

			@Override
			public void invoke(long window, int width, int height) {
				if (windowSizeCallbackSet != null && !windowSizeCallbackSet.isEmpty()) {
					synchronized (windowSizeCallbackSet) {
						Iterator<GLFWWindowSizeCallbackI> i = windowSizeCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWWindowSizeCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, width, height);
							} else {
								i.remove();
							}

						}
					}
				}
			}
		};
		debugMessageCallback = new GLDebugMessageCallbackI() {

			@Override
			public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {
				if (debugMessageCallbackSet != null && !debugMessageCallbackSet.isEmpty()) {
					synchronized (debugMessageCallbackSet) {
						Iterator<GLDebugMessageCallbackI> i = debugMessageCallbackSet.iterator();
						while (i.hasNext()) {
							GLDebugMessageCallbackI c = i.next();
							if (c != null) {
								c.invoke(source, type, id, severity, length, message, userParam);
							} else {
								i.remove();
							}

						}
					}
				}
			}
		};
		keyCallback = new GLFWKeyCallbackI() {

			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {

				if (keyCallbackSet != null && !keyCallbackSet.isEmpty()) {
					synchronized (debugMessageCallbackSet) {
						Iterator<GLFWKeyCallbackI> i = keyCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWKeyCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, key, scancode, action, mods);
							} else {
								i.remove();
							}

						}
					}
				}
			}
		};
		scrollCallback = new GLFWScrollCallbackI() {

			@Override
			public void invoke(long window, double xoffset, double yoffset) {

				if (scrollCallbackSet != null && !scrollCallbackSet.isEmpty()) {
					synchronized (debugMessageCallbackSet) {
						Iterator<GLFWScrollCallbackI> i = scrollCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWScrollCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, xoffset, yoffset);
							} else {
								i.remove();
							}

						}
					}
				}
			}
		};
		mouseButtonCallback = new GLFWMouseButtonCallbackI() {

			@Override
			public void invoke(long window, int button, int action, int mods) {

				if (mouseButtonCallbackSet != null && !mouseButtonCallbackSet.isEmpty()) {
					synchronized (debugMessageCallbackSet) {
						Iterator<GLFWMouseButtonCallbackI> i = mouseButtonCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWMouseButtonCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, button, action, mods);
							} else {
								i.remove();
							}

						}
					}
				}

			}
		};
		cursorPosCallback = new GLFWCursorPosCallbackI() {

			@Override
			public void invoke(long window, double xpos, double ypos) {

				if (cursorPosCallbackSet != null && !cursorPosCallbackSet.isEmpty()) {
					synchronized (debugMessageCallbackSet) {
						Iterator<GLFWCursorPosCallbackI> i = cursorPosCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWCursorPosCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, xpos, ypos);
							} else {
								i.remove();
							}

						}
					}
				}

			}
		};
	}

	public void bindToWindow(long windowID) {

		if (dropCallback != null)
			GLFW.glfwSetDropCallback(windowID, dropCallback);
		if (frameBufferSizeCallback != null)
			GLFW.glfwSetFramebufferSizeCallback(windowID, frameBufferSizeCallback);
		if (windowCloseCallback != null)
			GLFW.glfwSetWindowCloseCallback(windowID, windowCloseCallback);
		if (windowContentScaleCallback != null)
			GLFW.glfwSetWindowContentScaleCallback(windowID, windowContentScaleCallback);
		if (windowFocusCallback != null)
			GLFW.glfwSetWindowFocusCallback(windowID, windowFocusCallback);
		if (windowIconifyCallback != null)
			GLFW.glfwSetWindowIconifyCallback(windowID, windowIconifyCallback);
		if (windowMaximizeCallback != null)
			GLFW.glfwSetWindowMaximizeCallback(windowID, windowMaximizeCallback);
		if (windowPosCallback != null)
			GLFW.glfwSetWindowPosCallback(windowID, windowPosCallback);
		if (windowRefreshCallback != null)
			GLFW.glfwSetWindowRefreshCallback(windowID, windowRefreshCallback);
		if (windowSizeCallback != null)
			GLFW.glfwSetWindowSizeCallback(windowID, windowSizeCallback);

		if (keyCallback != null) {
			GLFW.glfwSetKeyCallback(windowID, keyCallback);
		}
		GLFW.glfwSetMouseButtonCallback(windowID, mouseButtonCallback);
		GLFW.glfwSetScrollCallback(windowID, scrollCallback);
		GLFW.glfwSetCursorPosCallback(windowID, cursorPosCallback);

	}

}
