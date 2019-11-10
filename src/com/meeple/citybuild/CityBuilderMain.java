package com.meeple.citybuild;

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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL46;

import com.meeple.citybuild.LevelData.Chunk.Tile;
import com.meeple.citybuild.WorldGenerator.TileTypes;
import com.meeple.citybuild.render.LevelRenderer;
import com.meeple.citybuild.render.RenderingMain;
import com.meeple.citybuild.render.WorldRenderer;
import com.meeple.citybuild.render.WorldRenderer.MeshExt;
import com.meeple.shared.ClientOptionSystem;
import com.meeple.shared.Delta;
import com.meeple.shared.Tickable;
import com.meeple.shared.frame.CursorHelper;
import com.meeple.shared.frame.CursorHelper.SpaceState;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.GLFWManager;
import com.meeple.shared.frame.OGL.KeyInputSystem;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.UniformManager;
import com.meeple.shared.frame.camera.VPMatrixSystem;
import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem.ProjectionMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.VPMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.CameraSpringArm;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.BtnState;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.Button;
import com.meeple.shared.frame.window.ClientWindowSystem;
import com.meeple.shared.frame.window.WindowManager;
import com.meeple.shared.frame.window.WindowState;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;

public class CityBuilderMain extends GameManager implements Consumer<ExecutorService>, ClientWindowSystem {

	public static Logger logger = Logger.getLogger(CityBuilderMain.class);
	private static String debugLayout = "[%r - %d{HH:mm:ss:SSS}][%t][%p] (%F:%L) %m%n";
	static String LevelFolder = "saves/";
	static String LevelExt = ".sv";
	//	private static String normalLayout = "[%d{HH:mm:ss:SSS}][%r]][%t][%p][%c] %m%n";

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

	private boolean renderMenu(Delta time) {

		double menuSeconds = FrameUtils.nanosToSeconds(time.totalNanos);

		float r = (float) (Math.sin(menuSeconds * 0.03f + 0.1f)) * 0.5f;
		float g = (float) (Math.sin(menuSeconds * 0.02f + 0.2f)) * 0.5f;
		float b = (float) (Math.sin(menuSeconds * 0.01f + 0.3f)) * 0.5f;

		window.clearColour.set(r, g, b, 0);
		return false;

	}

	private boolean renderLoading(Delta time) {
		double menuSeconds = FrameUtils.nanosToSeconds(time.totalNanos);

		float r = (float) (Math.sin(menuSeconds * 0.03f + 0.1f)) * 0.5f;
		float g = (float) (Math.sin(menuSeconds * 0.02f + 0.2f)) * 0.5f;
		float b = (float) (Math.sin(menuSeconds * 0.01f + 0.3f)) * 0.5f;
		window.clearColour.set(r, g, b, 0);

		return false;
	}

