package com.meeple.citybuild.client.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.meeple.components.IDComponent;
import com.meeple.components.NamedComponent;
import com.meeple.shared.frame.nuklear.NuklearUIComponent;

public class Screen implements NamedComponent, IDComponent<Integer> {
	private Map<String, Object> components = new HashMap<>();
	Set<NuklearUIComponent> uiComponents = new HashSet<>();

	@Override
	public Map<String, Object> getComponents() {
		// TODO Auto-generated method stub
		return components;
	}

	//	public abstract void render();

}
