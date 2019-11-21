package com.meeple.citybuild.client;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL46;

import com.meeple.citybuild.RayHelper;
import com.meeple.citybuild.client.gui.GameUI;
import com.meeple.citybuild.client.gui.LoadingScreen;
import com.meeple.citybuild.client.gui.MainMenuScreen;
import com.meeple.citybuild.client.gui.PauseScreen;
import com.meeple.citybuild.client.input.CameraControlHandler;
import com.meeple.citybuild.client.render.LevelRenderer;
import com.meeple.citybuild.client.render.Renderable;
import com.meeple.citybuild.client.render.RenderingMain;
import com.meeple.citybuild.server.Buildings;
import com.meeple.citybuild.server.Entity;
import com.meeple.citybuild.server.GameManager;
import com.meeple.citybuild.server.LevelData;
import com.meeple.citybuild.server.LevelData.Chunk.Tile;
import com.meeple.citybuild.server.WorldGenerator.TileTypes;
import com.meeple.shared.ClientOptionSystem;
import com.meeple.shared.Delta;
import com.meeple.shared.Tickable;
import com.meeple.shared.frame.CursorHelper;
import com.meeple.shared.frame.CursorHelper.SpaceState;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.GLFWManager;
import com.meeple.shared.frame.OGL.KeyInputSystem;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.UniformManager;
import com.meeple.shared.frame.camera.VPMatrixSystem;
import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem.ProjectionMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.VPMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.CameraSpringArm;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.BtnState;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.Button;
import com.meeple.shared.frame.nuklear.NuklearUIComponent;
import com.meeple.shared.frame.window.ClientWindowSystem;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;
import com.meeple.shared.frame.window.ClientWindowSystem.WindowEvent;
import com.meeple.shared.frame.window.WindowManager;
import com.meeple.shared.frame.window.WindowState;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;

public class CityBuilderMain extends GameManager implements Consumer<ExecutorService> {

	public static Logger logger = Logger.getLogger(CityBuilderMain.class);
	private static String debugLayout = "[%r - %d{HH:mm:ss:SSS}][%t][%p] (%F:%L) %m%n";
	static String LevelFolder = "saves/";
	static String LevelExt = ".sv";
	//	private static String normalLayout = "[%d{HH:mm:ss:SSS}][%r]][%t][%p][%c] %m%n";

	private static Supplier<Set<Tickable>> syncSetSupplier = new Supplier<Set<Tickable>>() {
		@Override
		public Set<Tickable> get() {
			return Collections.synchronizedSet(new HashSet<>());
		}
	};

	public static void main(String[] args) throws Exception {

		ExecutorService service = Executors.newCachedThreadPool();

		//just in case anything doesnt use the log4j
		//		System.setOut(ConsolePrintMirror.outConsole);
		//		System.setErr(ConsolePrintMirror.errConsole);
		Logger.getRootLogger().setLevel(org.apache.log4j.Level.ALL);
		Appender a = new ConsoleAppender(new PatternLayout(debugLayout));
		BasicConfigurator.configure(a);

		new CityBuilderMain().accept(service);

	}

	//TODO remove this 
	final ClientWindow window = new ClientWindow();
	NuklearUIComponent placementUI = new NuklearUIComponent();

