package com.meeple.shared.frame.OGL;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

public class KeyInputSystem {
	/*
		public GLFWKeyCallbackI registerKeyCallback(Map<Integer, Boolean> keyPressMap) {
			return new GLFWKeyCallbackI() {
	
				@Override
				public void invoke(long window, int key, int scancode, int action, int mods) {
					eventHandleKey(keyPressMap, window, key, scancode, action, mods);
				}
			};
		}
	
		public GLFWMouseButtonCallbackI registerMouseKeyCallback(Map<Integer, Boolean> mouseKeyPressMap) {
	
			return (new GLFWMouseButtonCallbackI() {
	
				@Override
				public void invoke(long window, int button, int action, int mods) {
					eventHandleKey(mouseKeyPressMap, window, button, 0, action, mods);
	
				}
			});
		}*/

	public void tick(Map<Integer, Long> keyPressMap, Map<Integer, Boolean> buttonPress, long delta) {
		synchronized (buttonPress) {

			Set<Entry<Integer, Boolean>> set = buttonPress.entrySet();
			synchronized (buttonPress) {
				for (Iterator<Entry<Integer, Boolean>> iterator = set.iterator(); iterator.hasNext();) {
					Entry<Integer, Boolean> entry = iterator.next();
					Integer key = entry.getKey();
					Boolean isPressed = entry.getValue();

					Long time = keyPressMap.getOrDefault(key, 0l);
					if (isPressed) {
						keyPressMap.put(key, time + delta);
					} else {
						keyPressMap.put(key, 0l);
					}
				}
			}
		}

	}

	public void eventHandleKey(Map<Integer, Boolean> keyPressMap, long window, int key, int scancode, int action, int mods) {
		keyPressMap.put(key, action != GLFW.GLFW_RELEASE);
	}

	public void eventHandleMouse(Map<Integer, Boolean> mousePressMap, long window, int mouseKey, int action, int mods) {
		mousePressMap.put(mouseKey, action != GLFW.GLFW_RELEASE);
	}


}
