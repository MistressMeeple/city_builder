package com.meeple.shared.frame.window;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWCursorEnterCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;

import com.meeple.backend.FrameTimings;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.FrameUtils;

public final class UserInput extends GLFWWrapper {
	private static Logger logger = Logger.getLogger(UserInput.class);

	public class GLFWKeyOrMouse {
		private final int key;
		private final EventOrigin origin;

		private GLFWKeyOrMouse(int key, EventOrigin origin) {
			this.key = key;
			this.origin = origin;
		}

		@Override
		public int hashCode() {
			return Integer.hashCode(key) * origin.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (this.getClass() != obj.getClass())
				return false;
			// Class name is Employ & have lastname
			GLFWKeyOrMouse other = (GLFWKeyOrMouse) obj;
			boolean sameKey = other.key == this.key;
			boolean sameOrigin = other.origin.equals(this.origin);
			return sameKey && sameOrigin;
		}

		@Override
		public String toString() {

			String name = "";
			if (origin == EventOrigin.Keyboard) {
				name = GLFW.glfwGetKeyName(key, 0);
				if (name == null) {
					switch (key) {
					case GLFW.GLFW_KEY_UNKNOWN:
						name = "Unknown";
						break;
					case GLFW.GLFW_KEY_SPACE:
						name = "Space";
						break;
					case GLFW.GLFW_KEY_GRAVE_ACCENT:
						name = "Grave";
						break;
					case GLFW.GLFW_KEY_ESCAPE:
						name = "Escape";
						break;
					case GLFW.GLFW_KEY_ENTER:
						name = "Enter";
						break;
					case GLFW.GLFW_KEY_TAB:
						name = "Tab";
						break;
					case GLFW.GLFW_KEY_BACKSPACE:
						name = "Backspace";
						break;
					case GLFW.GLFW_KEY_INSERT:
						name = "Insert";
						break;
					case GLFW.GLFW_KEY_DELETE:
						name = "Delete";
						break;
					case GLFW.GLFW_KEY_RIGHT:
						name = "Right";
						break;
					case GLFW.GLFW_KEY_LEFT:
						name = "Left";
						break;
					case GLFW.GLFW_KEY_DOWN:
						name = "Down";
						break;
					case GLFW.GLFW_KEY_UP:
						name = "Up";
						break;
					case GLFW.GLFW_KEY_PAGE_UP:
						name = "Page-Up";
						break;
					case GLFW.GLFW_KEY_PAGE_DOWN:
						name = "Page-Down";
						break;
					case GLFW.GLFW_KEY_HOME:
						name = "Home";
						break;
					case GLFW.GLFW_KEY_END:
						name = "End";
						break;
					case GLFW.GLFW_KEY_CAPS_LOCK:
						name = "Caps Lock";
						break;
					case GLFW.GLFW_KEY_SCROLL_LOCK:
						name = "Scroll Lock";
						break;
					case GLFW.GLFW_KEY_NUM_LOCK:
						name = "Num Lock";
						break;
					case GLFW.GLFW_KEY_PRINT_SCREEN:
						name = "Print Screen";
						break;
					case GLFW.GLFW_KEY_PAUSE:
						name = "Pause";
						break;
					case GLFW.GLFW_KEY_F1:
						name = "F1";
						break;
					case GLFW.GLFW_KEY_F2:
						name = "F2";
						break;
					case GLFW.GLFW_KEY_F3:
						name = "F3";
						break;
					case GLFW.GLFW_KEY_F4:
						name = "F4";
						break;
					case GLFW.GLFW_KEY_F5:
						name = "F5";
						break;
					case GLFW.GLFW_KEY_F6:
						name = "F6";
						break;
					case GLFW.GLFW_KEY_F7:
						name = "F7";
						break;
					case GLFW.GLFW_KEY_F8:
						name = "F8";
						break;
					case GLFW.GLFW_KEY_F9:
						name = "F9";
						break;
					case GLFW.GLFW_KEY_F10:
						name = "F10";
						break;
					case GLFW.GLFW_KEY_F11:
						name = "F11";
						break;
					case GLFW.GLFW_KEY_F12:
						name = "F12";
						break;
					case GLFW.GLFW_KEY_F13:
						name = "F13";
						break;
					case GLFW.GLFW_KEY_F14:
						name = "F14";
						break;
					case GLFW.GLFW_KEY_F15:
						name = "F15";
						break;
					case GLFW.GLFW_KEY_F16:
						name = "F16";
						break;
					case GLFW.GLFW_KEY_F17:
						name = "F17";
						break;
					case GLFW.GLFW_KEY_F18:
						name = "F18";
						break;
					case GLFW.GLFW_KEY_F19:
						name = "F19";
						break;
					case GLFW.GLFW_KEY_F20:
						name = "F20";
						break;
					case GLFW.GLFW_KEY_F21:
						name = "F21";
						break;
					case GLFW.GLFW_KEY_F22:
						name = "F22";
						break;
					case GLFW.GLFW_KEY_F23:
						name = "F23";
						break;
					case GLFW.GLFW_KEY_F24:
						name = "F24";
						break;
					case GLFW.GLFW_KEY_F25:
						name = "F25";
						break;
					case GLFW.GLFW_KEY_KP_ENTER:
						name = "Keypad-Enter";
						break;
					case GLFW.GLFW_KEY_LEFT_SHIFT:
						name = "Left Shift";
						break;
					case GLFW.GLFW_KEY_LEFT_CONTROL:
						name = "Left Control";
						break;
					case GLFW.GLFW_KEY_LEFT_ALT:
						name = "Left Alt";
						break;
					case GLFW.GLFW_KEY_LEFT_SUPER:
						name = "Left Super";
						break;
					case GLFW.GLFW_KEY_RIGHT_SHIFT:
						name = "Right Shift";
						break;
					case GLFW.GLFW_KEY_RIGHT_CONTROL:
						name = "Right Control";
						break;
					case GLFW.GLFW_KEY_RIGHT_ALT:
						name = "Right Alt";
						break;
					case GLFW.GLFW_KEY_RIGHT_SUPER:
						name = "Right Super";
						break;
					case GLFW.GLFW_KEY_MENU:
						name = "Menu";
						break;

					}
				}

			} else if (origin == EventOrigin.Mouse) {
				switch (key) {
				case GLFW.GLFW_MOUSE_BUTTON_LEFT:
					name = "Left click";
					break;
				case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
					name = "Right click";
					break;
				case GLFW.GLFW_MOUSE_BUTTON_MIDDLE:
					name = "Middle click";
					break;
				case GLFW.GLFW_MOUSE_BUTTON_4:
					name = "Mouse 4";
					break;
				case GLFW.GLFW_MOUSE_BUTTON_5:
					name = "Mouse 5";
					break;
				case GLFW.GLFW_MOUSE_BUTTON_6:
					name = "Mouse 6";
					break;
				case GLFW.GLFW_MOUSE_BUTTON_7:
					name = "Mouse 7";
					break;
				case GLFW.GLFW_MOUSE_BUTTON_8:
					name = "Mouse 8";
					break;
				default:
					break;
				}
			}
			try {
				if (name == null) {
					name = "Unregistered";
				}
				return origin.toString() + ": " + name;
			} catch (Exception e) {
				return super.toString();
			}

		}
	}