	@Override
	public void accept(ExecutorService executorService) {

		logger.info("Starting City builder client");
		KeyInputSystem keyInput = new KeyInputSystem();
		NkContextSingleton nkContext = new NkContextSingleton();
		ClientOptionSystem optionsSystem = new ClientOptionSystem();

		ClientWindowSystem.setupWindow(window, keyInput, nkContext, optionsSystem);
		//		Map<WindowState, Set<Tickable>> stateRendering = new HashMap<>();

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
		/*
				FrameUtils.addToSetMap(stateRendering, WindowState.MainMenu, this::renderMenu, syncSetSupplier);
				FrameUtils.addToSetMap(stateRendering, WindowState.Loading, this::renderLoading, syncSetSupplier);*/
		window.events.preCleanup.add(() -> {
			shutdownService(executorService);
		});
		window.changeStateEvent = this::onWindowStateChange;

		Consumer<String> levelSelect = (fileName) -> {

			window.setWindowState(WindowState.Loading);

			executorService.execute(() -> {

				if (fileName != null && !fileName.isEmpty()) {
					logger.trace("Loading game");
					loadLevel(new File(fileName));
				} else {
					logger.trace("Starting new game");
					newGame(0l);
				}

				executorService.execute(() -> {
					startGame();
				});
				//just sleep a second to make it a nicer transition
				try {
					Thread.sleep(1000);
				} catch (InterruptedException err) {
				}
				window.setWindowState(WindowState.Game);

			});

		};
		AtomicInteger clientQuitCounter = new AtomicInteger();
		try (GLFWManager glManager = new GLFWManager(); WindowManager windowManager = new WindowManager()) {
			RayHelper rh = new RayHelper();

			Tickable t = renderGame(window, vpMatrix, cameraAnchorEntity, ortho, rh, keyInput);

			//			FrameUtils.addToSetMap(stateRendering, WindowState.Game, t, syncSetSupplier);

			gameRenderScreen = new Renderable() {

				@Override
				public void render(NkContextSingleton nkContext, ClientWindow window, Delta delta) {
					t.apply(delta);
				}
			};
			gameUIScreen.colour.w = 0f;

			gameRenderScreen.setChild(gameUIScreen);

			clientQuitCounter.incrementAndGet();
			NuklearMenuSystem menuSystem = new NuklearMenuSystem();
			Button continueBtn = new Button() {

				@Override
				public BtnState getState() {
					return BtnState.Visible;
				}

				@Override
				public String getName() {
					return "continue";
				}

				@Override
				public void onClick() {
					logger.trace("continue game click");
					//TODO find latest save
					String latestName = "";
					levelSelect.accept(latestName);
					menuSystem.setActiveNuklear(window.menuQueue, window.registeredNuklear, null);
				}

			};
			Button loadBtn = new Button() {

				@Override
				public BtnState getState() {
					return BtnState.Visible;
				}

				@Override
				public String getName() {
					return "Load Game";
				}

				@Override
				public void onClick() {
					logger.trace("Load game click");
					//TODO find loaded file name
					String loadedName = "";
					levelSelect.accept(loadedName);
					menuSystem.setActiveNuklear(window.menuQueue, window.registeredNuklear, null);
				}

			};
			Button newBtn = new Button() {

				@Override
				public BtnState getState() {
					return BtnState.Visible;
				}

				@Override
				public String getName() {
					return "New Game";
				}

				@Override
				public void onClick() {
					logger.trace("NEW game click");
					levelSelect.accept(null);
					menuSystem.setActiveNuklear(window.menuQueue, window.registeredNuklear, null);
				}
			};

			menuSystem.create(nkContext, window, window.registeredNuklear);
			window.eventListeners.add(this::handleWindowEvent);
			window.setChild(loadingScreen);
			loadingScreen.setChild(mainMenuScreen);

			Map<WindowState, Delta> ticks = new HashMap<>();

			window.events.render.add(0, (delta) -> {

				if (window.state != null) {

					WindowState state = window.state;
					Delta time = ticks.get(state);
					if (time == null) {
						time = new Delta();
						ticks.put(state, time);
					}
					time.nanos = delta.nanos;
					time.seconds = delta.seconds;
					time.totalNanos += delta.nanos;

					//					System.out.println(nk_item_is_any_active(nkContext.context));
					if (window.currentFocus != null) {
						//						logger.trace(window.state.getWrapped() + " " + ((NuklearUIComponent) window.currentFocus).title);
					}
					//					Set<Tickable> r = stateRendering.get(state);
					window.renderTree(nkContext, window, delta);
					//					FrameUtils.iterateTickable(r, time);
				}
				return false;

			});
			ClientWindowSystem.start(windowManager, window, nkContext, clientQuitCounter, executorService);

		}
		shutdownService(executorService);
		logger.info("closing client now!");

	}

	Renderable gameRenderScreen;
	LoadingScreen loadingScreen = new LoadingScreen();
	GameUI gameUIScreen = new GameUI();
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
				break;
			case GameResume:
				resumeGame();
				break;
			case GameSave:
				saveGame();
				break;
			case GameStart:
				loadingScreen.setChild(gameRenderScreen);
				break;
			case GoToMainMenu:
				window.setChild(mainMenuScreen);
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

	public WindowState onWindowStateChange(WindowState oldState, WindowState newState) {
		if (newState == WindowState.Game/*_Pause*/) {
			placementUI.visible = false;
			pauseGame();
		}
		if (newState == WindowState.Game/*_Running*/) {
			placementUI.visible = true;
			resumeGame();
		}
		if (newState == WindowState.MainMenu || newState == WindowState.Close) {
			placementUI.visible = false;
			quitGame();
		}
		return newState;

	}

