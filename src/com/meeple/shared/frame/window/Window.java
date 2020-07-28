package com.meeple.shared.frame.window;

import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.io.Closeable;

import org.apache.log4j.Logger;
import org.joml.Vector4f;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.component.Bounds2DComponent;

public class Window implements Closeable {

	private static Logger logger = Logger.getLogger(Window.class);
	public long windowID = 0;
	public String name = "Default Title";
	public Long monitor = MemoryUtil.NULL;
	public Long share = MemoryUtil.NULL;
	public boolean vSync = true;
	public Thread loopThread ;//= new GLFWThread(this, frameTimeManager, enableDebug, runnables);
	
	//CONTEXTS
	public final GLContext glContext = new GLContext();

	//states
	public boolean created = false;
	public boolean shouldClose = false;
	public boolean hasClosed = false;
	
	//rendering info
	public int clearType = GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT;
	public int frameBufferSizeX;
	public int frameBufferSizeY;

	//final, so can never be null. all these components have methods to set the internal representations
	public final Bounds2DComponent bounds = new Bounds2DComponent();
	public final WindowHints hints = WindowHints.defaultWindowHints;
	public final WindowEvents events = new WindowEvents();
	public final Vector4f clearColour = new Vector4f(0, 0, 0, 0);
	public final MirroredWindowCallbacks callbacks = new MirroredWindowCallbacks();
	/**
	 * Normally which nuklear this has focus 
	 */
	public transient Object currentFocus = null;

	public void create() {

		if (!this.created) {
			GLFW.glfwDefaultWindowHints();
			this.hints.process();
			if (this.bounds.width == null || this.bounds.width < 0 || this.bounds.height == null || this.bounds.height < 0) {

				// Get the resolution of the primary monitor
				GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
				if (this.bounds.width == null || this.bounds.width < 0) {
					this.bounds.width = (long) vidmode.width();
				}
				if (this.bounds.height == null || this.bounds.height < 0) {
					this.bounds.height = (long) vidmode.width();
				}
			}

			this.windowID = GLFW.glfwCreateWindow(this.bounds.width.intValue(), this.bounds.height.intValue(), this.name, this.monitor, this.share);
			if (this.windowID == MemoryUtil.NULL) {
				throw new RuntimeException("Failed to create the GLFW this");
			}

			try (MemoryStack stack = stackPush()) {

				//only do this if the posXY are null, 
				if (this.bounds.posX == null || this.bounds.posY == null || this.bounds.posX < 0 || this.bounds.posY < 0) {

					// Get the resolution of the primary monitor
					GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
					if (this.bounds.posX == null || this.bounds.posX <= 0) {
						this.bounds.posX = (vidmode.width() - this.bounds.width) / 2;
					}
					if (this.bounds.posY == null || this.bounds.posY <= 0) {
						this.bounds.posY = (vidmode.height() - this.bounds.height) / 2;
					}

				}
				// position the this
				glfwSetWindowPos(this.windowID, this.bounds.posX.intValue(), this.bounds.posY.intValue());
			}

			this.frameBufferSizeX = this.bounds.width.intValue();
			this.frameBufferSizeY = this.bounds.height.intValue();
			new WindowCallbackManager().setWindowCallbacks(this.windowID, this.callbacks);

			this.created = true;
		} else {
			logger.warn("Window has already been created");
		}
	}

	public void show() {

	}

	@Override
	public void close() {
		{
			//NOTE should already be true
			if (!shouldClose) {
				logger.warn("Window flag to close was not true");
			}
			shouldClose = true;
		}
		//NOTE stop the thread worker
		{
			Thread thread = loopThread;
			if (thread != null) {
				thread.interrupt();
				try {
					thread.join();
				} catch (Exception e) {
					//interupted 
				}
			}
		}
		//NOTE cleanup all contexts
		{
			glContext.close();
			Callbacks.glfwFreeCallbacks(windowID);
			GLFW.glfwDestroyWindow(windowID);
		}

		FrameUtils.iterateRunnable(events.postCleanup, false);
		hasClosed = true;
	}

}
