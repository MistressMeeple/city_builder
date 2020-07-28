package com.meeple.shared.frame.window;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;

import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.GLFWManager;
import com.meeple.shared.frame.component.FrameTimeManager;
import com.meeple.shared.frame.thread.ThreadCloseManager;
import com.meeple.shared.frame.thread.ThreadManager;

/**
 * <B>**IMPORTANT** You need to create a {@link GLFWManager} before this class can be used</B><br>
 * Window manager is a class that manages windows through the use of an {@link ActiveWindowsComponent} <br>
 * It is recommended that you use this class to manage all of the windows. 
 * @author Megan
 *
 */
public class WindowManager implements AutoCloseable {

	private static Logger logger = Logger.getLogger(GLFWManager.class);
	private ActiveWindowsComponent internalActiveWindows = new ActiveWindowsComponent();

	



	/**
	 * This has to be called after the GLManager has been created 
	 * Creates a thread that can be started at any time that performs all Open GL rendering for all active windows. <br>
	 * Runs the events handler and frame time per frame.<br> 
	 * Any active windows that are closed will be cleaned up automatically, in addition any window added will also be shown and handled.<br>
	 * If any window is handled in its own thread, then this thread only monitors its close status and manages closing, otherwise this thread will handle any rendering and context switching automatically.
	 * @see GLFWManager#GLManager()
	 * @see #setupWindowThread(Window)
	 * @see #setWindowContext(Window)
	 * 
	 *   
	 * @param active Active window component that stores all the currently active windows to render. 
	 * @param eventsHandling
	 * @param frameTime
	 * @return
	 */
	public ThreadManager.Builder generateManagerRunnable(AtomicInteger quit, Runnable eventsHandling, FrameTimeManager frameTime, Window primaryWindow) {

		ThreadCloseManager close = new ThreadCloseManager() {

			@Override
			public boolean check() {
				return !internalActiveWindows.windows.isEmpty() && quit.get() > 0;
			}
		};
		Runnable eventshandler = null;
		boolean usingExternalEventsHandle = eventsHandling != null;

		Runnable wait = null;
		if (!usingExternalEventsHandle) {
			wait = new Runnable() {
				@Override
				public void run() {
					glfwPollEvents();
				}
			};
			eventshandler = wait;
		} else {
			eventshandler = eventsHandling;
		}
		Runnable windowManaging = new Runnable() {

			@Override
			public void run() {

				List<Window> list = internalActiveWindows.windows;

				synchronized (list) {

					Iterator<Window> i = list.iterator();
					while (i.hasNext()) {
						Window w = i.next();

						if (w.shouldClose || glfwWindowShouldClose(w.windowID)) {
							//quit.decrementAndGet();
							WindowManager.closeWindowUnmanaged(w);
							FrameUtils.iterateRunnable(w.events.postCleanup, false);
							i.remove();
						}
						// if the thread is new (hasnt started-died) then we can start it

						/*
						if (w.loopThread != null && w.loopThread.getState() == State.NEW && w.loopThread.getState() != State.TERMINATED) {
							w.loopThread.start();
						}
						*/
					}
				}

			}
		};
		Runnable primaryWindowManaging = new Runnable() {

			@Override
			public void run() {
				if (primaryWindow != null) {
					if (glfwWindowShouldClose(primaryWindow.windowID) || primaryWindow.shouldClose) {
						quit.set(0);
					}
				}

			}
		};

		ThreadManager.Builder builder = new ThreadManager.Builder();

		builder.add(eventshandler);
		builder.add(frameTime);
		builder.add(windowManaging);
		builder.add(primaryWindowManaging);
		builder.setQuit(close);

		return builder;
	}

	public ActiveWindowsComponent getActiveWindows() {
		return internalActiveWindows;
	}
	/*
		private void internalWindowTick(Window window) {
	
			iterateRunnable(window.events.frameStart);
	
			glfwShowWindow(window.windowID);
	
			//resize the openGL viewport to fit the window
			try (MemoryStack stack = stackPush()) {
				IntBuffer width = stack.mallocInt(1);
				IntBuffer height = stack.mallocInt(1);
	
				glfwGetWindowSize(window.windowID, width, height);
				window.bounds.size(width.get(0), height.get(0));
	
				glfwGetFramebufferSize(window.windowID, width, height);
				window.frameBufferSizeX = width.get(0);
				window.frameBufferSizeY = height.get(0);
				glViewport(0, 0, width.get(0), height.get(0));
			}
			iterateRunnable(window.events.preClear);
	
			GL46.glClearColor(window.clearColour.x, window.clearColour.y, window.clearColour.z, window.clearColour.w);
			GL46.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
	
			iterateRunnable(window.events.render);
	
			GLFW.glfwSwapBuffers(window.windowID); // swap the color buffers
	
			boolean wsc = glfwWindowShouldClose(window.windowID);
			boolean interupt = window.loopThread.isInterrupted();
			window.shouldClose = window.shouldClose || wsc || interupt;
			iterateRunnable(window.events.frameEnd);
		}*/

	@Override
	public void close() {
		synchronized (internalActiveWindows) {

			Iterator<Window> i = internalActiveWindows.windows.iterator();
			while (i.hasNext()) {
				Window window = i.next();
				closeWindowUnmanaged(window);
				i.remove();
			}
		}
	}

	public void closeWindow(Window window) {
		closeWindowUnmanaged(window);
		internalActiveWindows.windows.remove(window);

	}

	public static void closeWindowUnmanaged(Window window) {
		logger.trace("Closing window with ID: " + window.windowID);
		window.shouldClose = true;
		Thread thread = window.loopThread;
		if (thread != null) {
			thread.interrupt();
			try {
				thread.join();
			} catch (Exception e) {
				//interupted 
			}
		}
		logger.debug("Closing window '" + window.name+ "'");
		Callbacks.glfwFreeCallbacks(window.windowID);
		GLFW.glfwDestroyWindow(window.windowID);
		FrameUtils.iterateRunnable(window.events.postCleanup, false);
		window.hasClosed = true;
	}
}
