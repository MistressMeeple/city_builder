package com.meeple.shared.frame.camera;

import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.frame.OGL.IShaderUniformUploadSystem;
import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem.ProjectionMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.VPMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.ViewMatrix;
import com.meeple.shared.frame.component.Bounds2DComponent;
import com.meeple.shared.frame.window.Window;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;
import com.meeple.shared.utils.FrameUtils;

public class VPMatrixSystem implements IShaderUniformUploadSystem<VPMatrix, Integer[]> {
	public static class VPMatrix {
		public final Wrapper<ViewMatrix> view = new WrapperImpl<>(new ViewMatrix());
		public final Wrapper<ProjectionMatrix> proj = new WrapperImpl<>(new ProjectionMatrix());
		public Matrix4f cache = new Matrix4f();

	}

	public ProjectionMatrixSystem projSystem = new ProjectionMatrixSystem();
	public ViewMatrixSystem viewSystem = new ViewMatrixSystem();

	public void preMult(VPMatrix vp) {
		vp.proj.get().cache.mul(vp.view.get().cache, vp.cache);
	}

	@Override
	public void uploadToShader(VPMatrix upload, Integer[] uniforms, MemoryStack stack) {
		projSystem.uploadToShader(upload.proj.get(), uniforms[1], stack);
		viewSystem.uploadToShader(upload.view.get(), uniforms[2], stack);
		GL46.glUniformMatrix4fv(uniforms[0], false, IShaderUniformUploadSystem.generateMatrix4fBuffer(stack, upload.cache));

	}

	public static class ViewMatrixSystem implements IShaderUniformUploadSystem<ViewMatrix, Integer> {

		public static enum CameraMode {
			Normal,
			LookAt;
		}

		public static class CameraSpringArm {
			/**
			 * Constants that define the max camera angles
			 */
			public static float minDistance = 0.1f, minPitch = -80, maxPitch = 89;

			public Supplier<Vector3f> lookAt;
			private float distance = 10f;
			private float pitch = 0;
			public float yaw = 0f;

			public void addDistance(float toAdd) {
				distance += toAdd;
				distance = Math.max(distance, CameraSpringArm.minDistance);
			}

			public float getDistance() {
				return distance;
			}

			public void addPitch(float toAdd) {
				pitch += toAdd;
				pitch = FrameUtils.getClamped(minPitch, pitch, maxPitch);
			}

			public float getPitch() {
				return pitch;
			}

		}

		public static class ViewMatrix {
			public CameraMode cameraMode = CameraMode.LookAt;
			public final Matrix4f cache = new Matrix4f();
			public final Vector3f position = new Vector3f();
			public final Vector3f rotation = new Vector3f();
			public final CameraSpringArm springArm = new CameraSpringArm();
		}

		public void update(ViewMatrix camera){

			Matrix4f writeTo = new Matrix4f();
			if (camera.cameraMode == CameraMode.LookAt) {

				Vector3f pos = new Vector3f(camera.springArm.lookAt.get());
				pos.set(pos.x, pos.z, pos.y);
				float distance = camera.springArm.distance;
				float pitch = (float) Math.toRadians(camera.springArm.pitch);
				float yaw = (float) Math.toRadians(camera.springArm.yaw + 180);
				Vector3f dir = new Vector3f();
				float cp = (float) (Math.cos(pitch));
				float sy = (float) (Math.sin(yaw));
				float sp = (float) (Math.sin(pitch));
				float cy = (float) (Math.cos(yaw));
				dir.x = cp * sy;
				dir.y = sp;
				dir.z = cp * cy;
				Vector3f corrected = dir.mul(distance, new Vector3f());
				Vector3f actual = pos.add(corrected, new Vector3f());

				camera.position.set(actual.x, actual.z, actual.y);
				//these are provided as XZY and need to be swizzled
				writeTo.lookAt(actual.x, actual.z, actual.y, pos.x, pos.z, pos.y, 0, 0, 1f);
			} else {
				writeTo.rotate((float) Math.toRadians(90), 1, 0, 0);
				writeTo.rotate((float) Math.toRadians(camera.rotation.x), 1, 0, 0);
				writeTo.rotate((float) Math.toRadians(camera.rotation.y), 0, 1, 0);
				writeTo.rotate((float) Math.toRadians(-90), 1, 0, 0);

				Vector3f negativeCameraPos = new Vector3f(-camera.position.x, -camera.position.y, -camera.position.z);
				writeTo.translate(negativeCameraPos);
			}

			camera.cache.set(writeTo);
		}

		@Override
		public void uploadToShader(ViewMatrix camera, Integer uniformID, MemoryStack stack) {
			update(camera);
			GL46.glUniformMatrix4fv(uniformID, false, IShaderUniformUploadSystem.generateMatrix4fBuffer(stack, camera.cache));

		}

	}

	public static class ProjectionMatrixSystem implements IShaderUniformUploadSystem<ProjectionMatrix, Integer> {

		public static class ProjectionMatrix {
			public float FOV;
			public float nearPlane;
			public float farPlane;
			public Window window;
			public final Matrix4f cache = new Matrix4f();
			public boolean perspectiveOrOrtho = false;
			public float orthoAspect = 10f;
			public float scale = 1f;

		}

		public void update(ProjectionMatrix upload){
			
			Matrix4f matrix = new Matrix4f();
			Bounds2DComponent bounds = upload.window.getBounds2DComponent();
			float aspectRatio = (float) bounds.width / (float) bounds.height;
			if (upload.perspectiveOrOrtho) {
				matrix.perspective((float) Math.toRadians(upload.FOV), aspectRatio, upload.nearPlane, upload.farPlane);
			} else {
				float scale = (1f / upload.scale);
				matrix
					.ortho(
						aspectRatio * -upload.orthoAspect * scale,
						aspectRatio * upload.orthoAspect * scale,
						-upload.orthoAspect * scale,
						upload.orthoAspect * scale,
						upload.nearPlane,
						upload.farPlane);
			}
			upload.cache.set(matrix);
		}

		@Override
		public void uploadToShader(ProjectionMatrix upload, Integer uniformID, MemoryStack stack) {
			update(upload);
			GL46.glUniformMatrix4fv(uniformID, false, IShaderUniformUploadSystem.generateMatrix4fBuffer(stack, upload.cache));
		}

	}

}
