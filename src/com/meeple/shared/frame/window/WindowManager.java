package com.meeple.shared.frame.window;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

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

	public void create(Window window) {
		internalCreate(window, internalActiveWindows);
	}

	private static void internalCreate(Window window, ActiveWindowsComponent active) {
		if (!window.created) {
			logger.trace("Creating new window '" + window.title + "'");

			// Configure GLFW
			GLFW.glfwDefaultWindowHints(); // optional, the current window hints are already the default
			window.hints.process();
			if (window.bounds.width == null || window.bounds.width < 0 || window.bounds.height == null || window.bounds.height < 0) {

				// Get the resolution of the primary monitor
				GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
				if (window.bounds.width == null || window.bounds.width < 0) {
					window.bounds.width = (long) vidmode.width();
				}
				if (window.bounds.height == null || window.bounds.height < 0) {
					window.bounds.height = (long) vidmode.width();
				}
			}

			window.windowID = GLFW.glfwCreateWindow(window.bounds.width.intValue(), window.bounds.height.intValue(), window.title, window.monitor, window.share);
			if (window.windowID == MemoryUtil.NULL) {
				throw new RuntimeException("Failed to create the GLFW window");
			}

			try (MemoryStack stack = stackPush()) {

				//only do this if the posXY are null, 
				if (window.bounds.posX == null || window.bounds.posY == null || window.bounds.posX < 0 || window.bounds.posY < 0) {

					// Get the resolution of the primary monitor
					GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
					if (window.bounds.posX == null || window.bounds.posX <= 0) {
						window.bounds.posX = (vidmode.width() - window.bounds.width) / 2;
					}
					if (window.bounds.posY == null || window.bounds.posY <= 0) {
						window.bounds.posY = (vidmode.height() - window.bounds.height) / 2;
					}

				}
				// position the window
				glfwSetWindowPos(window.windowID, window.bounds.posX.intValue(), window.bounds.posY.intValue());
			}

			window.frameBufferSizeX = window.bounds.width.intValue();
			window.frameBufferSizeY = window.bounds.height.intValue();
			new WindowCallbackManager().setWindowCallbacks(window.windowID, window.callbacks);

			window.created = true;
		}
		active.windows.add(window);
	}

	/*
		*//**
			* Creates a thread for the window and assigns it. <br>
			* Returns a new thread that handles the window rendering. 
			* (This function also assigns the window.loopThread so the thread can be retrieved from either there or this function);
			* @param window
			* @return
			*//*
				@Deprecated
				private Thread setupWindowThread(Window window, FrameTimeComponent frameTimeManager) {
				if (!window.created) {
					System.out.println("Window has not been created, call create(Window) before calling this method.");
					throw new RuntimeException(new WindowNotCreatedException(window));
				}
				
				Thread t = new Thread(new Runnable() {
				
					@Override
					public void run() {
						System.out.println("Starting new thread for window '" + window.title + "'");
				
						FrameUtils.iterateRunnable(window.events.preCreation, false);
						System.out.println(window.windowID);
						glfwMakeContextCurrent(window.windowID);
						window.capabilities = GL.createCapabilities();
						glfwSwapInterval(window.vSync ? 1 : 0);
						FrameUtils.iterateRunnable(window.events.postCreation, false);
				
						GL46.glDebugMessageCallback(window.callbacks.debugMessageCallback, window.windowID);
				
						while (!window.shouldClose && !window.loopThread.isInterrupted()) {
							FrameUtils.iterateRunnable(window.events.frameStart, false);
							glClearColor(window.clearColour.x, window.clearColour.y, window.clearColour.z, window.clearColour.w);
							FrameUtils.iterateRunnable(window.events.preClear, false);
							glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				
							// do rendering
				
							FrameUtils.iterateBiConsumer(window.events.render, null, null, false);
							try (MemoryStack stack = stackPush()) {
								glViewport(0, 0, window.frameBufferSizeX, window.frameBufferSizeY);
							}
				
							glfwSwapBuffers(window.windowID);
							FrameUtils.iterateRunnable(window.events.frameEnd, false);
							if (frameTimeManager != null) {
								frameTimeManager.timeManagement.run();
							}
						}
						FrameUtils.iterateRunnable(window.events.preCleanup, false);
						window.hasClosed = true;
					}
				});
				
				window.loopThread = t;
				return t;
				}*/

	//
	//	/**
	//	 * This function blocks the current thread until either the primary window is closed, or all active windows are closed.<br>
	//	 * Handles the event polling, creates new window threads if a new window is added and cleans up afterwards (with {@link #closeAll(ActiveWindowsComponent)}<br>
	//	 * You do not need to call {@link #setupWindowThread(Window)} yourself, but it does allow better control over multi-threading
	//	 * @param active The Component that holds all active windows
	//	 */
	//	public void startEventsLoop() {
	//		Thread t = generateEventThread(null, new FrameTimeComponent(), null);
	//		t.start();
	//		try {
	//			t.join();
	//		} catch (InterruptedException err) {
	//			// TODO Auto-generated catch block
	//			err.printStackTrace();
	//		}
	//	}
	//
	//	/**
	//	 * This function blocks the current thread until either the primary window is closed, or all active windows are closed.<br>
	//	 * Handles the event polling, creates new window threads if a new window is added and cleans up afterwards (with {@link #closeAll(ActiveWindowsComponent)}<br>
	//	 * You do not need to call {@link #setupWindowThread(Window)} yourself, but it does allow better control over multi-threading
	//	 * @param active The Component that holds all active windows
	//	 */
	//	public void startEventsLoop(Runnable eventsHandling) {
	//		Thread t = generateEventThread(eventsHandling, new FrameTimeComponent(), null);
	//		t.start();
	//		try {
	//			t.join();
	//		} catch (InterruptedException err) {
	//			// TODO Auto-generated catch block
	//			err.printStackTrace();
	//		}
	//	}
	//
	//	/**
	//	 * This function blocks the current thread until either the primary window is closed, or all active windows are closed.<br>
	//	 * Handles the event polling, creates new window threads if a new window is added and cleans up afterwards (with {@link #closeAll(ActiveWindowsComponent)}<br>
	//	 * You do not need to call {@link #setupWindowThread(Window)} yourself, but it does allow better control over multi-threading
	//	 * @param active The Component that holds all active windows
	//	 */
	//	public void startEventsLoop(Runnable eventsHandling, FrameTimeComponent frameTime) {
	//		Thread t = generateEventThread(eventsHandling, frameTime, null);
	//		t.start();
	//		try {
	//			t.join();
	//		} catch (InterruptedException err) {
	//			// TODO Auto-generated catch block
	//			err.printStackTrace();
	//		}
	//	}

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
		logger.debug("Closing window '" + window.title + "'");
		Callbacks.glfwFreeCallbacks(window.windowID);
		GLFW.glfwDestroyWindow(window.windowID);
		FrameUtils.iterateRunnable(window.events.postCleanup, false);
		window.hasClosed = true;
	}
}
