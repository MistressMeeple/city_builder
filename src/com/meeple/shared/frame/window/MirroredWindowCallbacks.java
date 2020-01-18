package com.meeple.shared.frame.window;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWCursorEnterCallbackI;
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
import com.meeple.shared.frame.FrameUtils;

public class MirroredWindowCallbacks {

	protected GLFWCharCallbackI charCallback;
	protected GLFWCharModsCallbackI charModsCallback;
	protected GLFWCursorEnterCallbackI cursorEnterCallback;
	protected GLFWCursorPosCallbackI cursorPosCallback;
	protected GLFWDropCallbackI dropCallback;
	protected GLFWFramebufferSizeCallbackI frameBufferSizeCallback;
	protected GLFWKeyCallbackI keyCallback;
	protected GLFWMouseButtonCallbackI mouseButtonCallback;
	protected GLFWScrollCallbackI scrollCallback;
	protected GLFWWindowCloseCallbackI windowCloseCallback;
	protected GLFWWindowContentScaleCallbackI windowContentScaleCallback;
	protected GLFWWindowFocusCallbackI windowFocusCallback;
	protected GLFWWindowIconifyCallbackI windowIconifyCallback;
	protected GLFWWindowMaximizeCallbackI windowMaximizeCallback;
	protected GLFWWindowPosCallbackI windowPosCallback;
	protected GLFWWindowRefreshCallbackI windowRefreshCallback;
	protected GLFWWindowSizeCallbackI windowSizeCallback;
	protected GLDebugMessageCallbackI debugMessageCallback;

	public Set<GLFWCharCallbackI> charCallbackSet = new CollectionSuppliers.SetSupplier<GLFWCharCallbackI>().get();
	public Set<GLFWCharModsCallbackI> charModsCallbackSet = new CollectionSuppliers.SetSupplier<GLFWCharModsCallbackI>().get();
	public Set<GLFWCursorEnterCallbackI> cursorEnterCallbackSet = new CollectionSuppliers.SetSupplier<GLFWCursorEnterCallbackI>().get();
	public Set<GLFWCursorPosCallbackI> cursorPosCallbackSet = new CollectionSuppliers.SetSupplier<GLFWCursorPosCallbackI>().get();
	public Set<GLFWDropCallbackI> dropCallbackSet = new CollectionSuppliers.SetSupplier<GLFWDropCallbackI>().get();
	public Set<GLFWFramebufferSizeCallbackI> frameBufferSizeCallbackSet = new CollectionSuppliers.SetSupplier<GLFWFramebufferSizeCallbackI>().get();
	public Set<GLFWKeyCallbackI> keyCallbackSet = new CollectionSuppliers.SetSupplier<GLFWKeyCallbackI>().get();
	public Set<GLFWMouseButtonCallbackI> mouseButtonCallbackSet = new CollectionSuppliers.SetSupplier<GLFWMouseButtonCallbackI>().get();
	public Set<GLFWScrollCallbackI> scrollCallbackSet = new CollectionSuppliers.SetSupplier<GLFWScrollCallbackI>().get();
	public Set<GLFWWindowCloseCallbackI> windowCloseCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowCloseCallbackI>().get();
	public Set<GLFWWindowContentScaleCallbackI> windowContentScaleCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowContentScaleCallbackI>().get();
	public Set<GLFWWindowFocusCallbackI> windowFocusCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowFocusCallbackI>().get();
	public Set<GLFWWindowIconifyCallbackI> windowIconifyCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowIconifyCallbackI>().get();
	public Set<GLFWWindowMaximizeCallbackI> windowMaximizeCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowMaximizeCallbackI>().get();
	public Set<GLFWWindowPosCallbackI> windowPosCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowPosCallbackI>().get();
	public Set<GLFWWindowRefreshCallbackI> windowRefreshCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowRefreshCallbackI>().get();
	public Set<GLFWWindowSizeCallbackI> windowSizeCallbackSet = new CollectionSuppliers.SetSupplier<GLFWWindowSizeCallbackI>().get();
	public Set<GLDebugMessageCallbackI> debugMessageCallbackSet = new CollectionSuppliers.SetSupplier<GLDebugMessageCallbackI>().get();

	private Map<Integer, Set<Runnable>> keyPresses = new CollectionSuppliers.MapSupplier<Integer, Set<Runnable>>().get();

