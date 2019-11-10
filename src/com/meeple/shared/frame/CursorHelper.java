package com.meeple.shared.frame;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem.ProjectionMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.VPMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.ViewMatrix;
import com.meeple.shared.frame.window.Window;

public class CursorHelper {

	public static enum SpaceState {
		Viewport_Space,
		Normalised_Device_Space,
		Homogenous_Clip_Space,
		Eye_Space,
		World_Space,
		Local_Space;

	}

	public static Vector4f getMouse(SpaceState result, Window window, ProjectionMatrix proj, ViewMatrix view) {
		Vector4f ret = new Vector4f();
		if (result == null) {
			//early escape if no work is requested
			return ret;
		}
		if (true) {
			//start at beginning
			double[] xposArrD = new double[1], yposArrD = new double[1];
			GLFW.glfwGetCursorPos(window.windowID, xposArrD, yposArrD);
			ret.x = (float) xposArrD[0];
			ret.y = (float) yposArrD[0];
		}
		//return if this is requested result
		if (result == SpaceState.Viewport_Space) {
			return ret;
		}
		//normalised device space
		if (true) {
			ret.x = (2f * ret.x) / window.frameBufferSizeX - 1f;
			ret.y = (2f * ret.y) / window.frameBufferSizeY - 1f;
		}
		if (result == SpaceState.Normalised_Device_Space) {
			return ret;
		}
		if (true) {
			ret.y = -ret.y;
			ret.z = -1f;
			ret.w = 1f;
		}
		if (result == SpaceState.Homogenous_Clip_Space) {
			return ret;
		}
		if (true) {
			Matrix4f invertedProj = proj.cache.invert(new Matrix4f());
			ret = ret.mul(invertedProj);
			ret.z = -1f;
			ret.w = 0f;
		}
		if (result == SpaceState.Eye_Space) {
			return ret;
		}
		if (true) {
			Matrix4f invertedView = view.cache.invert(new Matrix4f());
			ret = invertedView.transform(ret);
			ret.w = 0;
		}
		return ret;
	}

	public static Vector4f getMouse(SpaceState start, Vector4f startVec, SpaceState result, Window window, ProjectionMatrix proj, ViewMatrix view) {
		Vector4f ret = startVec;
		if (result == null) {
			//early escape if no work is requested
			return ret;
		}
		if (start.ordinal() < 0) {
			//start at beginning
			double[] xposArrD = new double[1], yposArrD = new double[1];
			GLFW.glfwGetCursorPos(window.windowID, xposArrD, yposArrD);
			ret.x = (float) xposArrD[0];
			ret.y = (float) yposArrD[0];
		}
		//return if this is requested result
		if (result == SpaceState.Viewport_Space) {
			return ret;
		}
		//normalised device space
		if (start.ordinal() < 1) {
			ret.x = (2f * ret.x) / window.frameBufferSizeX - 1f;
			ret.y = (2f * ret.y) / window.frameBufferSizeY - 1f;
		}
		if (result == SpaceState.Normalised_Device_Space) {
			return ret;
		}
		if (start.ordinal() < 2) {
			ret.y = -ret.y;
			ret.z = -1f;
			ret.w = 1f;
		}
		if (result == SpaceState.Homogenous_Clip_Space) {
			return ret;
		}
		if (start.ordinal() < 3) {
			Matrix4f invertedProj = proj.cache.invert(new Matrix4f());
			ret = ret.mul(invertedProj);
			ret.z = -1f;
			ret.w = 0f;
		}
		if (result == SpaceState.Eye_Space) {
			return ret;
		}
		if (start.ordinal() < 4) {
			Matrix4f invertedView = view.cache.invert(new Matrix4f());
			ret = invertedView.transform(ret);
			ret.w = 0;
		}
		return ret;
	}

	public static Vector2f mouseViewport(Window window, VPMatrix vp) {

		double[] xposArrD = new double[1], yposArrD = new double[1];
		GLFW.glfwGetCursorPos(window.windowID, xposArrD, yposArrD);
		Vector2f mouseV = new Vector2f((float) xposArrD[0], (float) yposArrD[0]);
		//		Vector2f mouseND = new Vector2f((2f * mouseV.x) / window.frameBufferSizeX - 1f, (2f * mouseV.y) / window.frameBufferSizeY- 1f);
		return mouseV;

	}

	public Vector2f mouseNormalisedDevice(Window window, Vector2f cursorPos) {
		return new Vector2f((2f * cursorPos.x) / window.frameBufferSizeX - 1f, (2f * cursorPos.y) / window.frameBufferSizeY - 1f);
	}

	public Vector4f mouseHomogenousClip(Vector2f normalisedDevice) {
		return new Vector4f(normalisedDevice.x, -normalisedDevice.y, -1f, 1f);
	}

	public Vector4f mouseEye(VPMatrix vp, Vector4f mouseHomogenousClip) {
		Matrix4f invertedProj = vp.proj.getWrapped().cache.invert(new Matrix4f());
		Vector4f eye = invertedProj.transform(mouseHomogenousClip);
		eye.z = -1f;
		eye.w = 0f;
		return eye;
	}

	public Vector3f mouseWorld(VPMatrix vp, Vector4f mouseEye) {
		Matrix4f invertedView = vp.view.getWrapped().cache.invert(new Matrix4f());
		Vector4f ray = invertedView.transform(mouseEye);
		Vector3f world = new Vector3f(ray.x, ray.y, ray.z);
		return world;
	}

	public Vector3f mouseWorld(Window window, VPMatrix vp) {

		double[] xposArrD = new double[1], yposArrD = new double[1];
		GLFW.glfwGetCursorPos(window.windowID, xposArrD, yposArrD);
		int[] xposArrI = new int[1], yposArrI = new int[1];
		GLFW.glfwGetFramebufferSize(window.windowID, xposArrI, yposArrI);

		Vector2f mouseV = new Vector2f((float) xposArrD[0], (float) yposArrD[0]);
		Vector2f mouseND = new Vector2f((2f * mouseV.x) / xposArrI[0] - 1f, (2f * mouseV.y) / yposArrI[0] - 1f);
		Vector4f mouseHC = new Vector4f(mouseND.x, -mouseND.y, -1f, 1f);
		Matrix4f invertedProj = vp.proj.getWrapped().cache.invert(new Matrix4f());
		Vector4f eye = invertedProj.transform(mouseHC);
		eye.z = -1f;
		eye.w = 0f;

		Matrix4f invertedView = vp.view.getWrapped().cache.invert(new Matrix4f());
		Vector4f ray = invertedView.transform(eye);
		Vector3f world = new Vector3f(ray.x, ray.y, ray.z);
		return world;
	}

}