	public static enum EventOrigin {
		Keyboard, Mouse
	}

	/**
	 * This checks whether the specified key has been pressed THIS FRAME ONLY,
	 * generates a single frame event. <br>
	 * 
	 * @param key represented by GLFW.GLFW_KEY_X
	 * @return
	 *         <ul>
	 *         <li>{@link Boolean#TRUE} - if key has been pressed</li>
	 *         <li>{@link Boolean#FALSE} - if key has been released</li>
	 *         <li>null - if neither</li>
	 *         </ul>
	 */
	public Boolean keyPress(int key) {
		return eventSinglePressMap.get(new GLFWKeyOrMouse(key, EventOrigin.Keyboard));
	}

	/**
	 * This checks whether the specified mouse-button has been pressed THIS FRAME
	 * ONLY, generates a single frame event. <br>
	 * 
	 * @param key represented by GLFW.GLFW_MOUSE_BUTTON_X
	 * @return
	 *         <ul>
	 *         <li>{@link Boolean#TRUE} - if key has been pressed</li>
	 *         <li>{@link Boolean#FALSE} - if key has been released</li>
	 *         <li>null - if neither</li>
	 *         </ul>
	 */
	public Boolean mousePress(int key) {
		return eventSinglePressMap.get(new GLFWKeyOrMouse(key, EventOrigin.Mouse));
	}

