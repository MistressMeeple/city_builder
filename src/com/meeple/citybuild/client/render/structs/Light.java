package com.meeple.citybuild.client.render.structs;

import org.joml.Vector3f;

public class Light extends Struct {

	public static int sizeOf = 12;

	public boolean enabled = true;
	public final Vector3f colour = new Vector3f(), position = new Vector3f(), attenuation = new Vector3f();

	@Override
	public float[] toArray(float[] arr, int i) {

		arr[i++] = colour.x;
		arr[i++] = colour.y;
		arr[i++] = colour.z;
		arr[i++] = 0;		
		
		arr[i++] = position.x;
		arr[i++] = position.y;
		arr[i++] = position.z;
		arr[i++] = 0;
		
		arr[i++] = attenuation.x;
		arr[i++] = attenuation.y;
		arr[i++] = attenuation.z;
		
		arr[i++] = (enabled ? 1 : 0);

		return arr;
	}
}
