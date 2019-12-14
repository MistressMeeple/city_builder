package com.meeple.citybuild.client.render.structs;

import org.joml.Vector3f;

public class Light extends Struct {

	{
		sizeOf = 10;
	}
	public boolean enabled = true;
	public final Vector3f colour = new Vector3f(), position = new Vector3f(), attenuation = new Vector3f();

	@Override
	public float[] toArray(float[] arr, int i) {
		arr[i++] = (enabled ? 1 : 0);

		arr[i++] = colour.x;
		arr[i++] = colour.y;
		arr[i++] = colour.z;

		arr[i++] = position.x;
		arr[i++] = position.y;
		arr[i++] = position.z;

		arr[i++] = attenuation.x;
		arr[i++] = attenuation.y;
		arr[i++] = attenuation.z;

		return arr;
	}
}
