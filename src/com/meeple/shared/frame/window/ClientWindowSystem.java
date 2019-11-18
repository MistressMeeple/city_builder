package com.meeple.shared.frame.window;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nuklear.Nuklear.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
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

import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.ClientOptionSystem;
import com.meeple.shared.ClientOptionSystem.Delimeter;
import com.meeple.shared.ClientOptions;
import com.meeple.shared.Tickable;
import com.meeple.shared.frame.GLFWThread;
import com.meeple.shared.frame.OAL.AudioData;
import com.meeple.shared.frame.OGL.KeyInputSystem;
import com.meeple.shared.frame.component.Bounds2DComponent;
import com.meeple.shared.frame.component.FrameTimeManager;
import com.meeple.shared.frame.input.GLFWCursorType;
import com.meeple.shared.frame.nuklear.IOUtil;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.nuklear.NkWindowProperties;
import com.meeple.shared.frame.nuklear.NuklearManager;
import com.meeple.shared.frame.nuklear.NuklearManager.RegisteredGUIS;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.ActiveMenuQueue;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.BtnState;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.Button;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.Menu;
import com.meeple.shared.frame.nuklear.NuklearUIComponent;
import com.meeple.shared.frame.thread.ThreadManager.Builder;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;

public interface ClientWindowSystem {

	static Logger logger = Logger.getLogger(ClientWindowSystem.class);

	public class ClientWindow extends Window {

		public final Map<Integer, Boolean> keyPressMap = new HashMap<>();
		public final Map<Integer, Long> keyPressTicks = new HashMap<>();
		public final Map<Integer, Boolean> mousePressMap = new HashMap<>();
		public final Map<Integer, Long> mousePressTicks = new HashMap<>();
		//		KeyInputSystem keyInput = new KeyInputSystem();
		//		NkContextSingleton nkContext = new NkContextSingleton();
		//		CustomMenuSystem menuSystem = new CustomMenuSystem();
		public RegisteredGUIS registeredNuklear;
		public ActiveMenuQueue menuQueue;
		public final FrameTimeManager eventTimeManager = new FrameTimeManager();
		public final FrameTimeManager renderTimeManager = new FrameTimeManager();

		public final ClientOptions clientOptions = new ClientOptions();

		public final Wrapper<GLFWCursorType> currentCursorType = new WrapperImpl<>();
		public final Wrapper<GLFWCursorType> queueChangeCursorType = new WrapperImpl<>();

		/**
		 * For better control use 	{@link ClientWindowSystem#setWindowState(ClientWindow, WindowState) }
		 */
		public final Wrapper<WindowState> state = new WrapperImpl<>(WindowState.Loading);

		public boolean hasAudio = false;
		public long audioDevice;
		public final Set<Integer> audioSources = new HashSet<>();
	}

	public static class LevelPreview {
		public int index = 0;
		public String name;
		public boolean playable = true;
		//nkimage
	}

	//shortcut finals:
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

			//load stream into byte buffer
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
			//load audio data into appropriate system space....
			AL10.alBufferData(id, openALFormat, data, (int) format.getSampleRate());
			logger.trace(file + " using id " + id);

