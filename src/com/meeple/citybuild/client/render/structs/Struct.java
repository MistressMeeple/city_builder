package com.meeple.citybuild.client.render.structs;

import com.meeple.shared.frame.OGL.ShaderProgram;

public abstract class Struct {

	public static int sizeOf = 0;

	public static int dataOffset(int size, int nth) {
		int a = size - 1;
		int b = a / 4;
		int c = b + 1;
		int d = c * ShaderProgram.GLDataType.Float.getBytes();
		int e = nth * d;
		return e;
	}

	public abstract float[] toArray(float[] arr, int i);
}