	/**
	 * If {@link #keyPress(int)} is {@link Boolean#TRUE} then the runnable will be
	 * run
	 * 
	 * @param key     represented by GLFW.GLFW_KEY_X
	 * @param onPress Runnable to be run if this key was pressed this frame
	 */
	public void keyPress(int key, Runnable onPress) {
		Boolean press = eventSinglePressMap.get(new GLFWKeyOrMouse(key, EventOrigin.Keyboard));
		if (press != null && press == true && onPress != null) {
			onPress.run();
		}
	}

	/**
	 * If {@link #keyPress(int)} is {@link Boolean#TRUE} then the runnable will be
	 * run
	 * 
	 * @param key     represented by GLFW.GLFW_MOUSE_BUTTON_X
	 * @param onPress Runnable to be run if this key was pressed this frame
	 */
	public void mousePress(int key, Runnable onPress) {
		Boolean press = eventSinglePressMap.get(new GLFWKeyOrMouse(key, EventOrigin.Mouse));
		if (press != null && press == true) {
			onPress.run();
		}
	}

	/**
	 * If {@link #keyPress(int)} is {@link Boolean#FALSE} then the runnable will be
	 * run
	 * 
	 * @param key     represented by GLFW.GLFW_KEY_X
	 * @param onPress Runnable to be run if this key was released this frame
	 */
	public void keyRelease(int key, Runnable onRelease) {
		Boolean press = eventSinglePressMap.get(new GLFWKeyOrMouse(key, EventOrigin.Keyboard));
		if (press != null && press == false) {
			onRelease.run();
		}
	}

	/**
	 * If {@link #keyPress(int)} is {@link Boolean#FALSE} then the runnable will be
	 * run
	 * 
	 * @param key     represented by GLFW.GLFW_KEY_X
	 * @param onPress Runnable to be run if this key was released this frame
	 */
	public void mouseRelease(int key, Runnable onRelease) {
		Boolean press = eventSinglePressMap.get(new GLFWKeyOrMouse(key, EventOrigin.Mouse));
		if (press != null && press == true) {
			onRelease.run();
		}
	}

	/**
	 * returns true if the key is currently being triggered
	 * 
	 * @return true if is being triggered
	 */
	public boolean isPressed(KeyBinding query) {
		return FrameUtils.getOrDefault(eventPressMap, query.getKey(), false);
	}

	/**
	 * returns true if the event bound to a key or input is currently being
	 * triggered.<br>
	 * It is important to note that this can also be triggered by other means other
	 * than a key/button pressed since it is event-name based
	 * 
	 * @return true if is being triggered
	 */
	/*
	 * public boolean isPressed(String query) { return
	 * isPressed(keyBindings.get(query)); }
	 */

	/**
	 * Checks whether the key/mouse button has been pressed this frame, and this
	 * frame <b>only</b>
	 * 
	 * @param query
	 * @return
	 */
	public Boolean hasPressed(KeyBinding query) {
		return FrameUtils.getOrDefault(eventSinglePressMap, query.getKey(), false);
	}

	/*
	 * //temporarily removed because im not using string events but just using
	 * direct keybinding queries
	 * 
	 * private Boolean hasPressed(GLFWKeyOrMouse query) { return
	 * FrameUtils.getOrDefault(eventSinglePressMap, query, false); } public Boolean
	 * hasPressed(String query) { return hasPressed(keyBindings.get(query)); }
	 */

	public long getPressTime(GLFWKeyOrMouse key) {
		return FrameUtils.getOrDefault(eventPressTicks, key, 0l);
	}

