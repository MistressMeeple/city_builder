package com.meeple.citybuild.client.render.structs;

import org.joml.Vector3f;
import org.joml.Vector4f;

public class Material extends Struct {
	public static int sizeOf = 9;
	
	
	float baseScale;
	float reflectStrenght;
	
	public final Vector4f baseColour = new Vector4f();
	public final Vector3f reflectiveTint = new Vector3f();
	public float baseColourStrength = 0.5f, reflectivityStrength = 0.5f;

	@Override
	public float[] toArray(float[] arr, int i) {

		arr[i++] = baseColour.x;
		arr[i++] = baseColour.y;
		arr[i++] = baseColour.z;
		arr[i++] = baseColour.w;


		arr[i++] = reflectiveTint.x;
		arr[i++] = reflectiveTint.y;
		arr[i++] = reflectiveTint.z;

		arr[i++] = baseColourStrength;
		arr[i++] = reflectivityStrength;

		return arr;

	}
}
