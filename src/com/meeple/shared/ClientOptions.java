package com.meeple.shared;

import java.util.Map;

import com.meeple.shared.ClientOptionSystem.Delimeter;
import com.meeple.shared.utils.CollectionSuppliers;

public class ClientOptions {

	public static class boolOptions {
		public static final String invertMouseYName = "invertMouseY";
		public static final String invertMouseXName = "invertMouseX";
		public static final String muteMasterVolume = "muteMasterVolume";
		public static final String muteGameVolume = "muteGameVolume";
		public static final String muteMusicVolume = "muteMusicVolume";
		public static final String fullscreenName = "fullscreen";

	}

	public static class intOptions {

		public static final String masterVolumeName = "masterVolume";
		public static final String gameVolumeName = "gameVolume";
		public static final String musicVolumeName = "musicVolume";
		public static final String mouseSensitivityName = "mouseSensitivity";
		public static final String fovName = "fov";
	}

	public static class stringOptions {

	}

	protected Map<Delimeter, Map<String, Object>> options = new CollectionSuppliers.MapSupplier<Delimeter, Map<String, Object>>().get();

	public ClientOptions() {
		for (Delimeter d : Delimeter.values()) {
			options.put(d, new CollectionSuppliers.MapSupplier<String, Object>().get());
		}

	}

	public void put(String name, Object obj) {
		Delimeter d = Delimeter.get(obj);
		Map<String, Object> map = options.get(d);
		map.put(name, obj);
		//		options.get(delim).put(name, obj);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Delimeter delim, String name) {
		Map<?, ?> m = options.get(delim);
		T t = (T) delim.clazz.cast(m.get(name));
		return t;

	}

	@SuppressWarnings("unchecked")
	public <T> T get(Delimeter delim, String name, T defaultValue) {
		Map<?, ?> m = options.get(delim);
		T t = (T) delim.clazz.cast(m.get(name));
		if (t != null) {
			return t;
		} else {
			return defaultValue;
		}

	}
}
