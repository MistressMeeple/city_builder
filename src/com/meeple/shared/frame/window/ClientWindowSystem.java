package com.meeple.shared.frame.window;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nuklear.Nuklear.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;

import com.meeple.citybuild.client.render.Screen;
import com.meeple.shared.ClientOptionSystem;
import com.meeple.shared.ClientOptions;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.Delta;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.GLFWThread;
import com.meeple.shared.frame.OAL.AudioData;
import com.meeple.shared.frame.OGL.KeyInputSystem;
import com.meeple.shared.frame.component.FrameTimeManager;
import com.meeple.shared.frame.input.GLFWCursorType;
import com.meeple.shared.frame.nuklear.IOUtil;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.nuklear.NuklearManager;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.BtnState;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.Menu;
import com.meeple.shared.frame.nuklear.NuklearUIComponent;
import com.meeple.shared.frame.thread.ThreadManager.Builder;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;

public interface ClientWindowSystem {

	/*
	 * class InputEvent {
	 * 
	 * }
	 * 
	 * enum ButtonEventType { Press, Hold, Release }
	 * 
	 * class ButtonEvent extends InputEvent { int glfw_button; ButtonEvent
	 * eventType; long ticks; }
	 */

	static Logger logger = Logger.getLogger(ClientWindowSystem.class);

	public enum WindowEvent {
		/**
		 * Attempts to start the game (from menus)
		 */
		GameStart,
		/**
		 * Attempts to pause the game (from in game)
		 */
		GamePause,
		/**
		 * Attempts to resume to game (from game-pause)
		 */
		GameResume,
		/**
		 * A call to trigger game saving
		 */
		GameSave,
		/**
		 * A call to trigger game loading
		 */
		GameLoad,
		/**
		 * Goes to the main menu from anywhere in the client
		 */
		GoToMainMenu,
		/**
		 * Opens the option screen from any current screen
		 */
		OptionsOpen,
		/**
		 * Closes the options, from the option screen
		 */
		OptionsClose,
		/**
		 * Closes the game from anywhere in the client
		 */
		ClientClose

	}
	
	/**
	 * @deprecated I think?
	 * @author Megan
	 *
	 */
	public static class ClientWindow extends Window {

		public final Map<Integer, Boolean> keyPressMap = new CollectionSuppliers.MapSupplier<Integer, Boolean>().get();
		public final Map<Integer, Long> keyPressTicks = new CollectionSuppliers.MapSupplier<Integer, Long>().get();
		public final Map<Integer, Boolean> mousePressMap = new CollectionSuppliers.MapSupplier<Integer, Boolean>().get();
		public final Map<Integer, Long> mousePressTicks = new CollectionSuppliers.MapSupplier<Integer, Long>().get();
		// KeyInputSystem keyInput = new KeyInputSystem();
		// NkContextSingleton nkContext = new NkContextSingleton();
		// CustomMenuSystem menuSystem = new CustomMenuSystem();
		public Map<String, NuklearUIComponent> registeredNuklear = new CollectionSuppliers.MapSupplier<String, NuklearUIComponent>().get();
		public List<NuklearUIComponent> menuQueue = new CollectionSuppliers.ListSupplier<NuklearUIComponent>().get();
		public final FrameTimeManager eventTimeManager = new FrameTimeManager();
		public final FrameTimeManager renderTimeManager = new FrameTimeManager();
		public final NkContextSingleton nkContext = new NkContextSingleton();
		public final Screen render = new Screen() {
			@Override
			public void render(ClientWindow window, Delta delta) {

			}
		};

		public final ClientOptions clientOptions = new ClientOptions();

		private final Wrapper<GLFWCursorType> currentCursorType = new WrapperImpl<>();
		private final Wrapper<GLFWCursorType> queueChangeCursorType = new WrapperImpl<>();

		public void setCursorType(GLFWCursorType cursor) {
			this.queueChangeCursorType.setWrapped(cursor);
		}

		public Set<BiConsumer<WindowEvent, Object>> eventListeners = new CollectionSuppliers.SetSupplier<BiConsumer<WindowEvent, Object>>().get();

		public void sendEvent(WindowEvent eventType) {
			sendEvent(eventType, null);
		}

		public void sendEvent(WindowEvent eventType, Object param) {
			logger.trace("Recieving event: " + eventType);
			FrameUtils.iterateBiConsumer(eventListeners, eventType, param, false);
		}

		public boolean hasAudio = false;
		public long audioDevice;
		public final Set<Integer> audioSources = new CollectionSuppliers.SetSupplier<Integer>().get();

	}

	public static class LevelPreview {
		public int index = 0;
		public String name;
		public boolean playable = true;
		// nkimage
	}

	// shortcut finals:
	static final int MONO = 1, STEREO = 2;