	private final Map<Integer, Boolean> keyPressMap = new CollectionSuppliers.MapSupplier<Integer, Boolean>().get();
	private final Map<Integer, Long> keyPressTicks = new CollectionSuppliers.MapSupplier<Integer, Long>().get();
	private final Map<Integer, Boolean> mousePressMap = new CollectionSuppliers.MapSupplier<Integer, Boolean>().get();
	private final Map<Integer, Long> mousePressTicks = new CollectionSuppliers.MapSupplier<Integer, Long>().get();

	public void onKeypress(int key, Runnable handler) {
		FrameUtils.addToSetMap(keyPresses, key, handler, new CollectionSuppliers.SetSupplier<>());
	}

	/**
	 * returns true if the GLFW key is being pressed
	 * @param key key represented by GLFW.GLFW_KEY_x
	 * @return true if is being pressed 
	 */
	public boolean isKeyPressed(int key) {
		
		return keyPressMap.getOrDefault(key, false);
	}

	/**
	 * returns true if the GLFW mouse button is being pressed
	 * @param key key represented by GLFW.GLFW_MOUSE_BUTTON_x
	 * @return true if is being pressed 
	 */
	public boolean isMouseBtnPressed(int mouseBtn) {
		return mousePressMap.getOrDefault(mouseBtn, false);
	}

	public long getKeyPressTime(int key) {
		return keyPressTicks.getOrDefault(key, 0l);
	}

	public long getMouseBtnPressTime(int key) {
		return mousePressTicks.getOrDefault(key, 0l);
	}

	private void eventHandleKey(Map<Integer, Boolean> keyPressMap, long window, int key, int scancode, int action, int mods) {
		keyPressMap.put(key, action != GLFW.GLFW_RELEASE);
	}

	private void eventHandleMouse(Map<Integer, Boolean> mousePressMap, long window, int mouseKey, int action, int mods) {
		mousePressMap.put(mouseKey, action != GLFW.GLFW_RELEASE);
	}

	public void tick(long delta) {

		Set<Entry<Integer, Boolean>> keySet = keyPressMap.entrySet();
		synchronized (keyPressMap) {
			for (Iterator<Entry<Integer, Boolean>> iterator = keySet.iterator(); iterator.hasNext();) {
				Entry<Integer, Boolean> entry = iterator.next();
				Integer key = entry.getKey();
				Boolean isPressed = entry.getValue();

				Long time = keyPressTicks.getOrDefault(key, 0l);
				if (isPressed) {
					keyPressTicks.put(key, time + delta);
				} else {
					keyPressTicks.put(key, 0l);
				}
			}

		}

		Set<Entry<Integer, Boolean>> mouseSet = mousePressMap.entrySet();
		synchronized (mousePressMap) {
			for (Iterator<Entry<Integer, Boolean>> iterator = mouseSet.iterator(); iterator.hasNext();) {
				Entry<Integer, Boolean> entry = iterator.next();
				Integer key = entry.getKey();
				Boolean isPressed = entry.getValue();

				Long time = mousePressTicks.getOrDefault(key, 0l);
				if (isPressed) {
					mousePressTicks.put(key, time + delta);
				} else {
					mousePressTicks.put(key, 0l);
				}
			}

		}
	}

