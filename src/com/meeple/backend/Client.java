package com.meeple.backend;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import com.meeple.ClientOptions;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.component.FrameTimeManager;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.window.MirroredWindowCallbacks;
import com.meeple.shared.frame.window.UserInput;

public abstract class Client implements AutoCloseable {
	private static Logger logger = Logger.getLogger(Client.class);

	private static UncaughtExceptionHandler defaultHandler = new UncaughtExceptionHandler() {

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			logger.error("ERROR. ");
			String message = "Thread '" + t.getName() + "' encountered an uncaught error\r\n";
			message += e.getMessage() + "\r\n";
			for (StackTraceElement b : e.getStackTrace()) {

				message += "\tat " + b + " \r\n";

				// message += "\t" + b + "\r\n";
			}

			// Print cause, if any
			Throwable ourCause = e.getCause();

			if (ourCause != null && ourCause != e && !(ourCause.getMessage().contains(e.getLocalizedMessage()) || e.getMessage().contains(ourCause.getLocalizedMessage()))) {

				message += "Caused by: " + ourCause.getMessage() + " \r\n";
				for (StackTraceElement b : ourCause.getStackTrace()) {
					message += "\tat " + b + " \r\n";

				}
			}
			logger.error(message);

			JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);

		}
	};
	public long windowID;
	/**
	 * These are the actual values currently being used. changing these does not
	 * affect window/frame buffer sizes<br>
	 * To do so use the direct GLFW function calls
	 */
	public int windowWidth, windowHeight, fbWidth, fbHeight;

	/**
	 * Frame time manager that tries to control the delta of each frame
	 */
	public final FrameTimeManager timingManager = new FrameTimeManager();
	/**
	 * 
	 */
	public final FrameTimings currentFrameDelta = new FrameTimings();

	/**
	 * GL context used by this client
	 */
	public final GLContext glContext = new GLContext();
	/**
	 * Nuklear context used by this client
	 */
	public final NkContextSingleton nkContext = new NkContextSingleton();
	/**
	 * GLFW window callbacks used by this client
	 */
	public final MirroredWindowCallbacks callbacks = new MirroredWindowCallbacks();
	/**
	 * Small helper class for all user input built ontop of the GLFW interfaces and
	 * callbacks
	 */
	public final UserInput userInput = new UserInput();
	/**
	 * Class that holds the users preferences/option-choices of the client
	 */
	public final ClientOptions options = new ClientOptions(userInput);
	public final ExecutorService service = Executors.newCachedThreadPool();
	/**
	 * Uncaught exception handler for the client threads
	 * 
	 */
	private UncaughtExceptionHandler exceptionHandler = defaultHandler;
	private Thread loopThread;
	private boolean hasSetup = false;
	private AtomicBoolean hasCLosed = new AtomicBoolean(false);

	private final void glInit(int width, int height, String title) {

		GLFWErrorCallback.createPrint(System.err).set();

		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		windowID = glContext.generateWindow(width, height, title);

		callbacks.bindToWindow(windowID);
		userInput.bind(windowID);
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1);
			IntBuffer pHeight = stack.mallocInt(1);

			glfwGetWindowSize(windowID, pWidth, pHeight);

			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			glfwSetWindowPos(windowID, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
		}

		glfwMakeContextCurrent(windowID);
		// Enable v-sync
		glfwSwapInterval(1);

		glfwShowWindow(windowID);
		glContext.init();
		nkContext.setup(windowID, userInput);
	}

	public final void setup(int width, int height, String title) {
		Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);
		glInit(width, height, title);
		GL.setCapabilities(glContext.capabilities);
		setupGL();
		hasSetup = true;
	}

	/**
	 * Requests the client closes if true. or cancels the call this tick if false.
	 * <br>
	 * Sending false will only work if true has been called in the <b>SAME</b> tick.
	 * cannot cancel attempts to close from previous ticks <br>
	 * By default this will use the
	 * {@link GLFW#glfwSetWindowShouldClose(long, boolean)} combined with
	 * {@link GLFW#glfwWindowShouldClose(long)}
	 * 
	 * @param close - should the client close
	 */
	public void shouldClose(boolean close) {
		GLFW.glfwSetWindowShouldClose(this.windowID, close);
	}

	/**
	 * Returns whether or not the client should close. used internal per tick to
	 * check if should keep running. By default this will use the
	 * {@link GLFW#glfwSetWindowShouldClose(long, boolean)} combined with
	 * {@link GLFW#glfwWindowShouldClose(long)}
	 * 
	 * @return true if client *should* close, false to keep running
	 */
	protected boolean shouldClose() {
		return glfwWindowShouldClose(windowID);
	}

	/**
	 * Called per frame to setup the rendering of the client
	 */
	protected void startFrame() {
		if (!hasSetup) {
			throw new RuntimeException("Window has not beein setup. cannot start.");
		}
		currentFrameDelta.tick();
		// NOTE get the latest accurate window/frame buffer sizes
		try (MemoryStack stack = stackPush()) {
			IntBuffer w = stack.mallocInt(1);
			IntBuffer h = stack.mallocInt(1);

			glfwGetWindowSize(windowID, w, h);
			windowWidth = w.get(0);
			windowHeight = h.get(0);
			glViewport(0, 0, windowWidth, windowHeight);

			glfwGetFramebufferSize(windowID, w, h);
			fbWidth = w.get(0);
			fbHeight = h.get(0);
		}

		// NOTE handle the input updates
		nkContext.handleInput(windowID);
		userInput.tick(currentFrameDelta, windowID);
	}

	/**
	 * Called at the end of a frame to finished the rendering of the client
	 */
	protected void endFrame() {
		nkContext.render(fbWidth, fbHeight, windowWidth, windowHeight);
		glfwSwapBuffers(windowID);
		timingManager.run();
	}

	/**
	 * Called when the client is about to close, just before the cleanup of
	 * contexts.
	 */
	protected void onClose() {

	}

	/**
	 * Per frame rendering of the client
	 * 
	 * @param delta - delta difference of the last frame and the current frame
	 */
	protected abstract void render(FrameTimings delta);

	/**
	 * customise contexts in this stage. build in contexts have been constructed.
	 */
	protected abstract void setupGL();

	/**
	 * This shows the window and starts its main loop. <b><I>This will block the
	 * current thread.</i></b>
	 */
	public final void show() {
		loopThread = Thread.currentThread();

		Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);

		GL.setCapabilities(glContext.capabilities);
		while (!shouldClose()) {
			startFrame();
			render(currentFrameDelta);
			endFrame();
		}
		logger.trace("end of loop");

		if (hasCLosed.compareAndSet(false, true)) {
			logger.trace("closing");
			if (!shouldClose()) {
				shouldClose(true);
			}
			if (loopThread != Thread.currentThread()) {
				try {
					loopThread.join();
				} catch (InterruptedException err) {
					err.printStackTrace();
				}
			}

			GL.setCapabilities(glContext.capabilities);
			onClose();
			cleanup();

		}
	}

	@Override
	public final void close() {
		logger.info("TODO: impliment actual close method");
	}

	private final void cleanup() {
		glContext.close();
		nkContext.close();

		glfwFreeCallbacks(windowID);
		glfwDestroyWindow(windowID);

		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

}
