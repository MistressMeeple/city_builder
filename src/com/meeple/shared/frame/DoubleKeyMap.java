package com.meeple.shared.frame;

import java.util.List;
import java.util.Map;

import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;

public class DoubleKeyMap<V> {
	private List<V> registeredAttributes = new CollectionSuppliers.ListSupplier<V>().get();
	private Map<String, Integer> attribNames = new CollectionSuppliers.MapSupplier<String, Integer>().get();
	private Map<Integer, Integer> attribIndicies = new CollectionSuppliers.MapSupplier<Integer, Integer>().get();

	public void registerAttribute(int index, V data) {
		int reg = registeredAttributes.size();
		registeredAttributes.add(reg, data);
		attribIndicies.put(index, reg);
	}

	public void registerAttribute(String name, V data) {
		int reg = registeredAttributes.size();
		registeredAttributes.add(reg, data);
		attribNames.put(name, reg);
	}

	public void registerAttribute(int index, String name, V data) {
		int reg = registeredAttributes.size();
		registeredAttributes.add(reg, data);
		attribIndicies.put(index, reg);
		attribNames.put(name, reg);
	}

	public V get(int index) {
		try {
			return registeredAttributes.get(attribIndicies.get(index));
		} catch (Exception e) {
			return null;
		}
	}

	public V get(String name) {
		try {
			return registeredAttributes.get(attribNames.get(name));
		} catch (Exception e) {
			return null;
		}
	}

}