	public MirroredWindowCallbacks() {
		//NOTE setup the mouse and key handling part of the callbacks
		keyCallbackSet.add((long windowID, int key, int scancode, int action, int mods) -> eventHandleKey(keyPressMap, windowID, key, scancode, action, mods));
		mouseButtonCallbackSet.add((long windowID, int key, int action, int mods) -> eventHandleMouse(mousePressMap, windowID, key, action, mods));

		charCallback = new GLFWCharCallbackI() {

			@Override
			public void invoke(long window, int codepoint) {

				if (charCallbackSet != null && !charCallbackSet.isEmpty()) {
					synchronized (charCallbackSet) {

						Iterator<GLFWCharCallbackI> i = charCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWCharCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, codepoint);
							} else {
								i.remove();
							}

						}
					}
				}
			}
		};
		charModsCallback = new GLFWCharModsCallbackI() {

			@Override
			public void invoke(long window, int codepoint, int mods) {
				if (charModsCallbackSet != null && !charModsCallbackSet.isEmpty()) {
					synchronized (charModsCallbackSet) {

						Iterator<GLFWCharModsCallbackI> i = charModsCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWCharModsCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, codepoint, mods);
							} else {
								i.remove();
							}

						}
					}
				}
			}
		};
		cursorEnterCallback = new GLFWCursorEnterCallbackI() {

			@Override
			public void invoke(long window, boolean entered) {
				if (cursorEnterCallbackSet != null && !cursorEnterCallbackSet.isEmpty()) {
					synchronized (cursorEnterCallbackSet) {

						Iterator<GLFWCursorEnterCallbackI> i = cursorEnterCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWCursorEnterCallbackI c = i.next();
							if (c != null) {
								c.invoke(window, entered);
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
					synchronized (cursorPosCallbackSet) {

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
		keyCallback = new GLFWKeyCallbackI() {

			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {

				if (keyCallbackSet != null && !keyCallbackSet.isEmpty()) {
					synchronized (keyCallbackSet) {

						Iterator<GLFWKeyCallbackI> i = keyCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWKeyCallbackI c = (GLFWKeyCallbackI) i.next();
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

		mouseButtonCallback = new GLFWMouseButtonCallbackI() {

			@Override
			public void invoke(long window, int button, int action, int mods) {

				if (mouseButtonCallbackSet != null && !mouseButtonCallbackSet.isEmpty()) {
					synchronized (mouseButtonCallbackSet) {

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
		scrollCallback = new GLFWScrollCallbackI() {

			@Override
			public void invoke(long window, double xoffset, double yoffset) {

				if (scrollCallbackSet != null && !scrollCallbackSet.isEmpty()) {
					synchronized (scrollCallbackSet) {

						Iterator<GLFWScrollCallbackI> i = scrollCallbackSet.iterator();
						while (i.hasNext()) {

							GLFWScrollCallbackI c = (GLFWScrollCallbackI) i.next();
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

		windowCloseCallback = new GLFWWindowCloseCallbackI() {

			@Override
			public void invoke(long window) {

				if (windowCloseCallbackSet != null && !windowCloseCallbackSet.isEmpty()) {
					synchronized (windowCloseCallbackSet) {

						Iterator<GLFWWindowCloseCallbackI> i = windowCloseCallbackSet.iterator();
						while (i.hasNext()) {
							GLFWWindowCloseCallbackI c = (GLFWWindowCloseCallbackI) i.next();
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
							GLFWWindowContentScaleCallbackI c = (GLFWWindowContentScaleCallbackI) i.next();
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
							GLFWWindowFocusCallbackI c = (GLFWWindowFocusCallbackI) i.next();
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
							GLFWWindowIconifyCallbackI c = (GLFWWindowIconifyCallbackI) i.next();
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
							GLFWWindowMaximizeCallbackI c = (GLFWWindowMaximizeCallbackI) i.next();
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
							GLFWWindowPosCallbackI c = (GLFWWindowPosCallbackI) i.next();
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
							GLFWWindowSizeCallbackI c = (GLFWWindowSizeCallbackI) i.next();
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
							GLDebugMessageCallbackI c = (GLDebugMessageCallbackI) i.next();
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
	}

	public void bindToWindow(long windowID) {

		if (charCallback != null)
			GLFW.glfwSetCharCallback(windowID, charCallback);
		if (charModsCallback != null)
			GLFW.glfwSetCharModsCallback(windowID, charModsCallback);
		if (cursorEnterCallback != null)
			GLFW.glfwSetCursorEnterCallback(windowID, cursorEnterCallback);
		if (cursorPosCallback != null)
			GLFW.glfwSetCursorPosCallback(windowID, cursorPosCallback);
		if (dropCallback != null)
			GLFW.glfwSetDropCallback(windowID, dropCallback);
		if (frameBufferSizeCallback != null)
			GLFW.glfwSetFramebufferSizeCallback(windowID, frameBufferSizeCallback);
		if (keyCallback != null)
			GLFW.glfwSetKeyCallback(windowID, keyCallback);
		if (mouseButtonCallback != null)
			GLFW.glfwSetMouseButtonCallback(windowID, mouseButtonCallback);
		if (scrollCallback != null)
			GLFW.glfwSetScrollCallback(windowID, scrollCallback);
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

	}

}