	public Tickable renderGame(ClientWindow window, VPMatrix vpMatrix, Entity cameraAnchorEntity, ProjectionMatrix ortho, RayHelper rh, KeyInputSystem keyInput) {

		//		ShaderProgram mainProgram = new ShaderProgram();
		ShaderProgram program = new ShaderProgram();
		ShaderProgram uiProgram = new ShaderProgram();

		VPMatrixSystem vpSystem = new VPMatrixSystem();
		LevelRenderer levelRenderer = new LevelRenderer();

		Wrapper<UniformManager<String[], Integer[]>.Uniform<VPMatrix>> puW = new WrapperImpl<>();
		Wrapper<UniformManager<String, Integer>.Uniform<ProjectionMatrix>> uipuW = new WrapperImpl<>();

		vpMatrix.proj.getWrapped().window = window;
		vpMatrix.proj.getWrapped().FOV = 90;
		vpMatrix.proj.getWrapped().nearPlane = 0.001f;
		vpMatrix.proj.getWrapped().farPlane = 10000f;
		vpMatrix.proj.getWrapped().orthoAspect = 10f;
		vpMatrix.proj.getWrapped().perspectiveOrOrtho = true;
		vpMatrix.proj.getWrapped().scale = 1f;

		ortho.window = window;
		ortho.FOV = 90;
		ortho.nearPlane = 0.001f;
		ortho.farPlane = 10000f;
		ortho.orthoAspect = 10f;
		ortho.perspectiveOrOrtho = false;
		ortho.scale = 1f;
		CameraSpringArm arm = vpMatrix.view.getWrapped().springArm;
		window.events.postCreation.add(() -> {

			puW.setWrapped(levelRenderer.setupWorldProgram(program, vpSystem, vpMatrix));
			uipuW.setWrapped(levelRenderer.setupUIProgram(uiProgram, vpSystem.projSystem, ortho));

			/*mpuW.setWrapped(levelRenderer.setupMainProgram(mainProgram, vpSystem, vpMatrix));
			RenderingMain.system.loadVAO(mainProgram, cube);*/

		});

		vpSystem.preMult(vpMatrix);
		Tickable tick = CameraControlHandler.handlePitchingTick(window, ortho, arm);
		return (time) -> {
			vpSystem.preMult(vpMatrix);
			RenderingMain.system.queueUniformUpload(program, RenderingMain.multiUpload, puW.getWrapped(), vpMatrix);
			//TODO change line thickness
			GL46.glLineWidth(3f);
			GL46.glPointSize(3f);
			keyInput.tick(window.mousePressTicks, window.mousePressMap, time.nanos);
			keyInput.tick(window.keyPressTicks, window.keyPressMap, time.nanos);
			if (level != null) {

				CameraControlHandler.handlePanningTick(window, ortho, vpMatrix.view.getWrapped(), cameraAnchorEntity);
				tick.apply(time);

				long mouseLeftClick = window.mousePressTicks.getOrDefault(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0l);
				if (mouseLeftClick > 0) {
					Vector4f cursorRay = CursorHelper.getMouse(SpaceState.World_Space, window, vpMatrix.proj.getWrapped(), vpMatrix.view.getWrapped());
					rh.update(new Vector3f(cursorRay.x, cursorRay.y, cursorRay.z), new Vector3f(vpMatrix.view.getWrapped().position), this);
					Tile tile = rh.getCurrentTile();
					if (tile != null) {
						tile.type = TileTypes.Other;
						rh.getCurrentChunk().rebake.set(true);
					}

					Vector3f c = rh.getCurrentTerrainPoint();
					/*
																if (c != null) {
																//TODO rendering debug mouse cursor pos
																Vector4f colour = new Vector4f(1, 0, 0, 1);
																MeshExt m = new MeshExt();
																WorldRenderer.setupDiscardMesh3D(m, 1);
																
																m.positionAttrib.data.add(c.x);
																m.positionAttrib.data.add(c.y);
																m.positionAttrib.data.add(c.z + 1f);
																
																m.colourAttrib.data.add(colour.x);
																m.colourAttrib.data.add(colour.y);
																m.colourAttrib.data.add(colour.z);
																m.colourAttrib.data.add(colour.w);
																
																m.mesh.name = "model";
																m.mesh.modelRenderType = GLDrawMode.Points;
																m.mesh.singleFrameDiscard = true;
																RenderingMain.system.loadVAO(program, m.mesh);
																}*/

				}

				//TODO level clear colour
				window.clearColour.set(0f, 0f, 0f, 0f);
				levelRenderer.preRender(level, vpMatrix, program);
				CameraControlHandler.preRenderMouseUI(window, ortho, uiProgram).apply(time);

				//				MeshExt mesh = new MeshExt();
				//				bakeChunk(level.chunks.get(new Vector2i()), mesh);
				//				RenderingMain.system.loadVAO(program, mesh.mesh);

			}

			RenderingMain.system.render(program);
			RenderingMain.system.render(uiProgram);
			//this is the cube test rendering program
			//						RenderingMain.system.render(mainProgram);
			return false;
		};
	}

	Wrapper<Buildings> placement = new WrapperImpl<>();

	@Override
	public void levelTick(Delta delta) {

	}

}
