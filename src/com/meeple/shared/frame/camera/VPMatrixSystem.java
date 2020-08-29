package com.meeple.shared.frame.camera;

import java.nio.FloatBuffer;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.frame.FrameUtils;

public class VPMatrixSystem  {

	public static class ViewMatrixSystem  {

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

		public void uploadToShader(ViewMatrix camera, Integer uniformID, MemoryStack stack) {

			Matrix4f viewMatrix = new Matrix4f();
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
				viewMatrix.lookAt(actual.x, actual.z, actual.y, pos.x, pos.z, pos.y, 0, 0, 1f);
			} else {
				viewMatrix.rotate((float) Math.toRadians(90), 1, 0, 0);
				viewMatrix.rotate((float) Math.toRadians(camera.rotation.x), 1, 0, 0);
				viewMatrix.rotate((float) Math.toRadians(camera.rotation.y), 0, 1, 0);
				viewMatrix.rotate((float) Math.toRadians(-90), 1, 0, 0);

				Vector3f negativeCameraPos = new Vector3f(-camera.position.x, -camera.position.y, -camera.position.z);
				viewMatrix.translate(negativeCameraPos);
			}

			GL46.glUniformMatrix4fv(uniformID, false,  viewMatrix.get(FloatBuffer.allocate(16)));
			camera.cache.set(viewMatrix);

		}

	}


}
