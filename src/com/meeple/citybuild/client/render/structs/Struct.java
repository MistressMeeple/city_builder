package com.meeple.citybuild.client.render.structs;

/**
 * A class that represents a struct in GLSL code. <br>
 * {@link #toArray(float[], int)} handles the raw data conversion to be uploaded properly to GLSL<br>
 * {@link #dataOffset(int, int)} handles the stride offsets for instanced data sets
 * @author Megan
 *
 */
public abstract class Struct {

	private static final int maxDataInBuffer = 4;

	/**
	 * Returns the offset of the nth element represented in an array of GLSL data
	 * @param size size of the struct
	 * @param nth which element this is, with a 0-index
	 * @return the data offset in bytes from the begging of the data that this element starts
	 */
	public static int dataOffset(int size, int nth) {
		int a = size - 1;
		int b = a / maxDataInBuffer;
		int c = b + 1;
		int d = c * 4;
		int e = nth * d;
		return e;
	}
/**
 * Writes all the data from the struct into the specified array starting at i
 * @param array to write data into 
 * @param i starting index
 * @return the modified array 
 */
	public abstract float[] toArray(float[] array, int i);
}