	/*
	 * //temporarily removed because im not using string events but just using
	 * direct keybinding queries public long getPressTime(String eventName) { return
	 * getPressTime(keyBindings.get(eventName)); }
	 */

	/**
	 * This checks whether a keyboard key is being pressed.
	 * 
	 * @param key any key in the GLFW static list, eg {@link GLFW#GLFW_KEY_X}
	 * @return true if being pressed currently, false if not.
	 */
	@Deprecated
	public boolean isKeyPressed(int key) {
		return eventPressMap.getOrDefault(new GLFWKeyOrMouse(key, EventOrigin.Keyboard), false);
	}

	/**
	 * returns true if the GLFW mouse button is currently being pressed
	 * 
	 * @param key key represented by GLFW.GLFW_MOUSE_BUTTON_x
	 * @return true if is being pressed
	 */
	@Deprecated
	public boolean isMouseBtnPressed(int mouseBtn) {
		return eventPressMap.getOrDefault(new GLFWKeyOrMouse(mouseBtn, EventOrigin.Mouse), false);
	}

	/**
	 * returns the nano-seconds that the GLFW key has been pressed for
	 * 
	 * @param key key represented by GLFW.GLFW_KEY_x
	 * @return how long the key has been pressed for in nano seconds
	 */
	@Deprecated
	public long getKeyPressTime(int key) {
		return eventPressTicks.getOrDefault(new GLFWKeyOrMouse(key, EventOrigin.Keyboard), 0l);
	}

	/**
	 * returns the nano-seconds that the GLFW mouse button has been pressed for
	 * 
	 * @param key key represented by GLFW.GLFW_MOUSE_BUTTON_x
	 * @return how long the key has been pressed for in nano seconds
	 */
	@Deprecated
	public long getMouseBtnPressTime(int key) {
		return eventPressTicks.getOrDefault(new GLFWKeyOrMouse(key, EventOrigin.Mouse), 0l);
	}

	protected GLFWCharCallbackI charCallback;
	protected GLFWCharModsCallbackI charModsCallback;
	protected GLFWCursorEnterCallbackI cursorEnterCallback;
	protected GLFWCursorPosCallbackI cursorPosCallback;
	protected GLFWKeyCallbackI keyCallback;
	protected GLFWMouseButtonCallbackI mouseButtonCallback;
	protected GLFWScrollCallbackI scrollCallback;
	// protected GLFWJoystickCallbackI joystickCallback;

	public final Set<GLFWKeyCallbackI> keyCallbackSet = new CollectionSuppliers.SetSupplier<GLFWKeyCallbackI>().get();
	public final Set<GLFWMouseButtonCallbackI> mouseButtonCallbackSet = new CollectionSuppliers.SetSupplier<GLFWMouseButtonCallbackI>().get();
	public final Set<GLFWScrollCallbackI> scrollCallbackSet = new CollectionSuppliers.SetSupplier<GLFWScrollCallbackI>().get();
	public final Set<GLFWCharCallbackI> charCallbackSet = new CollectionSuppliers.SetSupplier<GLFWCharCallbackI>().get();
	public final Set<GLFWCharModsCallbackI> charModsCallbackSet = new CollectionSuppliers.SetSupplier<GLFWCharModsCallbackI>().get();
	public final Set<GLFWCursorEnterCallbackI> cursorEnterCallbackSet = new CollectionSuppliers.SetSupplier<GLFWCursorEnterCallbackI>().get();
	public final Set<GLFWCursorPosCallbackI> cursorPosCallbackSet = new CollectionSuppliers.SetSupplier<GLFWCursorPosCallbackI>().get();
	// public final Set<GLFWJoystickCallbackI> joystickCallbacks = new
	// CollectionSuppliers.SetSupplier<GLFWJoystickCallbackI>().get();
	private final Map<GLFWKeyOrMouse, Boolean> eventPressMap = new CollectionSuppliers.MapSupplier<GLFWKeyOrMouse, Boolean>().get();
	private final Map<GLFWKeyOrMouse, Long> eventPressTicks = new CollectionSuppliers.MapSupplier<GLFWKeyOrMouse, Long>().get();
	private final Map<GLFWKeyOrMouse, Boolean> eventSinglePressMap = new CollectionSuppliers.MapSupplier<GLFWKeyOrMouse, Boolean>().get();