			audio.length = (long) (1000f * stream.getFrameLength() / format.getFrameRate());
			audio.bufferID = id;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return audio;
	}

	public default void initWindow(ClientWindow window, NuklearMenuSystem menuSystem) {

		window.registeredNuklear = menuSystem.new RegisteredGUIS();
		window.menuQueue = menuSystem.new ActiveMenuQueue();
	}

	/**
	 * setup the shared menu system for my windows
	 *<br>
	 * this does mostly just creates the templates/update variables but there are a lot of variables to fill in
	 * @param window
	 * @param stateRendering
	 * @param levelPreviewWrapper
	 * @param levelSelect
	 * @param clientOptionSystem
	 * @param menuSystem
	 * @param nkContext
	 */
	public default void setupMenu(ClientWindow window, Map<WindowState, Set<Tickable>> stateRendering, ClientOptionSystem clientOptionSystem, NuklearMenuSystem menuSystem,
		NkContextSingleton nkContext, Button... buttons) {
		initWindow(window, menuSystem);
		menuSystem.create(nkContext, window, window.registeredNuklear);
		window.state.setWrapped(WindowState.Menu);

		NuklearUIComponent mainMenu = new NuklearUIComponent();
		NuklearUIComponent pauseMenu = new NuklearUIComponent();
		NuklearUIComponent optionsMenu = new NuklearUIComponent();
		GLFWKeyCallbackI menuNavigation = new GLFWKeyCallbackI() {

			@Override
			public void invoke(long windowID, int key, int scancode, int action, int mods) {
				if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
					switch (window.state.getWrapped()) {
						case Cutscene:
							break;
						case Game_Pause:
							menuSystem.goBackNuklear(window.menuQueue);
							setWindowState(window, WindowState.Game_Running);
							break;
						case Game_Running:

							menuSystem.navigateNuklear(window.registeredNuklear, window.menuQueue, pauseMenu.UUID);
							setWindowState(window, WindowState.Game_Pause);
							break;
						case Loading:

							break;
						case Menu:
							if (NuklearMenuSystem.getActiveMenu(window.menuQueue) != mainMenu) {
								menuSystem.goBackNuklear(window.menuQueue);
							}
							break;
						default:
							break;
					}

				}
			}
		};
		GLFWMouseButtonCallback mouse = new GLFWMouseButtonCallback() {

			@Override
			public void invoke(long windowID, int button, int action, int mods) {
				if (button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_RELEASE) {

					switch (window.state.getWrapped()) {
						case Cutscene:
							break;
						case Game_Pause:
							menuSystem.goBackNuklear(window.menuQueue);
							setWindowState(window, WindowState.Game_Running);
							break;
						case Game_Running:
							break;
						case Loading:

							break;
						case Menu:
							if (NuklearMenuSystem.getActiveMenu(window.menuQueue) != mainMenu) {
								menuSystem.goBackNuklear(window.menuQueue);
							}
							break;
						default:
							break;

					}

				}
			}
		};

		Menu mainMenuDetails = new Menu() {

			@Override
			public boolean spacingBeforeDraw() {
				return true;
			}

			@Override
			public void returnClick() {
				window.shouldClose = true;

			}

			@Override
			public void secondaryClick() {
				menuSystem.navigateNuklear(window.registeredNuklear, window.menuQueue, optionsMenu.UUID);

			}

			@Override
			public int height() {
				return (int) (NuklearMenuSystem.buttonHeight * (0.5f + buttons.length));
			}

			@Override
			public BtnState getReturnState() {
				return BtnState.Visible;
			}

			@Override
			public BtnState getSecondaryState() {
				return BtnState.Visible;
			}

			@Override
			public String getSecondaryName() {
				return "Options";
			}

			@Override
			public String getReturnName() {
				return "Quit";
			}

			@Override
			public void draw(NkContext context, MemoryStack stack) {

				nk_layout_row_dynamic(context, NuklearMenuSystem.buttonHeight - NuklearMenuSystem.sub, 1);
				for (Button b : buttons) {
					if (b.getState() != BtnState.Disabled) {
						if (nk_button_label(context, b.getName())) {
							b.onClick();
						}
					} else {
						menuSystem.styledButton(context, NuklearMenuSystem.getDisabled(context, stack), () -> {
							nk_button_label(context, b.getName());
						});
					}
				}

			}

			@Override
			public int getGroupTags() {
				return 0;
			}

		};
		mainMenu.title = "Main Menu";
		menuSystem.setupMenu(window, mainMenu, mainMenuDetails);
		/*
			Wrapper<NkImage> muteWrapper = new WrapperImpl<>();
			Wrapper<NkImage> non_muteWrapper = new WrapperImpl<>();
		
			Runnable setupButtons = new Runnable() {
		
				@Override
				public void run() {
					System.out.println("setting up image");
					muteWrapper.setWrapped(menuSystem.createImage("res/gui/mute.png"));
					non_muteWrapper.setWrapped(menuSystem.createImage("res/gui/non-mute.png"));
				}
			};
			window.events.postCreation.add(setupButtons);
		*/
		
		Menu optionsMenuDetails = new Menu() {

			@Override
			public boolean spacingBeforeDraw() {
				return false;
			}

			@Override
			public void secondaryClick() {

			}

			@Override
			public void returnClick() {

				menuSystem.goBackNuklear(window.menuQueue);
			}

			@Override
			public int height() {
				return 20 * NuklearMenuSystem.buttonHeight;
			}

			@Override
			public BtnState getSecondaryState() {
				return BtnState.Hidden;
			}

			@Override
			public String getSecondaryName() {
				return null;
			}

			@Override
			public BtnState getReturnState() {
				// TODO Auto-generated method stub
				return BtnState.Visible;
			}

			@Override
			public String getReturnName() {
				return "Back";
			}

			@Override
			public void draw(NkContext context, MemoryStack stack) {

				nk_layout_row_dynamic(context, NuklearMenuSystem.buttonHeight * 5, 1);
				if (nk_group_begin(context, "Sounds", NK_WINDOW_TITLE)) {

					nk_layout_row(context, NK_DYNAMIC, NuklearMenuSystem.buttonHeight, new float[] { 0.2f, 0.5f, 0.15f, 0.15f });
					nk_label(context, "\t Master: ", NK_TEXT_ALIGN_RIGHT);
					{
						int masterVolume = window.clientOptions.get(Delimeter.Int, ClientOptions.intOptions.masterVolumeName, 50);
						masterVolume = nk_slide_int(context, 0, masterVolume, 100, 5);
						window.clientOptions.put(ClientOptions.intOptions.masterVolumeName, masterVolume);
					}

					nk_label(context, "" + window.clientOptions.get(Delimeter.Int, ClientOptions.intOptions.masterVolumeName), NK_TEXT_CENTERED);
					boolean masterVolumeMuted = window.clientOptions.get(Delimeter.Bool, ClientOptions.boolOptions.muteMasterVolume, false);

					if (nk_button_label(context, masterVolumeMuted ? "x" : "o")) {
						window.clientOptions.put(ClientOptions.boolOptions.muteMasterVolume, !masterVolumeMuted);
					}
					nk_label(context, "\t Game: ", NK_TEXT_ALIGN_RIGHT);
					{
						int gameVolume = window.clientOptions.get(Delimeter.Int, ClientOptions.intOptions.gameVolumeName, 50);
						gameVolume = nk_slide_int(context, 0, gameVolume, 100, 5);

						window.clientOptions.put(ClientOptions.intOptions.gameVolumeName, gameVolume);

					}
					nk_label(context, "" + window.clientOptions.get(Delimeter.Int, ClientOptions.intOptions.gameVolumeName), NK_TEXT_CENTERED);
					boolean gameVolumeMuted = window.clientOptions.get(Delimeter.Bool, ClientOptions.boolOptions.muteGameVolume, false);
					if (nk_button_label(context, gameVolumeMuted ? "x" : "o")) {
						window.clientOptions.put(ClientOptions.boolOptions.muteGameVolume, !gameVolumeMuted);
					}

					nk_label(context, "\t Music: ", NK_TEXT_ALIGN_RIGHT);
					{
						int musicVolume = window.clientOptions.get(Delimeter.Int, ClientOptions.intOptions.musicVolumeName, 50);
						musicVolume = nk_slide_int(context, 0, musicVolume, 100, 5);
						window.clientOptions.put(ClientOptions.intOptions.musicVolumeName, musicVolume);

					}
					nk_label(context, "" + window.clientOptions.get(Delimeter.Int, ClientOptions.intOptions.musicVolumeName), NK_TEXT_CENTERED);
					boolean musicVolumeMuted = window.clientOptions.get(Delimeter.Bool, ClientOptions.boolOptions.muteMusicVolume, false);
					if (nk_button_label(context, musicVolumeMuted ? "x" : "o")) {
						window.clientOptions.put(ClientOptions.boolOptions.muteMusicVolume, !musicVolumeMuted);
					}

					nk_group_end(context);
				}
				nk_spacing(context, 1);

			}

			@Override
			public int getGroupTags() {
				return NK_WINDOW_SCROLL_AUTO_HIDE;
			}
		};
		optionsMenu.close.add(new Consumer<NuklearUIComponent>() {

			@Override
			public void accept(NuklearUIComponent updateTo) {
				clientOptionSystem.writeSettingsFile(window.clientOptions);
			}
		});
		optionsMenu.title = "Options";
		menuSystem.setupMenu(window, optionsMenu, optionsMenuDetails);
		//TODO setup pause menu

		
		

		pauseMenu.container = window;
		Bounds2DComponent pauseMenuContainerBounds = pauseMenu.container.getBounds2DComponent();
		pauseMenu.UUID = NuklearMenuSystem.generateUUID();
		pauseMenu.title = "Pause";
		pauseMenu.bounds.set((pauseMenuContainerBounds.width / 3), 0, pauseMenuContainerBounds.width / 3, pauseMenuContainerBounds.height);

		pauseMenu.visible = false;
		pauseMenu.properties.add(NkWindowProperties.BACKGROUND);
		pauseMenu.properties.add(NkWindowProperties.NO_SCROLLBAR);
		pauseMenu.render = new BiConsumer<NkContext, MemoryStack>() {

			@Override
			public void accept(NkContext context, MemoryStack stack) {

				int perc = 30;
				nk_layout_row_dynamic(context, (int) ((pauseMenuContainerBounds.height / 100) * perc), 1);

				if (nk_group_begin(context, "", 0)) {
					nk_group_end(context);
				}

				nk_layout_row_dynamic(context, (int) ((pauseMenuContainerBounds.height / 100) * (100 - perc)), 1);
				if (nk_group_begin(context, "Menu", 0)) {
					nk_layout_row_dynamic(context, (int) ((pauseMenuContainerBounds.height / 100) * (100 - perc)) / 5, 1);

					if (nk_button_label(context, "Resume")) {
						menuSystem.setActiveNuklear(window.menuQueue, null, null);
						setWindowState(window, WindowState.Game_Running);
					}
					if (nk_button_label(context, "Options")) {
						menuSystem.navigateNuklear(window.registeredNuklear, window.menuQueue, optionsMenu.UUID);
						setWindowState(window, WindowState.Menu);

					}
					if (nk_button_label(context, "Main Menu")) {
						menuSystem.setActiveNuklear(window.menuQueue, window.registeredNuklear, mainMenu.UUID);
						setWindowState(window, WindowState.Menu);
					}
					nk_group_end(context);
				}
			}
		};

		//		menuSystem.setupLevelSelectMenu(window, levelSelectMenuWrapper, levelSelectIndex, levelPreviewWrapper, levelSelect);

		menuSystem.registerUI(window.registeredNuklear, mainMenu);
		menuSystem.registerUI(window.registeredNuklear, optionsMenu);
		menuSystem.registerUI(window.registeredNuklear, pauseMenu);

		menuSystem.navigateNuklear(window.registeredNuklear, window.menuQueue, mainMenu.UUID);

		window.callbacks.keyCallbackSet.add(menuNavigation);
		window.callbacks.mouseButtonCallbackSet.add(mouse);
	}

	public default NuklearUIComponent setupLevelSelectMenu(ClientWindow window, NuklearMenuSystem menuSystem, Wrapper<LevelPreview[]> levelPreviewWrapper,
		Wrapper<Consumer<LevelPreview>> levelSelect) {

		Wrapper<Integer> levelSelectIndex = new WrapperImpl<>();
		Menu levelDetails = new Menu() {

			@Override
			public boolean spacingBeforeDraw() {
				return false;
			}

			@Override
			public void secondaryClick() {
				menuSystem.setActiveNuklear(window.menuQueue, null, null);
				levelSelect.getWrapped().accept(levelPreviewWrapper.getWrapped()[levelSelectIndex.getWrapped()]);

			}

			@Override
			public void returnClick() {
				menuSystem.goBackNuklear(window.menuQueue);

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
							menuSystem.styledButton(context, active, () -> {
								nk_button_label(context, lp.name);
							});
						} else {
							if (nk_button_label(context, lp.name)) {
								levelSelectIndex.setWrapped(id);
							}
						}
					} else {
						menuSystem.styledButton(context, NuklearMenuSystem.getDisabled(context, stack), () -> nk_button_label(context, "[Locked] " + lp.name));

						if (sel == i) {
							//if disabled cannot be selected 
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
		window.events.postCreation.add(() -> {
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

			//define listener
			AL10.alListener3f(AL10.AL_VELOCITY, 0f, 0f, 0f);
			AL10.alListener3f(AL10.AL_ORIENTATION, 0f, 0f, -1f);
			window.hasAudio = true;
		});
	}

	static void closeAudioSystem(ClientWindow window) {
		window.events.preCleanup.add(() -> {

			for (Integer source : window.audioSources) {
				AL10.alSourceStop(source);
				AL10.alDeleteSources(source);
			}
			ALC10.alcCloseDevice(window.audioDevice);
			ALC.destroy();
		});
	}

	static void initializeNkTheme(NkContext context, NuklearMenuSystem menuSystem) {
		// The MemoryStack is described in the Struct Reference
		try (MemoryStack stack = MemoryStack.stackPush()) {

			// The list of colors we want to use
			NkColor white = NuklearMenuSystem.createColour(stack, 255, 255, 255, 255);
			NkColor black = NuklearMenuSystem.createColour(stack, 0, 0, 0, 255);
			NkColor grey01 = NuklearMenuSystem.createColour(stack, 45, 45, 45, 255);
			//			NkColor grey02 = createColor(stack, 70, 70, 70, 255);
			NkColor grey03 = NuklearMenuSystem.createColour(stack, 120, 120, 120, 255);
			NkColor grey04 = NuklearMenuSystem.createColour(stack, 140, 140, 140, 255);
			NkColor grey05 = NuklearMenuSystem.createColour(stack, 150, 150, 150, 255);
			NkColor grey06 = NuklearMenuSystem.createColour(stack, 160, 160, 160, 255);
			NkColor grey07 = NuklearMenuSystem.createColour(stack, 170, 170, 170, 255);
			NkColor grey08 = NuklearMenuSystem.createColour(stack, 180, 180, 180, 255);
			//			NkColor grey09 = createColor(stack, 185, 185, 185, 255);
			NkColor grey10 = NuklearMenuSystem.createColour(stack, 190, 190, 190, 255);
			//			NkColor grey11 = createColor(stack, 200, 200, 200, 255);
			NkColor grey12 = NuklearMenuSystem.createColour(stack, 240, 240, 240, 255);
			NkColor blue1 = NuklearMenuSystem.createColour(stack, 80, 80, 200, 255);
			NkColor blue2 = NuklearMenuSystem.createColour(stack, 128, 196, 255, 255);
			NkColor blue3 = NuklearMenuSystem.createColour(stack, 64, 196, 255, 255);
			NkColor red = NuklearMenuSystem.createColour(stack, 255, 0, 0, 255);

			// This buffer acts like an array of NkColor structs
			int size = NkColor.SIZEOF * NK_COLOR_COUNT; // How much memory we need to store all the color data
			ByteBuffer buffer = stack.calloc(size);
			NkColor.Buffer colors = new NkColor.Buffer(buffer);
			colors.put(NK_COLOR_TEXT, white);
			colors.put(NK_COLOR_WINDOW, blue1);
			colors.put(NK_COLOR_HEADER, white);
			colors.put(NK_COLOR_BORDER, white);
			colors.put(NK_COLOR_BUTTON, black);
			colors.put(NK_COLOR_BUTTON_HOVER, grey07);
			colors.put(NK_COLOR_BUTTON_ACTIVE, grey06);
			colors.put(NK_COLOR_TOGGLE, grey05);
			colors.put(NK_COLOR_TOGGLE_HOVER, grey03);
			colors.put(NK_COLOR_TOGGLE_CURSOR, grey10);
			colors.put(NK_COLOR_SELECT, grey06);
			colors.put(NK_COLOR_SELECT_ACTIVE, white);
			colors.put(NK_COLOR_SLIDER, grey12);
			colors.put(NK_COLOR_SLIDER_CURSOR, blue2);
			colors.put(NK_COLOR_SLIDER_CURSOR_HOVER, blue3);
			colors.put(NK_COLOR_SLIDER_CURSOR_ACTIVE, blue2);
			colors.put(NK_COLOR_PROPERTY, grey10);
			colors.put(NK_COLOR_EDIT, grey05);
			colors.put(NK_COLOR_EDIT_CURSOR, black);
			colors.put(NK_COLOR_COMBO, grey10);
			colors.put(NK_COLOR_CHART, grey06);
			colors.put(NK_COLOR_CHART_COLOR, grey01);
			colors.put(NK_COLOR_CHART_COLOR_HIGHLIGHT, red);
			colors.put(NK_COLOR_SCROLLBAR, grey08);
			colors.put(NK_COLOR_SCROLLBAR_CURSOR, grey04);
			colors.put(NK_COLOR_SCROLLBAR_CURSOR_HOVER, grey05);
			colors.put(NK_COLOR_SCROLLBAR_CURSOR_ACTIVE, grey06);
			colors.put(NK_COLOR_TAB_HEADER, grey08);
			colors.close();
		}
	}

	/**
	 * This starts the main thread worker and runs setup up most things.<br>
	 * this will block the current thread. 
	 * @param windowManager
	 * @param window
	 * @param nkContext
	 * @param quitCountdown
	 * @param service
	 */
	public default void start(WindowManager windowManager, ClientWindow window, NkContextSingleton nkContext, AtomicInteger quitCountdown, ExecutorService service) {
		/*
				setupAudioSystem(window);
				closeAudioSystem(window);*/
		WindowMonitorBoundsSystem wmbs = new WindowMonitorBoundsSystem();
		wmbs.centerBoundsInMonitor(0, window.bounds);

		window.loopThread = new GLFWThread(window, quitCountdown, window.renderTimeManager, true, new Runnable[] { () -> {
			GLFW.glfwPollEvents();
		} });
		window.events.preCleanup.add(() -> setWindowState(window, WindowState.Close));
		/*Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
		
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				System.out.println("ERROR. ");
				String message = "Thread '" + t.getName() + "' encountered an uncaught error\r\n";
				message += e.getMessage() + "\r\n";
				for (StackTraceElement b : e.getStackTrace()) {
		
					message += "\tat " + b + " \r\n";
		
					//					message += "\t" + b + "\r\n";
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
		
					//					message += "\t" + b + "\r\n";
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
		});*/

		windowManager.create(window);
		Builder t = windowManager.generateManagerRunnable(quitCountdown, NuklearManager.globalEventsHandler(nkContext, windowManager.getActiveWindows()), window.eventTimeManager, window);
		service.execute(() -> window.loopThread.start());
		t.build().run();
		//		service.execute(t.build());
		//		window.loopThread.run();

	}

	public default void setupWindow(ClientWindow window, KeyInputSystem keyInput, NkContextSingleton nkContext,
		ClientOptionSystem clientOptionSystem) {

		clientOptionSystem.readSettingsFile(window.clientOptions);

		WindowHints.debug = true;
		window.setName( "Main Window");
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

		window.events.frameStart.add(() -> {
			if (window.queueChangeCursorType.getWrapped() != null) {
				switch (window.queueChangeCursorType.getWrapped()) {
					case Disabled:
						glfwSetInputMode(window.getID(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
						window.currentCursorType.setWrapped(GLFWCursorType.Disabled);
						break;
					case Hidden:
						glfwSetInputMode(window.getID(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
						window.currentCursorType.setWrapped(GLFWCursorType.Hidden);
						break;
					case Normal:
						glfwSetInputMode(window.getID(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
						window.currentCursorType.setWrapped(GLFWCursorType.Normal);
						break;
					default:
						break;

				}
			}
		});

		window.events.postCreation.add(() -> {
			logger.debug("Started new window thread: " + Thread.currentThread().getName());
			glfwShowWindow(window.getID());
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

		window.callbacks.keyCallbackSet.add((long windowID, int key, int scancode, int action, int mods) -> keyInput.eventHandleKey(window.keyPressMap, windowID, key, scancode, action, mods));
		window.callbacks.mouseButtonCallbackSet.add((long windowID, int key, int action, int mods) -> keyInput.eventHandleMouse(window.mousePressMap, windowID, key, action, mods));

	}

	/**
	 * Changes the window state with the {@link #onWindowStateChange(WindowState, WindowState)} function
	 * @param window
	 * @param state
	 */
	public default void setWindowState(ClientWindow window, WindowState state) {
		WindowState s = window.state.getWrapped();
		//		if (!s.equals(state)) {
		state = onWindowStateChange(s, state);
		window.state.setWrapped(state);
		//		}
	}

	/**
	 * Allows for redirecting of window state switching. <br>
	 * By default this will just send the new state back to the setWindowState
	 * @param oldState the previous state
	 * @param newState the planned new state of the window
	 * @return the actual new state 
	 */
	public default WindowState onWindowStateChange(WindowState oldState, WindowState newState) {
		return newState;
	}

}