	public static AudioData createBufferData(String file) throws UnsupportedAudioFileException {

		AudioData audio = new AudioData();
		try {
			AudioInputStream stream = null;
			File f = new File(file);
			if (f.exists()) {
				stream = AudioSystem.getAudioInputStream(f);
			}
			if (stream == null) {
				InputStream audioSrc = AudioData.class.getResourceAsStream(file);
				BufferedInputStream bufferedIn = new BufferedInputStream(audioSrc);
				stream = AudioSystem.getAudioInputStream(bufferedIn);

			}
			AudioFormat format = stream.getFormat();
			if (format.isBigEndian())
				throw new UnsupportedAudioFileException("Can't handle Big Endian formats yet");

			// load stream into byte buffer
			int openALFormat = -1;
			switch (format.getChannels()) {
			case MONO:
				switch (format.getSampleSizeInBits()) {
				case 8:
					openALFormat = AL10.AL_FORMAT_MONO8;
					break;
				case 16:
					openALFormat = AL10.AL_FORMAT_MONO16;
					break;
				}
				break;
			case STEREO:
				switch (format.getSampleSizeInBits()) {
				case 8:
					openALFormat = AL10.AL_FORMAT_STEREO8;
					break;
				case 16:
					openALFormat = AL10.AL_FORMAT_STEREO16;
					break;
				}
				break;
			}

			ByteBuffer data = BufferUtils.createByteBuffer(stream.available());
			IOUtil.writeToBuffer(stream, data);
			data.flip();

			int id = AL10.alGenBuffers();
			// load audio data into appropriate system space....
			AL10.alBufferData(id, openALFormat, data, (int) format.getSampleRate());
			logger.trace(file + " using id " + id);

			audio.length = (long) (1000f * stream.getFrameLength() / format.getFrameRate());
			audio.bufferID = id;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return audio;
	}

	public default NuklearUIComponent setupLevelSelectMenu(ClientWindow window, NuklearMenuSystem menuSystem, Wrapper<LevelPreview[]> levelPreviewWrapper, Wrapper<Consumer<LevelPreview>> levelSelect) {

		Wrapper<Integer> levelSelectIndex = new WrapperImpl<>();
		Menu levelDetails = new Menu() {

			@Override
			public boolean spacingBeforeDraw() {
				return false;
			}

			@Override
			public void secondaryClick() {
//				menuSystem.setActiveNuklear(window.menuQueue, null, null);
				levelSelect.getWrapped().accept(levelPreviewWrapper.getWrapped()[levelSelectIndex.getWrapped()]);

			}

			@Override
			public void returnClick() {
//				menuSystem.goBackNuklear(window.menuQueue);

			}

			@Override
			public int height() {
				return 20 * NuklearMenuSystem.buttonHeight;
			}

			@Override
			public BtnState getSecondaryState() {
				return levelSelectIndex.getWrappedOrDefault(-1) != -1 ? BtnState.Visible : BtnState.Disabled;
			}

			@Override
			public String getSecondaryName() {
				return "Load Level";
			}

			@Override
			public BtnState getReturnState() {
				return BtnState.Visible;
			}

			@Override
			public String getReturnName() {
				return "Back";
			}

			@Override
			public int getGroupTags() {
				return 0;
			}

			@Override
			public void draw(NkContext context, MemoryStack stack) {
				NkColor active = context.style().button().active().data().color();
				nk_layout_row_dynamic(context, NuklearMenuSystem.buttonHeight - NuklearMenuSystem.sub, 1);

				int sel = levelSelectIndex.getWrappedOrDefault(-1);
				for (int i = 0; i < levelPreviewWrapper.getWrappedOrDefault(new LevelPreview[0]).length; i++) {

					LevelPreview lp = levelPreviewWrapper.getWrapped()[i];
					if (lp.playable) {
						int id = i;
						if (sel == i) {
							NuklearManager.styledButton(context, active, () ->
							{
								nk_button_label(context, lp.name);
							});
						} else {
							if (nk_button_label(context, lp.name)) {
								levelSelectIndex.setWrapped(id);
							}
						}
					} else {
						NuklearManager.styledButton(context, NuklearMenuSystem.getDisabled(context, stack), () -> nk_button_label(context, "[Locked] " + lp.name));

						if (sel == i) {
							// if disabled cannot be selected
							levelSelectIndex.setWrapped(-1);
						}
					}
				}

			}
		};

		NuklearUIComponent levelSelectMenu = new NuklearUIComponent();

		levelSelectMenu.title = "Level Select";
		levelSelectMenu.open.add(new Runnable() {

			@Override
			public void run() {
				levelSelectIndex.setWrapped(-1);
			}
		});
		menuSystem.setupMenu(window, levelSelectMenu, levelDetails);
		return levelSelectMenu;
	}

	static void setupAudioSystem(ClientWindow window) {
		window.events.postCreation.add(() ->
		{
			window.audioDevice = ALC10.alcOpenDevice((ByteBuffer) null);

			ALCCapabilities deviceCaps = ALC.createCapabilities(window.audioDevice);

			IntBuffer contextAttribList = BufferUtils.createIntBuffer(16);

			// Note the manner in which parameters are provided to OpenAL...
			contextAttribList.put(ALC11.ALC_REFRESH);
			contextAttribList.put(60);

			contextAttribList.put(ALC11.ALC_SYNC);
			contextAttribList.put(ALC11.ALC_FALSE);

			contextAttribList.put(0);
			contextAttribList.flip();

			long newContext = ALC10.alcCreateContext(window.audioDevice, contextAttribList);

			if (!ALC10.alcMakeContextCurrent(newContext)) {
				System.err.println("Failed to make context current");
				return;
			}

			AL.createCapabilities(deviceCaps);

			// define listener
			AL10.alListener3f(AL10.AL_VELOCITY, 0f, 0f, 0f);
			AL10.alListener3f(AL10.AL_ORIENTATION, 0f, 0f, -1f);
			window.hasAudio = true;
		});
		window.events.preCleanup.add(() ->
		{

			for (Integer source : window.audioSources) {
				AL10.alSourceStop(source);
				AL10.alDeleteSources(source);
			}
			ALC10.alcCloseDevice(window.audioDevice);
			ALC.destroy();
		});
	}

	/**
	 * This starts the main thread worker and runs setup up most things.<br>
	 * this will block the current thread.
	 * 
	 * @param windowManager
	 * @param window
	 * @param nkContext
	 * @param quitCountdown
	 * @param service
	 */
	public static void start(WindowManager windowManager, ClientWindow window, AtomicInteger quitCountdown, ExecutorService service) {
		/*
		 * setupAudioSystem(window); closeAudioSystem(window);
		 */
		WindowMonitorBoundsSystem wmbs = new WindowMonitorBoundsSystem();
		wmbs.centerBoundsInMonitor(0, window.bounds);

		window.loopThread = new GLFWThread(window, window.renderTimeManager, true, new Runnable[] {});
		window.events.preCleanup.add(() -> window.sendEvent(WindowEvent.ClientClose));
		Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				System.out.println("ERROR. ");
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
				System.err.println(message);
				quitCountdown.set(0);
				JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);

			}
		});
		window.loopThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				System.out.println("ERROR. ");
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
						message += "\tat " + b.toString() + " \r\n";

					}
				}
				System.err.println(message);
				quitCountdown.decrementAndGet();
				JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);

			}
		});

		window.create();
