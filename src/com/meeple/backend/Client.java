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

/**
 * This class represents the GLFW window, and holds the Nuklear and OpenGL instances. As well as user input, user options, delta calculation per frame, and other useful helper class wrappers.
 * <br>  
 * To use, create an instance of this class, call {@link #setup(int, int, String)} and then {@link #show()}. Almost all method can be overridden but are not required to be for a functional client. 
 * <br>
 * It is recommended to use this in a resource try-catch block in case any errors crop up and all the cleanup is handled automatically. 
 * @author Megan
 *
 */
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
	/**
	 * This is the ID generated and used by the entire GLFW library
	 */
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
	 * This holds the delta between frames, all values contained are calculated per frame too so they are accurate on each use. 
	 */
	public final FrameTimings currentFrameDelta = new FrameTimings();

	/**
	 * GL context used by this client
	 */
	public final GLContext glContext = new GLContext();
	
	/**
	 * Nuklear context wrapper used by this client
	 */
	public final NkContextSingleton nkContext = new NkContextSingleton();
	
	/**
	 * GLFW window callback's used by this client
	 */
	public final MirroredWindowCallbacks callbacks = new MirroredWindowCallbacks();
	
	/**
	 * Small helper class for all user input built on top of the GLFW interfaces and callback's
	 */
	public final UserInput userInput = new UserInput();
	
	/**
	 * Class that holds the users preferences/option-choices of the client
	 */
	public final ClientOptions options = new ClientOptions(userInput);
	
	/**
	 * Executor service to run async tasks. this is initialised with {@link Executors#newCachedThreadPool()}
	 */
	public final ExecutorService service = Executors.newCachedThreadPool();
	
	/**
	 * Uncaught exception handler for the client threads, initialised with a default handler. 
	 * 
	 */
	public UncaughtExceptionHandler exceptionHandler = defaultHandler;
	
	/**
	 * Once the {@link #show()} is called, this is set to {@link Thread#currentThread()} to allow interrupting 
	 */
	private Thread loopThread;
	
	/**
	 * A private flag to check whether or not the window has been set up already or not
	 */
	private boolean hasSetup = false;
	
	/**
	 * This stores the closed state of the window. this is only changed once normally 
	 */
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

	/**
	 * Sets up the client by doing the following in order
	 * <ol>
	 * <li>Sets the thread's exception handler to the one stored in this class</li>
	 * <li>Sets the GLFW error callback to {@link System#err}</li>
	 * <li>Sets up the GLFW Context and window</li>
	 * <li>Initialises the {@link GLContext} class, which creates a GL context, and is stored in {@link #glContext}</li>
	 * <li>Initialises and sets up the {@link NkContextSingleton} class, stored in {@link #nkContext}</li>
	 * <li>Binds the GLFW instance to the generated OpenGL context from {@link #glContext}</li>
	 * </ol>
	 *  
	 * @param width The desired width of the window
	 * @param height The desired height of the window
	 * @param title The title of the window
	 */
	public final void setup(int width, int height, String title) {
		Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);
		glInit(width, height, title);
		GL.setCapabilities(glContext.capabilities);
		setupGL();
		hasSetup = true;
	}

	/**
	 * Requests the client closes if true. Calling with false will only work if the window is still open and running its render loop. 
	 * <br>
	 * Sending false will only work if true has been called in the <b>SAME</b> tick.
	 * cannot cancel attempts to close from previous ticks <br>
	 * By default this will use the
	 * {@link GLFW#glfwSetWindowShouldClose(long, boolean) glfwSetWindowShouldClose} combined with
	 * {@link GLFW#glfwWindowShouldClose(long) glfwWindowShouldClose}
	 * 
	 * @param close - should the client close
	 */
	public void shouldClose(boolean close) {
		GLFW.glfwSetWindowShouldClose(this.windowID, close);
	}

	/**
	 * Returns whether or not the client should close. used internal per tick to
	 * check if should keep running. By default this will use the
	 *  {@link GLFW#glfwSetWindowShouldClose(long, boolean) glfwSetWindowShouldClose} combined with
	 * {@link GLFW#glfwWindowShouldClose(long) glfwWindowShouldClose}
	 * 
	 * @return true if client should close, false for the client to remain open close
	 */
	protected boolean shouldClose() {
		return glfwWindowShouldClose(windowID);
	}

	/**
	 * Called per frame to setup the rendering of the client.<br>
	 * By default this performs the following tasks: 
	 * <ul>
	 * <li>Gets the frame-delta between this and last frame, and stores it in {@link #currentFrameDelta}</li>
	 * <li>Gets the current accurate size of the window and stores them in {@link #windowWidth} and {@link #windowHeight}</li>
	 * <li>Sets the viewport to the full size of the window</li>
	 * <li>Gets the current accurate size of the window's frame buffers and stores them in {@link #fbWidth} and {@link #fbHeight}</li>
	 * <li>Handles Nuklear (through {@link NkContextSingleton#handleInput(long) nkContext.handleInpu} and window input (through {@link UserInput#tick(FrameTimings, long) userInput.tick}</li>
	 * </ul>
	 * Overriding this method will stop these functions happening automatically. 
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
	 * Called at the end of a frame to finished the rendering of the client<br>
	 * By default this performs the following tasks:
	 * <ul>
	 * <li>Renders the Nuklear context with {@link NkContextSingleton#render(float, float, float, float) nkContext.render}</li>
	 * <li>Swaps the frame buffer with {@link GLFW#glfwSwapBuffers(long)}</li>
	 * <li>Pauses the thread to achieve the desired framerate (if neccessary) with {@link FrameTimeManager#run() timingManager.run}</li>
	 * </ul>
	 * Overriding this method will stop these functions happening automatically. 
	 */
	protected void endFrame() {
		nkContext.render(fbWidth, fbHeight, windowWidth, windowHeight);
		glfwSwapBuffers(windowID);
		timingManager.run();
	}

	/**
	 * Called when the client is about to close, just before the cleanup of contexts.
	 */
	protected void onClose() {

	}

	/**
	 * Per frame rendering of the client
	 * 
	 * @param delta The delta difference of the last frame and the current frame pre-calculated
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
		if(!hasSetup) {
			throw new RuntimeException("Client has not been set up and cannot be shown.");
		}
		logger.trace("Window to begin rendering loop (" + (shouldClose()+")"));
		loopThread = Thread.currentThread();

		Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);

		GL.setCapabilities(glContext.capabilities);
		while (!shouldClose()) {
			startFrame();
			render(currentFrameDelta);
			endFrame();
		}
		logger.trace("Rendering loop has ended, moving on to closing");
		if (hasCLosed.compareAndSet(false, true)) {
			close();

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

	/**
	 * This method attempts to stop the window application by calling {@link #shouldClose(boolean) shouldClose(true)}.
	 */
	@Override
	public void close() {
		if (!shouldClose()) {
			shouldClose(true);
		}
	}

	private final void cleanup() {

		logger.trace("Clenaing up the contexts before closing");
		glContext.close();
		nkContext.close();
		logger.trace("Closing window");
		glfwFreeCallbacks(windowID);
		glfwDestroyWindow(windowID);
		logger.trace("Terminating OpenGL");
		glfwTerminate();
		glfwSetErrorCallback(null).free();
		hasSetup=false;
	}

}
