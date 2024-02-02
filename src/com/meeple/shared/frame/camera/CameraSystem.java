package com.meeple.shared.frame.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.meeple.shared.frame.camera.Camera.CameraMode;

public class CameraSystem {

	public void update(Camera camera, Matrix4f writeTo) {
		writeTo.identity();
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
			// these are provided as XZY and need to be swizzled
			writeTo.lookAt(actual.x, actual.z, actual.y, pos.x, pos.z, pos.y, 0, 0, 1f);
		} else {
			writeTo.rotate((float) Math.toRadians(90), 1, 0, 0);
			writeTo.rotate((float) Math.toRadians(camera.rotation.x), 1, 0, 0);
			writeTo.rotate((float) Math.toRadians(camera.rotation.y), 0, 1, 0);
			writeTo.rotate((float) Math.toRadians(-90), 1, 0, 0);

			Vector3f negativeCameraPos = new Vector3f(-camera.position.x, -camera.position.y, -camera.position.z);
			writeTo.translate(negativeCameraPos);
		}

	}

}
