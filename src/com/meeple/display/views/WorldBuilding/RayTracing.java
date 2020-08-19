package com.meeple.display.views.WorldBuilding;

import org.apache.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.meeple.shared.frame.CursorHelper;
import com.meeple.shared.frame.CursorHelper.SpaceState;

public class RayTracing {

	private static Logger logger = Logger.getLogger(RayTracing.class);
	Matrix4f projection, view;;

	public RayTracing() {

	}
	public Vector3f getRay() {
		return null;
	}
	public void update(float mouseX,float mouseY) {
//		Vector4f cursorRay = CursorHelper.getMouse(SpaceState.World_Space, cityBuilder.window, vpMatrix.proj.getWrapped(), vpMatrix.view.getWrapped());
		
		
	}
	
	
}
