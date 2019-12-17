package com.meeple.citybuild.client;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallbackI;

import com.meeple.citybuild.RayHelper;
import com.meeple.citybuild.client.gui.GameUI;
import com.meeple.citybuild.client.gui.LoadingScreen;
import com.meeple.citybuild.client.gui.MainMenuScreen;
import com.meeple.citybuild.client.gui.PauseScreen;
import com.meeple.citybuild.client.render.LevelRenderer;
import com.meeple.citybuild.client.render.Screen;
import com.meeple.citybuild.server.Buildings;
import com.meeple.citybuild.server.Entity;
import com.meeple.citybuild.server.GameManager;
import com.meeple.shared.ClientOptionSystem;
import com.meeple.shared.Delta;
import com.meeple.shared.Tickable;
import com.meeple.shared.frame.GLFWManager;
import com.meeple.shared.frame.OGL.KeyInputSystem;
import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem.ProjectionMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.VPMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.CameraSpringArm;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem;
import com.meeple.shared.frame.nuklear.NuklearUIComponent;
import com.meeple.shared.frame.window.ClientWindowSystem;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;
import com.meeple.shared.frame.window.ClientWindowSystem.WindowEvent;
import com.meeple.shared.frame.window.WindowManager;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;

public class CityBuilderMain extends GameManager implements Consumer<ExecutorService> {

	public static Logger logger = Logger.getLogger(CityBuilderMain.class);
	private static String debugLayout = "[%r][%d{HH:mm:ss:SSS}][%t][%p] (%F:%L) %m%n";
	static String LevelFolder = "saves/";
	static String LevelExt = ".sv";
	//	private static String normalLayout = "[%d{HH:mm:ss:SSS}][%r]][%t][%p][%c] %m%n";

	public static void main(String[] args) throws Exception {

		ExecutorService service = Executors.newCachedThreadPool();

		//just in case anything doesnt use the log4j
		//		System.setOut(ConsolePrintMirror.outConsole);
		//		System.setErr(ConsolePrintMirror.errConsole);
		
		//setup logger
		Logger.getRootLogger().setLevel(org.apache.log4j.Level.ALL);
		Appender a = new ConsoleAppender(new PatternLayout(debugLayout));
		BasicConfigurator.configure(a);

		//start program
		new CityBuilderMain().accept(service);

	}

	
	
	public final ClientWindow window = new ClientWindow();
	NuklearUIComponent placementUI = new NuklearUIComponent();

	@Override
	public void accept(ExecutorService executorService) {

		logger.info("Starting City builder client");
		KeyInputSystem keyInput = new KeyInputSystem();
		ClientOptionSystem optionsSystem = new ClientOptionSystem();


		ClientWindowSystem.setupWindow(window, keyInput, window.nkContext, optionsSystem);

		VPMatrix vpMatrix = new VPMatrix();
		CameraSpringArm arm = vpMatrix.view.getWrapped().springArm;
		ProjectionMatrix ortho = new ProjectionMatrix();
		arm.addDistance(15f);
		vpMatrix.view.getWrapped().springArm.addPitch(45);

		Entity cameraAnchorEntity = new Entity();
		vpMatrix.view.getWrapped().springArm.lookAt = new Supplier<Vector3f>() {

			@Override
			public Vector3f get() {
				return cameraAnchorEntity.position;
			}
		};

		window.events.preCleanup.add(() -> {
			shutdownService(executorService);
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
		try (GLFWManager glManager = new GLFWManager(); WindowManager windowManager = new WindowManager()) {
			RayHelper rh = new RayHelper();

			LevelRenderer levelRenderer = new LevelRenderer();
			Tickable t = levelRenderer.renderGame(this, vpMatrix, cameraAnchorEntity, ortho, rh, keyInput, window.nkContext);

			//			FrameUtils.addToSetMap(stateRendering, WindowState.Game, t, syncSetSupplier);

			gameRenderScreen = new Screen() {

				@Override
				public void render(ClientWindow window, Delta delta) {
					t.apply(delta);
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
					//						logger.trace(window.state.getWrapped() + " " + ((NuklearUIComponent) window.currentFocus).title);
				}

				window.render.renderTree(window, delta);

				return false;

			});
			ClientWindowSystem.start(windowManager, window, clientQuitCounter, executorService);

		}
		shutdownService(executorService);
		logger.info("closing client now!");

	}

	Screen gameRenderScreen;
	LoadingScreen loadingScreen = new LoadingScreen();
	public GameUI gameUI = new GameUI();
	MainMenuScreen mainMenuScreen = new MainMenuScreen();
	PauseScreen pauseScreen = new PauseScreen();

	public void handleWindowEvent(WindowEvent event, Object param) {
		switch (event) {
			case ClientClose:
				quitGame();
				window.shouldClose = true;
				break;
			case GameLoad:
				if (param != null) {
					if (param instanceof File) {
						loadLevel((File) param);
					} else if (param instanceof Number) {
						newGame(((Number) param).longValue());
					} else {
						try {
							newGame((long) param);
						} catch (ClassCastException e) {
							//wasnt a number

						}
					}
				}
				if (level == null) {
					newGame(System.currentTimeMillis());
				}

				break;
			case GamePause:
				pauseGame();
				gameUI.setChild(pauseScreen);
				break;
			case GameResume:
				gameUI.clearChild();
				resumeGame();
				break;
			case GameSave:
				saveGame();
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

	private void shutdownService(ExecutorService executorService) {

		if (!executorService.isShutdown()) {
			executorService.shutdown();
			//try to shut peacefully
			while (!executorService.isShutdown()) {
				try {
					executorService.awaitTermination(1l, TimeUnit.SECONDS);
				} catch (InterruptedException err) {
				}
				//forcefully shutdown
				executorService.shutdownNow();
			}
		}
	}

	Wrapper<Buildings> placement = new WrapperImpl<>();

	@Override
	public void levelTick(Delta delta) {

	}

}
