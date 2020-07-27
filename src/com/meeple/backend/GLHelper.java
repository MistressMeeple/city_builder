package com.meeple.backend;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL46;

import com.meeple.shared.frame.OGL.GLEnum;
import com.meeple.shared.frame.OGL.ShaderProgram;

@SuppressWarnings("static-access")
public class GLHelper {
	private static GL46 gl;

	public static class GLSLAttribute {
		/**
		 * Name of the attribute as in the shader code
		 */
		public String name;
		/**
		 * The assigned index of the attribute, or the start of the indices if it is larger than 4
		 */
		public int index;
		/**
		 * The size of the array that represents the attribute (normal attributes have an array size of 1)
		 */
		public int arraySize;
		public AttributeType type;
	}

	/*Byte(GL46.GL_BYTE, 8),
	UnsignedByte(GL46.GL_UNSIGNED_BYTE, 16),
	Short(GL46.GL_SHORT, 16),
	UnsignedShort(GL46.GL_UNSIGNED_SHORT, 16),
	Int(GL46.GL_INT, 32),
	UnsignedInt(GL46.GL_UNSIGNED_INT, 32),
	Fixed(GL46.GL_FIXED, 32),
	HalfFloat(GL46.GL_HALF_FLOAT, 16),
	Float(GL46.GL_FLOAT, 32),
	Double(GL46.GL_DOUBLE, 64);*/
	public static enum AttributeType implements GLEnum {

		Float(gl.GL_FLOAT, 1, 32),
		Vec2f(gl.GL_FLOAT_VEC2, Float, 2),
		Vec3f(gl.GL_FLOAT_VEC3, Float, 3),
		Vec4f(gl.GL_FLOAT_VEC4, Float, 4),
		Mat2f(gl.GL_FLOAT_MAT2, Float, 4),
		Mat3f(gl.GL_FLOAT_MAT3, Float, 9),
		Mat4f(gl.GL_FLOAT_MAT4, Float, 16),
		Mat2x3f(gl.GL_FLOAT_MAT2x3, Float, 6),
		Mat2x4f(gl.GL_FLOAT_MAT2x4, Float, 8),
		Mat3x2f(gl.GL_FLOAT_MAT3x2, Float, 6),
		Mat3x4f(gl.GL_FLOAT_MAT3x4, Float, 12),
		Mat4x2f(gl.GL_FLOAT_MAT4x2, Float, 8),
		Mat4x3f(gl.GL_FLOAT_MAT4x3, Float, 12),
		Int(gl.GL_INT, 1, 32),
		Vec2i(gl.GL_INT_VEC2, Int, 2),
		Vec3i(gl.GL_INT_VEC3, Int, 3),
		Vec4i(gl.GL_INT_VEC4, Int, 4),
		Unsigned_Int(gl.GL_UNSIGNED_INT, Int, 1),
		Vec2ui(gl.GL_UNSIGNED_INT_VEC2, Int, 2),
		Vec3ui(gl.GL_UNSIGNED_INT_VEC3, Int, 3),
		Vec4ui(gl.GL_UNSIGNED_INT_VEC4, Int, 4),
		Double(gl.GL_DOUBLE, 1, 64),
		Vec2d(gl.GL_DOUBLE_VEC2, Double, 2),
		Vec3d(gl.GL_DOUBLE_VEC3, Double, 3),
		Vec4d(gl.GL_DOUBLE_VEC4, Double, 4),
		Mat2d(gl.GL_DOUBLE_MAT2, Double, 4),
		Mat3d(gl.GL_DOUBLE_MAT3, Double, 9),
		Mat4d(gl.GL_DOUBLE_MAT4, Double, 16),
		Mat2x3d(gl.GL_DOUBLE_MAT2x3, Double, 6),
		Mat2x4d(gl.GL_DOUBLE_MAT2x4, Double, 8),
		Mat3x2d(gl.GL_DOUBLE_MAT3x2, Double, 6),
		Mat3x4d(gl.GL_DOUBLE_MAT3x4, Double, 12),
		Mat4x2d(gl.GL_DOUBLE_MAT4x2, Double, 8),
		Mat4x3d(gl.GL_DOUBLE_MAT4x3, Double, 12),
		Other;

		private int id = -1;
		private int size;
		private AttributeType base;
		private int bytes;

		private AttributeType() {

		}

		private AttributeType(int id, int size, int bits) {
			this.id = id;
			this.size = size;
			this.base = this;
			this.bytes = bits / 8;
		}

		private AttributeType(int id, AttributeType base, int size) {
			this.id = id;
			this.size = size;
			this.base = base;
			;
		}

		@Override
		public int getGLID() {
			return id;
		}

		public int getSize() {
			return size;
		}

		public AttributeType getBase() {
			return base;
		}

		public int getBytes() {
			return bytes;
		}

		public static AttributeType fromID(int id) {
			for (AttributeType type : values()) {
				if (id == type.id) {
					return type;
				}
			}
			return Other;
		}
	}

	/**
	 * Wrapper function for {@link GL46#glGetActiveAttrib(int, int, IntBuffer, IntBuffer)}
	 * @param program
	 * @param index
	 * @return
	 */
	public static GLSLAttribute glGetActiveAttrib(ShaderProgram program, int index) {
		return glGetActiveAttrib(program.programID, index);

	}

	/**
	 * 
	 * @param program
	 * @param arrayElement Array count of the attribute NOT the attribute location
	 * @return
	 */
	public static GLSLAttribute glGetActiveAttrib(int program, int arrayElement) {

		IntBuffer size_b = BufferUtils.createIntBuffer(1);
		IntBuffer type_b = BufferUtils.createIntBuffer(1);

		String name = gl.glGetActiveAttrib(program, arrayElement, size_b, type_b);
		int size = size_b.get();
		int typeID = type_b.get();
		int index = gl.glGetAttribLocation(program, name);

		AttributeType type = AttributeType.fromID(typeID);
		GLSLAttribute attrib = new GLSLAttribute();
		attrib.index = index;
		attrib.name = name;
		attrib.arraySize = size;
		attrib.type = type;
		return attrib;

	}

	private static int currentDiff(int index, int dataLength) {
		int mod = dataLength % 4;
		return (index * 4 <= dataLength ? 4 : mod);
	}

	public static void setupAttrib(int program, GLSLAttribute glAtt, int instanceStride) {

		int dataSize = glAtt.arraySize * glAtt.type.size;
		if (dataSize > 4) {
			int index = 0;
			int runningTotal = 0;
			for (int i = 0; i < dataSize; i += 4) {
				int currDiff = currentDiff(index, dataSize);
				int runningIndex = glAtt.index + index;
				int size = currDiff;
				int type = glAtt.type.base.getGLID();
				boolean normalized = false;
				int stride = glAtt.arraySize * glAtt.type.size * glAtt.type.base.bytes;
				int offset = runningTotal * glAtt.type.base.bytes;

				gl
					.glVertexAttribPointer(
						runningIndex,
						size,
						type,
						normalized,
						stride,
						offset);
				if (instanceStride > 0) {
					gl.glVertexAttribDivisor(runningIndex, instanceStride);

				}
				index++;
				runningTotal += currDiff;
			}
		} else {

			int index = glAtt.index;
			int size = dataSize;
			int type = glAtt.type.base.getGLID();
			boolean normalized = false;
			int stride = 0, offset = 0;

			gl.glVertexAttribPointer(index, size, type, normalized, stride, offset);
			if (instanceStride > 0) {
				GL46.glVertexAttribDivisor(index, instanceStride);
			}
		}
	}
}
