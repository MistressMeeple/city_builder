package com.meeple.shared.frame;

import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class CursorHelper {

	public static enum SpaceState {
		Viewport_Space,
		Normalised_Device_Space,
		Homogenous_Clip_Space,
		Eye_Space,
		World_Space,
		Local_Space;

	}

	public static Vector4f getMouse(SpaceState result, long windowID, float cursorPosX, float cursorPosY, Matrix4f projectionMatrix, Matrix4f viewMatrix) {
		int[] windowFrameBufferSizeX = new int[1], windowFrameBufferSizeY = new int[1];
		glfwGetFramebufferSize(windowID, windowFrameBufferSizeX, windowFrameBufferSizeY);

		return getMouse(result, (float) cursorPosX, (float) cursorPosY, windowFrameBufferSizeX[0], windowFrameBufferSizeY[0], projectionMatrix, viewMatrix);
	}

	public static Vector4f getMouse(SpaceState result, long windowID, int windowFrameBufferSizeX, int windowFrameBufferSizeY, Matrix4f projectionMatrix, Matrix4f viewMatrix) {
		//start at beginning
		double[] xposArrD = new double[1], yposArrD = new double[1];
		glfwGetCursorPos(windowID, xposArrD, yposArrD);
		return getMouse(result, (float) xposArrD[0], (float) yposArrD[0], windowFrameBufferSizeX, windowFrameBufferSizeY, projectionMatrix, viewMatrix);
	}

	public static Vector4f getMouse(SpaceState result, long windowID, Matrix4f projectionMatrix, Matrix4f viewMatrix) {
			//start at beginning
			double[] xposArrD = new double[1], yposArrD = new double[1];
			glfwGetCursorPos(windowID, xposArrD, yposArrD);

			int[] windowFrameBufferSizeX = new int[1], windowFrameBufferSizeY = new int[1];
			glfwGetFramebufferSize(windowID, windowFrameBufferSizeX, windowFrameBufferSizeY);

			return getMouse(result, (float) xposArrD[0], (float) yposArrD[0], windowFrameBufferSizeX[0], windowFrameBufferSizeY[0], projectionMatrix, viewMatrix);
	}

	public static Vector4f getMouse(SpaceState result, float cursorPosX, float cursorPosY, int windowFrameBufferSizeX, int windowFrameBufferSizeY, Matrix4f projectionMatrix, Matrix4f viewMatrix) {
		Vector4f ret = new Vector4f();
		if (result == null) {
			//early escape if no work is requested
			return ret;
		}
		if (true) {
			//start at beginning
			ret.x = cursorPosX;
			ret.y = cursorPosY;
		}
		//return if this is requested result
		if (result == SpaceState.Viewport_Space) {
			return ret;
		}
		//normalised device space
		if (true) { 
			ret.x = (2f * ret.x) / windowFrameBufferSizeX - 1f;
			ret.y = (2f * ret.y) / windowFrameBufferSizeY - 1f;
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
			Matrix4f invertedProj = projectionMatrix.invert(new Matrix4f());
			ret = ret.mul(invertedProj);
			ret.z = -1f;
			ret.w = 0f;
		}
		if (result == SpaceState.Eye_Space) {
			return ret;
		}
		if (true) {
			Matrix4f invertedView = viewMatrix.invert(new Matrix4f());
			ret = invertedView.transform(ret);
			ret.w = 0;
		}
		return ret;
	}

	/*
	public static Vector4f getMouse(SpaceState start, Vector4f startVec, SpaceState result, Window window, ProjectionMatrix proj, ViewMatrix view) {
		Vector4f ret = startVec;
		if (result == null) {
			//early escape if no work is requested
			return ret;
		}
		if (start.ordinal() < 0) {
			//start at beginning
			double[] xposArrD = new double[1], yposArrD = new double[1];
			GLFW.glfwGetCursorPos(window.getID(), xposArrD, yposArrD);
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
	*/

}