	@Override
	public void accept(ExecutorService executorService) {

		logger.info("Starting City builder client");
		KeyInputSystem keyInput = new KeyInputSystem();
		NkContextSingleton nkContext = new NkContextSingleton();
		ClientOptionSystem optionsSystem = new ClientOptionSystem();

		ShaderProgram mainProgram = new ShaderProgram();
		ShaderProgram program = new ShaderProgram();
		ShaderProgram uiProgram = new ShaderProgram();

		setupWindow(window, keyInput, nkContext, optionsSystem);
		Map<WindowState, Set<Tickable>> stateRendering = new HashMap<>();

		LevelRenderer levelRenderer = new LevelRenderer();
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

		Supplier<Set<Tickable>> syncSetSupplier = new Supplier<Set<Tickable>>() {
			@Override
			public Set<Tickable> get() {
				return Collections.synchronizedSet(new HashSet<>());
			}
		};

		FrameUtils.addToSetMap(stateRendering, WindowState.Menu, this::renderMenu, syncSetSupplier);
		FrameUtils.addToSetMap(stateRendering, WindowState.Loading, this::renderLoading, syncSetSupplier);
		float size = 0.95f;
		float cube_strip[] = {
			-size, size, size, // Front-top-left
			size, size, size, // Front-top-right
			-size, -size, size, // Front-bottom-left
			size, -size, size, // Front-bottom-right
			size, -size, -size, // Back-bottom-right
			size, size, size, // Front-top-right
			size, size, -size, // Back-top-right
			-size, size, size, // Front-top-left
			-size, size, -size, // Back-top-left
			-size, -size, size, // Front-bottom-left
			-size, -size, -size, // Back-bottom-left
			size, -size, -size, // Back-bottom-right
			-size, size, -size, // Back-top-left
			size, size, -size // Back-top-right
		};
		Mesh cube = new Mesh();

		Attribute vertexAttrib = new Attribute();
		vertexAttrib.name = "position";
		vertexAttrib.bufferType = BufferType.ArrayBuffer;
		vertexAttrib.dataType = GLDataType.Float;
		vertexAttrib.bufferUsage = BufferUsage.StreamDraw;
		vertexAttrib.dataSize = 3;
		vertexAttrib.normalised = false;
		cube.VBOs.add(vertexAttrib);

		Attribute colourAttrib = new Attribute();
		colourAttrib.name = "colour";
		colourAttrib.bufferType = BufferType.ArrayBuffer;
		colourAttrib.dataType = GLDataType.Float;
		colourAttrib.bufferUsage = BufferUsage.StreamDraw;
		colourAttrib.dataSize = 4;
		colourAttrib.normalised = false;
		colourAttrib.instanced = true;
		colourAttrib.instanceStride = 1;
		cube.VBOs.add(colourAttrib);

		Attribute translationAttrib = new Attribute();
		translationAttrib.name = "modelMatrix";
		translationAttrib.bufferType = BufferType.ArrayBuffer;
		translationAttrib.dataType = GLDataType.Float;
		translationAttrib.bufferUsage = BufferUsage.StreamDraw;
		translationAttrib.dataSize = 16;
		translationAttrib.normalised = false;
		translationAttrib.instanced = true;
		translationAttrib.instanceStride = 1;
		cube.VBOs.add(translationAttrib);

		cube.vertexCount = cube_strip.length;
		cube.modelRenderType = GLDrawMode.TriangleStrip;
		for (float f : cube_strip) {
			vertexAttrib.data.add(f);
		}
		for (int i = 0; i < 4; i++) {
			Matrix4f matrix = new Matrix4f();
			matrix.translate(i, 0, 0f);
			Vector4f colour = new Vector4f(0, 0, 1, 1);

			FrameUtils.appendToList(colourAttrib.data, colour);
			FrameUtils.appendToList(translationAttrib.data, matrix);
			cube.renderCount += 1;

		}
		VPMatrixSystem vpSystem = new VPMatrixSystem();
		Wrapper<UniformManager<String[], Integer[]>.Uniform<VPMatrix>> puW = new WrapperImpl<>();
		Wrapper<UniformManager<String, Integer>.Uniform<ProjectionMatrix>> uipuW = new WrapperImpl<>();
		Wrapper<UniformManager<String[], Integer[]>.Uniform<VPMatrix>> mpuW = new WrapperImpl<>();
		window.events.postCreation.add(() -> {

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

			puW.setWrapped(levelRenderer.setupWorldProgram(program, vpSystem, vpMatrix));
			uipuW.setWrapped(levelRenderer.setupUIProgram(uiProgram, vpSystem.projSystem, ortho));
			mpuW.setWrapped(levelRenderer.setupMainProgram(mainProgram, vpSystem, vpMatrix));
			RenderingMain.system.loadVAO(mainProgram, cube);

		});
		window.events.preCleanup.add(() -> {

			executorService.shutdown();
			try {
				executorService.awaitTermination(10l, TimeUnit.SECONDS);
			} catch (InterruptedException err) {
				logger.trace("interupted while waiting termination of executor service. normally fine");
			}
			executorService.shutdownNow();

		});

		Consumer<String> levelSelect = (fileName) -> {

			setWindowState(window, WindowState.Loading);

			executorService.execute(() -> {

				if (fileName != null && !fileName.isEmpty()) {
					logger.trace("Loading game");
					loadLevel(new File(fileName));
				} else {
					logger.trace("Starting new game");
					newGame(0l);
				}

				//just sleep a second to make it a nicer transition
				try {
					Thread.sleep(1000);
				} catch (InterruptedException err) {
				}
				executorService.execute(() -> {
					startGame();

				});
				setWindowState(window, WindowState.Game_Running);

			});

		};
		AtomicInteger clientQuitCounter = new AtomicInteger();

		try (GLFWManager glManager = new GLFWManager(); WindowManager windowManager = new WindowManager()) {
			RayHelper rh = new RayHelper();
			Tickable tick = CameraControlHandler.handlePitchingTick(window, ortho, arm);
			FrameUtils
				.addToSetMap(
					stateRendering,
					WindowState.Game_Running,
					(time) -> {
						vpSystem.preMult(vpMatrix);
						RenderingMain.system.queueUniformUpload(program, RenderingMain.multiUpload, puW.getWrapped(), vpMatrix);
						RenderingMain.system.queueUniformUpload(mainProgram, RenderingMain.multiUpload, mpuW.getWrapped(), vpMatrix);
						//TODO change line thickness
						GL46.glLineWidth(3f);
						GL46.glPointSize(3f);
						keyInput.tick(window.mousePressTicks, window.mousePressMap, time.nanos);
						keyInput.tick(window.keyPressTicks, window.keyPressMap, time.nanos);
						if (level != null) {

							CameraControlHandler.handlePanningTick(window, ortho, vpMatrix.view.getWrapped(), cameraAnchorEntity);
							tick.apply(time);
							{
								long mouseLeftClick = window.mousePressTicks.getOrDefault(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0l);
								if (mouseLeftClick > 0) {
									Vector4f cursorRay = CursorHelper.getMouse(SpaceState.World_Space, window, vpMatrix.proj.getWrapped(), vpMatrix.view.getWrapped());
									rh
										.update(
											new Vector3f(cursorRay.x, cursorRay.y, cursorRay.z),
											new Vector3f(vpMatrix.view.getWrapped().position),
											this);
									Tile tile = rh.getCurrentTile();
									if (tile != null) {
										tile.type = TileTypes.Other;
									}
									Vector3f c = rh.getCurrentTerrainPoint();
									if (c != null) {

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
										m.mesh.singleFrameDiscard = false;
										RenderingMain.system.loadVAO(program, m.mesh);
									}
								}
							}

							//TODO level clear colour
							window.clearColour.set(0f, 0f, 0f, 1f);
							levelRenderer.preRender(level, vpMatrix, program);
							CameraControlHandler.preRenderMouseUI(window, ortho, uiProgram).apply(time);
						}
						RenderingMain.system.render(program);
						RenderingMain.system.render(uiProgram);
						//this is the cube test rendering program
						//						RenderingMain.system.render(mainProgram);
						return false;
					},
					syncSetSupplier);

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
					menuSystem.setActiveNuklear(window.menuQueue, window.activeNuklear, null);
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
					menuSystem.setActiveNuklear(window.menuQueue, window.activeNuklear, null);
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
					menuSystem.setActiveNuklear(window.menuQueue, window.activeNuklear, null);
				}

			};

			setupMenu(window, stateRendering, optionsSystem, menuSystem, nkContext, continueBtn, loadBtn, newBtn);
			start(windowManager, window, nkContext, clientQuitCounter, executorService);

		}
		logger.info("closing client now!");

	}

	@Override
	public WindowState onWindowStateChange(WindowState oldState, WindowState newState) {
		logger.trace(oldState + " " + newState);
		if (newState == WindowState.Game_Pause) {
			//			pauseGame();
		}
		if (newState == WindowState.Game_Running) {
			//			resumeGame();
		}
		if (newState == WindowState.Menu || newState == WindowState.Close) {
			quitGame();
		}
		return newState;

	}

	@Override
	public void renderGame() {
		// TODO Auto-generated method stub

	}

	@Override
	public void levelTick(Delta delta) {
		// TODO Auto-generated method stub

	}

}