	private Map<String, GLFWKeyOrMouse> keyBindings = Collections.synchronizedMap(new TreeMap<>());

	public class KeyBinding {

		private final String eventName;
		private GLFWKeyOrMouse key;

		public KeyBinding(String event, int key, EventOrigin origin) {
			this.eventName = event;
			rebind(new GLFWKeyOrMouse(key, origin));
		}

		public String getEventName() {
			return eventName;
		}

		public void rebind(GLFWKeyOrMouse key) {
			GLFWKeyOrMouse old = keyBindings.put(eventName, key);
			if (old != null) {
				logger.info("Event '" + eventName + "' has already been registered and is being overrided");
				logger.info(old.toString() + " is being replaced by " + key.toString());
			}
			this.key = key;
		}

		public GLFWKeyOrMouse getKey() {
			//return keyBindings.get(eventName);
			return key;
		}
	}

	public void tick(FrameTimings delta, long window) {

		Set<Entry<GLFWKeyOrMouse, Boolean>> set = eventPressMap.entrySet();
		synchronized (eventPressMap) {
			for (Iterator<Entry<GLFWKeyOrMouse, Boolean>> i = set.iterator(); i.hasNext();) {
				Entry<GLFWKeyOrMouse, Boolean> entry = i.next();
				GLFWKeyOrMouse key = entry.getKey();

				Boolean isPressed = entry.getValue();
				Long time = eventPressTicks.getOrDefault(key, 0l);
				if (isPressed) {
					if (time < 1l) {
						eventSinglePressMap.put(key, Boolean.TRUE);
					} else {
						eventSinglePressMap.remove(key);
					}
					time = time + delta.deltaNanos;
				} else {
					if (time > 0l) {
						eventSinglePressMap.put(key, Boolean.FALSE);
					} else {
						eventSinglePressMap.remove(key);
					}
					time = 0l;
					i.remove();
				}
				eventPressTicks.put(key, time);

			}
		}
	}

	public void eventHandle(EventOrigin origin, int key, int action) {
		GLFWKeyOrMouse store = new GLFWKeyOrMouse(key, origin);
		eventPressMap.put(store, action != GLFW.GLFW_RELEASE);
	}

	@Override
	public void bind(long windowID) {
		super.bind(windowID);
		initMirrors();
		// NOTE setup the mouse and key handling part of the callbacks
		keyCallbackSet.add((long _windowID, int key, int scancode, int action, int mods) -> eventHandle(EventOrigin.Keyboard, key, action));
		mouseButtonCallbackSet.add((long _windowID, int key, int action, int mods) -> eventHandle(EventOrigin.Mouse, key, action));

		GLFW.glfwSetCharCallback(windowID, charCallback);
		GLFW.glfwSetCharModsCallback(windowID, charModsCallback);
		GLFW.glfwSetCursorEnterCallback(windowID, cursorEnterCallback);
		GLFW.glfwSetCursorPosCallback(windowID, cursorPosCallback);
		GLFW.glfwSetKeyCallback(windowID, keyCallback);
		GLFW.glfwSetMouseButtonCallback(windowID, mouseButtonCallback);
		GLFW.glfwSetScrollCallback(windowID, scrollCallback);
		// GLFW.glfwSetJoystickCallback(joystickCallback);

	}

	private void initMirrors() {
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
		/*
		 * joystickCallback = new GLFWJoystickCallbackI() {
		 * 
		 * @Override public void invoke(int jid, int event) { if (joystickCallbacks !=
		 * null && !joystickCallbacks.isEmpty()) { synchronized (joystickCallbacks) {
		 * Iterator<GLFWJoystickCallbackI> i = joystickCallbacks.iterator(); while
		 * (i.hasNext()) { GLFWJoystickCallbackI c = i.next(); if (c != null) {
		 * c.invoke(jid, event); } else { i.remove(); }
		 * 
		 * } } } } };
		 */
	}

}