//		Builder t = windowManager.generateManagerRunnable(quitCountdown, NuklearManager.globalEventsHandler(window.nkContext, windowManager.getActiveWindows()), window.eventTimeManager, window);
		service.execute(() -> window.loopThread.start());
//		t.build().run();
		// service.execute(t.build());
		// window.loopThread.run();

	}

	public static void setupWindow(ClientWindow window, KeyInputSystem keyInput, NkContextSingleton nkContext, ClientOptionSystem clientOptionSystem) {

		clientOptionSystem.readSettingsFile(window.clientOptions);

		WindowHints.debug = true;
		window.name = ("Main Window");
		window.vSync = true;
		WindowHints hints = new WindowHints().setVisible(false).setDoublebuffer(true).setResizable(false);
		window.hints.copyFrom(hints, false);

		window.bounds.set(0, 0, 1280, 800);
		window.hints.setVisible(false);

		window.currentCursorType.setWrapped(GLFWCursorType.Normal);

		window.callbacks.frameBufferSizeCallbackSet.add(new GLFWFramebufferSizeCallbackI() {

			@Override
			public void invoke(long windowID, int width, int height) {
				window.frameBufferSizeX = width;
				window.frameBufferSizeY = height;

			}
		});

		window.events.frameStart.add(() ->
		{
			if (window.queueChangeCursorType.getWrapped() != null) {
				switch (window.queueChangeCursorType.getWrapped()) {
				case Disabled:
					glfwSetInputMode(window.windowID, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
					window.currentCursorType.setWrapped(GLFWCursorType.Disabled);
					break;
				case Hidden:
					glfwSetInputMode(window.windowID, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
					window.currentCursorType.setWrapped(GLFWCursorType.Hidden);
					break;
				case Normal:
					glfwSetInputMode(window.windowID, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
					window.currentCursorType.setWrapped(GLFWCursorType.Normal);
					break;
				default:
					break;

				}
			}
		});

		window.events.postCreation.add(() ->
		{
			logger.debug("Started new window thread: " + Thread.currentThread().getName());
			glfwShowWindow(window.windowID);
		});
		/*
		 * Thread time management settings
		 */
		{
			/**
			 * This is time event polling rate
			 */
			window.eventTimeManager.desiredFrameRate = 60;
			/**
			 * This is the render rate
			 */
			window.renderTimeManager.desiredFrameRate = 60;
		}
		window.vSync = false;
		
		//window.callbacks.keyCallbackSet.add((long windowID, int key, int scancode, int action, int mods) -> keyInput.eventHandleKey(window.keyPressMap, windowID, key, scancode, action, mods));
		//window.callbacks.mouseButtonCallbackSet.add((long windowID, int key, int action, int mods) -> keyInput.eventHandleMouse(window.mousePressMap, windowID, key, action, mods));

	}

}
