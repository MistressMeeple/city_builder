package com.meeple.citybuild.client.gui;

import java.util.HashSet;
import java.util.Set;

import com.meeple.components.IDComponent;
import com.meeple.components.NamedComponent;
import com.meeple.shared.frame.nuklear.NuklearUIComponent;

public class Screen implements NamedComponent, IDComponent<Integer> {
	
	Set<NuklearUIComponent> uiComponents = new HashSet<>();

	@Override
	public Integer getID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setID(Integer id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}


	//	public abstract void render();

}
