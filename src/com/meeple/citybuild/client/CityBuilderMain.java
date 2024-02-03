package com.meeple.citybuild.client;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallbackI;

import com.meeple.citybuild.client.gui.GameUI;
import com.meeple.citybuild.client.gui.LoadingScreen;
import com.meeple.citybuild.client.gui.MainMenuScreen;
import com.meeple.citybuild.client.gui.PauseScreen;
import com.meeple.citybuild.client.render.LevelRenderer;
import com.meeple.citybuild.client.render.Screen;
import com.meeple.citybuild.server.Buildings;
import com.meeple.citybuild.server.GameManager;
import com.meeple.citybuild.server.LevelData;
import com.meeple.citybuild.server.WorldGenerator;
import com.meeple.shared.ClientOptionSystem;
import com.meeple.shared.Delta;
import com.meeple.shared.RayHelper;
import com.meeple.shared.Tickable;
import com.meeple.shared.frame.GLFWManager;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.KeyInputSystem;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem;
import com.meeple.shared.frame.nuklear.NuklearUIComponent;
import com.meeple.shared.frame.window.ClientWindowSystem;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;
import com.meeple.shared.frame.window.ClientWindowSystem.WindowEvent;
import com.meeple.shared.frame.window.WindowManager;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;
import com.meeple.shared.utils.FrameUtils;

public class CityBuilderMain implements Consumer<ExecutorService> {

	/**
	 * This is the main logger created with log4j. most, if not all, messages come through this
	 */
	public static Logger logger = Logger.getLogger(CityBuilderMain.class);
	/**
	 * Debug layout for the logger to use while in the development environment. tracks location of each message to the file and line. <br>
	 * Not recommended on release due to  performance consumption for finding caller file + line 
	 */
	private static String debugLayout = "[%r][%d{HH:mm:ss:SSS}][%t][%p] (%F:%L) %m%n";
	//	private static String normalLayout = "[%d{HH:mm:ss:SSS}][%r]][%t][%p][%c] %m%n";

	/**
	 * Save folder of the game files
	 */
	final static String LevelFolder = "saves/";
	/**
	 * Save extension of the safe files 
	 */
	final static String LevelExt = ".sv";
	/**
	 * Client container representing most of the OGL and GLFW context per client
	 */
	public final ClientWindow window = new ClientWindow();

	public LevelData level;
	WorldGenerator worldGen = new WorldGenerator();

	public static void main(String[] args) throws Exception {

		ExecutorService service = Executors.newCachedThreadPool();

		//just in case anything doesnt use the log4j
		//		System.setOut(ConsolePrintMirror.outConsole);
		//		System.setErr(ConsolePrintMirror.errConsole);

		//setup logger
		Logger.getRootLogger().setLevel(org.apache.log4j.Level.DEBUG);
		Appender a = new ConsoleAppender(new PatternLayout(debugLayout));
		BasicConfigurator.configure(a);

		//start program
		new CityBuilderMain().accept(service);

	}



	@Override
	public void accept(ExecutorService executorService) {

		logger.info("Starting City builder client");
		KeyInputSystem keyInput = new KeyInputSystem();
		ClientOptionSystem optionsSystem = new ClientOptionSystem();

		ClientWindowSystem.setupWindow(window, keyInput, window.nkContext, optionsSystem);


		window.events.preCleanup.add(() -> {
			FrameUtils.shutdownService(executorService, 1l, TimeUnit.SECONDS);
		});

		window.callbacks.keyCallbackSet.add(new GLFWKeyCallbackI() {

			@Override
			public void invoke(long windowID, int key, int scancode, int action, int mods) {
				if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
					if (level != null) {
						if (level.pause.get()) {
							window.sendEvent(WindowEvent.GameResume);
						} else {
							window.sendEvent(WindowEvent.GamePause);
						}
					}
				}
			}
		});

		AtomicInteger clientQuitCounter = new AtomicInteger();
		try (GLFWManager glManager = new GLFWManager(); GLContext glContext = new GLContext(); WindowManager windowManager = new WindowManager()) {

			RayHelper rh = new RayHelper();

			LevelRenderer levelRenderer = new LevelRenderer();
			Tickable gameRendering = levelRenderer.renderGame(this, glContext, rh, keyInput, window.nkContext);

			//			FrameUtils.addToSetMap(stateRendering, WindowState.Game, t, syncSetSupplier);

			gameRenderScreen = new Screen() {

				@Override
				public void render(ClientWindow window, Delta delta) {
					keyInput.tick(window.mousePressTicks, window.mousePressMap, delta.nanos);
					keyInput.tick(window.keyPressTicks, window.keyPressMap, delta.nanos);
					gameRendering.apply(delta);
				}
			};
			gameUI.colour.w = 0f;
			pauseScreen.colour.w = 0.5f;

			gameRenderScreen.setChild(gameUI);

			clientQuitCounter.incrementAndGet();
			NuklearMenuSystem menuSystem = new NuklearMenuSystem();
			menuSystem.create(window, window.registeredNuklear);
			window.eventListeners.add(this::handleWindowEvent);
			window.render.setChild(loadingScreen);
			loadingScreen.setChild(mainMenuScreen);

			window.events.render.add(0, (delta) -> {

				if (window.currentFocus != null) {
					logger.trace( " " + ((NuklearUIComponent) window.currentFocus).title);
				}
				window.render.renderTree(window, delta);
				return false;

			});
			window.events.preCleanup.add(()->glContext.close());
			ClientWindowSystem.start(windowManager, window, clientQuitCounter, executorService);

		}
		FrameUtils.shutdownService(executorService, 1l, TimeUnit.SECONDS);
		logger.info("closing client now!");

	}

	/**
	 * This is the game rendering controller. 
	 */
	Screen gameRenderScreen;
	LoadingScreen loadingScreen = new LoadingScreen();
	public GameUI gameUI = new GameUI();
	MainMenuScreen mainMenuScreen = new MainMenuScreen();
	PauseScreen pauseScreen = new PauseScreen();

	public void handleWindowEvent(WindowEvent event, Object param) {
		switch (event) {
			case ClientClose:
				GameManager.quitGame(level);
				window.shouldClose = true;
				break;
			case GameLoad:
				if (param != null) {
					if (param instanceof File) {
						level = GameManager.loadLevel((File) param);
					} else if (param instanceof Number) {
						level = GameManager.newGame(worldGen, ((Number) param).longValue());
					} else {
						try {
							level = GameManager.newGame(worldGen, (long) param);
						} catch (ClassCastException e) {
							//wasnt a number

						}
					}
				}
				if (level == null) {
					level = GameManager.newGame(worldGen, System.currentTimeMillis());
				}

				break;
			case GamePause:
				GameManager.pauseGame(level);
				gameUI.setChild(pauseScreen);
				break;
			case GameResume:
				gameUI.clearChild();
				GameManager.resumeGame(level);
				break;
			case GameSave:
			GameManager.saveGame(level);
				break;
			case GameStart:
				loadingScreen.setChild(gameRenderScreen);
				gameUI.clearChild();
				break;
			case GoToMainMenu:
				loadingScreen.setChild(mainMenuScreen);
				logger.trace("herp, todo");
				break;
			case OptionsClose:
				logger.trace("herp, todo");
				break;
			case OptionsOpen:
				logger.trace("herp, todo");
				break;
			default:
				break;
		}

	}

	Wrapper<Buildings> placement = new WrapperImpl<>();



}
