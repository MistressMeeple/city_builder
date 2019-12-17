package com.meeple.citybuild.client.render.structs;

import org.joml.Vector3f;
/**
 * a light holds the data represented in the shader code. <br>
 * colour, position, attenuation and whether or not it is enabled<br>
 * This class also manages the conversion to raw data to be uploaded and its data size in the GLSL code<br>
 * Function for calculating attenuation can be found <a href="https://www.desmos.com/calculator/bd8ujvojbu"> here</a>
 * @author Megan
 *
 */
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
//		arr[i++] = 0;

		arr[i++] = (enabled ? 1 : 0);

		return arr;
	}
}
