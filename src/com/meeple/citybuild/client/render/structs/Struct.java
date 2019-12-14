package com.meeple.citybuild.client.render.structs;

public abstract class Struct {

	public static int sizeOf = 0;
	public abstract float[] toArray(float[] arr, int i);

	public float[] toArray() {
		return toArray(new float[sizeOf], 0);
	}

	public int dataOffset(int nth) {
		return nth * ((sizeOf - 1) / (4)) + 1;
	}

}
