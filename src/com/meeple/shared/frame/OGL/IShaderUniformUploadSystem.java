package com.meeple.shared.frame.OGL;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

/**
 * 
 * @author Megan
 *
 * @param <UploadObject> Object class that will be uploaded to shader
 */
public interface IShaderUniformUploadSystem<UploadObject, ID> {

	public abstract void uploadToShader(UploadObject upload, ID uniformID, MemoryStack stack);

	public static FloatBuffer generateMatrix4fBuffer(MemoryStack stack, Matrix4f matrix) {

		return stack
			.floats(
				matrix.m00(),
				matrix.m01(),
				matrix.m02(),
				matrix.m03(),
				matrix.m10(),
				matrix.m11(),
				matrix.m12(),
				matrix.m13(),
				matrix.m20(),
				matrix.m21(),
				matrix.m22(),
				matrix.m23(),
				matrix.m30(),
				matrix.m31(),
				matrix.m32(),
				matrix.m33());
	}
}
