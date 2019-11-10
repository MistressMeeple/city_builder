package com.meeple.shared.frame.OGL;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public abstract class MultiUniformUploadSystemBase<T> implements IShaderUniformUploadSystem<T, Integer[]> {

	public class ArrayBuilder {
		Map<Integer, String> temp = new TreeMap<>();

		public ArrayBuilder add(int index, String name) {
			temp.put(index, name);
			return this;
		}

		public String[] build() {
			String[] ret = new String[temp.size()];

			Set<Entry<Integer, String>> set = temp.entrySet();
			synchronized (temp) {
				for (Iterator<Entry<Integer, String>> iterator = set.iterator(); iterator.hasNext();) {
					Entry<Integer, String> entry = iterator.next();
					Integer key = entry.getKey();
					String value = entry.getValue();
					ret[key] = value;
				}
			}
			return ret;
		}
	}

	protected UniformManager<String[], Integer[]>.Uniform<T> register(UniformManager<String[], Integer[]> system, ArrayBuilder builder) {
		return system.register(builder.build(), this);
	}

}
