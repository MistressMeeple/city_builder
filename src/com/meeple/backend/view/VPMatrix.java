package com.meeple.backend.view;

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;
import static org.lwjgl.opengl.GL31.glUniformBlockBinding;

import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL46;

import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram;

public class VPMatrix {

	public final class CameraKey {
		private final int keyIndex;

		public CameraKey(int key) {
			this.keyIndex = key;
		}
	}

	public static final int Default_VP_Matrix_Binding_Point = 2;
	private int matrixBuffer;
	private int boundTo = Default_VP_Matrix_Binding_Point;

	private int activeCamera = 0;
	private List<Matrix4f> cameras = new CollectionSuppliers.ListSupplier<Matrix4f>().get();

	//limited use
	private Matrix4f projectionMatrix = new Matrix4f();
	//internal use only
	private Matrix4f viewProjectionMatrix = new Matrix4f();

	public Matrix4f getActiveCamera() {
		return cameras.get(activeCamera);
	}
	/**
	 * Returns the main camera matrix
	 * @return matrix4f representing the camera
	 */
	public Matrix4f getCamera(CameraKey key) {
		return cameras.get(key.keyIndex);
	}

	/**
	 * Returns the main camera matrix with the additional operation of setting the camera as active
	 * @return matrix4f representing the camera
	 */
	public Matrix4f getCamera(CameraKey key, boolean setActive) {
		if (setActive)
			activeCamera(key);

		return cameras.get(key.keyIndex);
	}

	public void setBindingPoint(int binding) {
		boundTo = binding;
	}

	public int getBindingPoint() {
		return boundTo;
	}

	public void setupBuffer(GLContext glContext) {

		this.matrixBuffer = glContext.genBuffer();

		glBindBuffer(GL46.GL_UNIFORM_BUFFER, matrixBuffer);
		glBufferData(
			GL_UNIFORM_BUFFER,
			16 * 4 * 3,
			ShaderProgram.BufferUsage.DynamicDraw.getGLID());

		//binds the buffer to a binding index
		glBindBufferBase(GL_UNIFORM_BUFFER, boundTo, matrixBuffer);

	}

	public static void bindToProgram(int program, int bindingPoint) {
		glUseProgram(program);
		int actualIndex = GL46.glGetUniformBlockIndex(program, "Matrices");
		//binds the binding index to the interface block (by index)
		glUniformBlockBinding(program, actualIndex, VPMatrix.Default_VP_Matrix_Binding_Point);
	}

	public void setPerspective(float fov, float aspectRatio, float near, float far) {
		projectionMatrix
			.setPerspective(
				(float) Math.toRadians(fov),
				(float) aspectRatio,
				near,
				far);
		//NOTE invert either X or Y axis for my prefered coord system
		projectionMatrix.scale(-1, 1, 1);
		

	}

	public void activeCamera(CameraKey camera) {
		this.activeCamera = camera.keyIndex;
	}
	public Matrix4f getVPMatrix() {
		return viewProjectionMatrix;
	}

	public void upload() {
		projectionMatrix.mul(cameras.get(activeCamera), viewProjectionMatrix);
		writeVPFMatrix(matrixBuffer, projectionMatrix, cameras.get(activeCamera), viewProjectionMatrix);
	}

	private static void writeVPFMatrix(int buffer, Matrix4f projection, Matrix4f view, Matrix4f vp) {

		//no need to be in a program bidning, since this is shared between multiple programs
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
		float[] store = new float[16];

		if (projection != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 0, projection.get(store));
		if (view != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 1, view.get(store));
		if (vp != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 2, vp.get(store));

		glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);

	}

	public CameraKey newCamera() {
		CameraKey key = new CameraKey(cameras.size());
		cameras.add(new Matrix4f());
		return key;
	}
}
