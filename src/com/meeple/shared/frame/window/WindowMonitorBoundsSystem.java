package com.meeple.shared.frame.window;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import com.meeple.shared.frame.component.Bounds2DComponent;

public class WindowMonitorBoundsSystem {

	public void setWindowBorderless(long windowID, int monitorIndex) {

		long monitorID = 0;
		int rr = 0;
		try (MemoryStack stack = stackPush()) {
			PointerBuffer monitors = GLFW.glfwGetMonitors();
			monitorID = monitors.get(monitorIndex);
			GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitorID);
			rr = vidmode.refreshRate();
		}
		Bounds2DComponent monitorBounds = getMonitorBounds(monitorID, new Bounds2DComponent());
		try (MemoryStack stack = stackPush()) {
			long m = MemoryUtil.NULL;
			int w = monitorBounds.width.intValue();
			int h = monitorBounds.height.intValue();
			glfwSetWindowMonitor(windowID, m, monitorBounds.posX.intValue(), monitorBounds.posY.intValue(), w, h, rr);
		}
		glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
	}

	public void setWindowFullscreen(long windowID, int monitorIndex) {
		long monitorID = 0;
		int rr = 0;
		try (MemoryStack stack = stackPush()) {
			PointerBuffer monitors = GLFW.glfwGetMonitors();
			monitorID = monitors.get(monitorIndex);
			GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitorID);
			rr = vidmode.refreshRate();
		}
		Bounds2DComponent monitorBounds = getMonitorBounds(monitorID, new Bounds2DComponent());
		try (MemoryStack stack = stackPush()) {
			long m = monitorID;
			int w = monitorBounds.width.intValue();
			int h = monitorBounds.height.intValue();
			glfwSetWindowMonitor(windowID, m, monitorBounds.posX.intValue(), monitorBounds.posY.intValue(), w, h, rr);
		}
	}

	public void setWindowWindowed(long windowID, Bounds2DComponent bounds) {
		int rr = 0;
		try (MemoryStack stack = stackPush()) {
			GLFWVidMode vidmode = GLFW.glfwGetVideoMode(glfwGetPrimaryMonitor());
			rr = vidmode.refreshRate();
		}
		try (MemoryStack stack = stackPush()) {
			long m = MemoryUtil.NULL;

			glfwSetWindowMonitor(windowID, m, bounds.posX.intValue(), bounds.posY.intValue(), bounds.width.intValue(), bounds.height.intValue(), rr);
		}
	}

	public void setWindowBounds(long windowID, Bounds2DComponent bounds) {
		GLFW.glfwSetWindowSize(windowID, bounds.width.intValue(), bounds.height.intValue());
		GLFW.glfwSetWindowPos(windowID, bounds.posX.intValue(), bounds.posY.intValue());
	}

	public Bounds2DComponent getWindowBounds(long windowID) {
		return getWindowBounds(windowID, new Bounds2DComponent());
	}

	public Bounds2DComponent getWindowBounds(long windowID, Bounds2DComponent bounds) {
		try (MemoryStack stack = stackPush()) {
			int[] x = new int[1];
			int[] y = new int[1];
			GLFW.glfwGetWindowSize(windowID, x, y);
			bounds.width = (long) x[0];
			bounds.height = (long) y[0];
			GLFW.glfwGetWindowPos(windowID, x, y);
			bounds.posX = (long) x[0];
			bounds.posY = (long) y[0];
		}
		return bounds;
	}

	private Bounds2DComponent getMonitorBounds(long monitorID, Bounds2DComponent bounds) {

		try (MemoryStack stack = stackPush()) {
			GLFWVidMode vidmode = GLFW.glfwGetVideoMode(monitorID);
			int[] x = new int[1];
			int[] y = new int[1];
			GLFW.glfwGetMonitorPos(monitorID, x, y);
			bounds.width = (long) vidmode.width();
			bounds.height = (long) vidmode.height();
			bounds.posX = (long) x[0];
			bounds.posY = (long) 0;
		}
		return bounds;
	}

	/**
	 * Returns a new instance of Bounds2DComponent from {@link #getMonitorBounds(int, Bounds2DComponent)}
	 * @param monitorIndex
	 * @return
	 */
	public Bounds2DComponent getMonitorBounds(int monitorIndex) {
		return getMonitorBounds(monitorIndex, new Bounds2DComponent());
	}

	/**
	 * Get the monitor position and size, reads into the given bounds and returns. 
	 * @param monitorIndex
	 * @param bounds
	 * @return
	 */
	public Bounds2DComponent getMonitorBounds(int monitorIndex, Bounds2DComponent bounds) {

		long monitor = 0;
		//no idea what this actually means... 
		try (MemoryStack stack = stackPush()) {
			// Get the resolution of the primary monitor
			PointerBuffer monitors = GLFW.glfwGetMonitors();
			monitor = monitors.get(monitorIndex);
		}
		bounds = getMonitorBounds(monitor, bounds);
		return bounds;
	}

	/**
	 * Takes in a bounds purely for width/height. calculates the bounds that will centralise the width/height for the provided monitor
	 * @param monitorIndex Index of the monitor that will be calculated with
	 * @param bounds width/height as part of the calculation, written into and returned 
	 * @return
	 */
	public Bounds2DComponent centerBoundsInMonitor(int monitorIndex, Bounds2DComponent bounds) {

		Bounds2DComponent monitor = getMonitorBounds(monitorIndex);

		if (bounds.posX == null || bounds.posX <= 0) {
			bounds.posX = ((monitor.width - bounds.width) / 2) + monitor.posX;
		}
		if (bounds.posY == null || bounds.posY <= 0) {
			bounds.posY = ((monitor.height - bounds.height) / 2) + monitor.posY;
		}

		return bounds;
	}

}
