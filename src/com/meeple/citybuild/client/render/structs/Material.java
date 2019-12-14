package com.meeple.citybuild.client.render.structs;

import org.joml.Vector3f;

public class Material extends Struct {
	{
		sizeOf = 9;
	}

	public Vector3f ambient = new Vector3f(), diffuse = new Vector3f();
	public float ambientStrength = 1f, diffuseStrength = 0.5f, lightScaling = 1f;

	@Override
	public float[] toArray(float[] arr, int i) {

		arr[i++] = ambient.x;
		arr[i++] = ambient.y;
		arr[i++] = ambient.z;

		arr[i++] = diffuse.x;
		arr[i++] = diffuse.y;
		arr[i++] = diffuse.z;

		arr[i++] = ambientStrength;
		arr[i++] = diffuseStrength;
		arr[i++] = lightScaling;

		return arr;

	}
}
