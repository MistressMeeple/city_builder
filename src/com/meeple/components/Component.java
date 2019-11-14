package com.meeple.components;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface Component {

	public Map<String, Object> getComponents();

	public static <T> T get(Component comp, String id) {
		return (T) comp.getComponents().get(id);
	}

	public static class BaseComponent implements Component {
		Map<String, Object> components = Collections.synchronizedMap(new HashMap<>());

		@Override
		public Map<String, Object> getComponents() {
			return components;
		}

	}
}
