package com.meeple.citybuild.client.render;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL46;

import com.meeple.citybuild.RayHelper;
import com.meeple.citybuild.client.input.CameraControlHandler;
import com.meeple.citybuild.server.Entity;
import com.meeple.citybuild.server.GameManager;
import com.meeple.citybuild.server.LevelData;
import com.meeple.citybuild.server.LevelData.Chunk.Tile;
import com.meeple.citybuild.server.WorldGenerator.TileTypes;
import com.meeple.shared.Tickable;
import com.meeple.shared.frame.CursorHelper;
import com.meeple.shared.frame.CursorHelper.SpaceState;
import com.meeple.shared.frame.OGL.KeyInputSystem;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.UniformManager;
import com.meeple.shared.frame.camera.VPMatrixSystem;
import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem.ProjectionMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.VPMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.CameraSpringArm;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;

public class GameLayer {

	abstract class Layer extends Screen {

	}

	//		ShaderProgram mainProgram = new ShaderProgram();
	ShaderProgram program = new ShaderProgram();
	ShaderProgram uiProgram = new ShaderProgram();

	VPMatrixSystem vpSystem = new VPMatrixSystem();
	LevelRenderer levelRenderer = new LevelRenderer();

	Wrapper<UniformManager<String[], Integer[]>.Uniform<VPMatrix>> puW = new WrapperImpl<>();
	Wrapper<UniformManager<String, Integer>.Uniform<ProjectionMatrix>> uipuW = new WrapperImpl<>();
	LevelData level;

	public GameLayer(ClientWindow window, VPMatrix vpMatrix, Entity cameraAnchorEntity, ProjectionMatrix ortho, RayHelper rh, KeyInputSystem keyInput, NkContextSingleton nkContext) {
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
		window.events.postCreation.add(() -> {

			puW.setWrapped(levelRenderer.setupWorldProgram(program, vpSystem, vpMatrix));
			uipuW.setWrapped(levelRenderer.setupUIProgram(uiProgram, vpSystem.projSystem, ortho));

			/*mpuW.setWrapped(levelRenderer.setupMainProgram(mainProgram, vpSystem, vpMatrix));
			RenderingMain.system.loadVAO(mainProgram, cube);*/

		});

		vpSystem.preMult(vpMatrix);
	}

	public Tickable r(ClientWindow window, VPMatrix vpMatrix, Entity cameraAnchorEntity, ProjectionMatrix ortho, RayHelper rh, KeyInputSystem keyInput, NkContextSingleton nkContext,
		GameManager gameManager) {

		CameraSpringArm arm = vpMatrix.view.getWrapped().springArm;
		Tickable tick = CameraControlHandler.handlePitchingTick(window, ortho, arm);
		return (time) -> {
			vpSystem.preMult(vpMatrix);
			RenderingMain.instance.system.queueUniformUpload(program, RenderingMain.instance.multiUpload, puW.getWrapped(), vpMatrix);
			//TODO change line thickness
			GL46.glLineWidth(3f);
			GL46.glPointSize(3f);
			keyInput.tick(window.mousePressTicks, window.mousePressMap, time.nanos);
			keyInput.tick(window.keyPressTicks, window.keyPressMap, time.nanos);
			if (level != null) {
				//TODO better ui testing for mouse controls
				if (!Nuklear.nk_item_is_any_active(nkContext.context)) {
					CameraControlHandler.handlePanningTick(window, ortho, vpMatrix.view.getWrapped(), cameraAnchorEntity);
					tick.apply(time);

					long mouseLeftClick = window.mousePressTicks.getOrDefault(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0l);
					if (mouseLeftClick > 0) {
						Vector4f cursorRay = CursorHelper.getMouse(SpaceState.World_Space, window, vpMatrix.proj.getWrapped(), vpMatrix.view.getWrapped());
						rh.update(new Vector3f(cursorRay.x, cursorRay.y, cursorRay.z), new Vector3f(vpMatrix.view.getWrapped().position), gameManager);
						Tile tile = rh.getCurrentTile();
						if (tile != null) {
							tile.type = TileTypes.Other;
							rh.getCurrentChunk().rebake.set(true);
						}

						//					Vector3f c = rh.getCurrentTerrainPoint();
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
			}

			RenderingMain.instance.system.render(program);
			RenderingMain.instance.system.render(uiProgram);
			//this is the cube test rendering program
			//						RenderingMain.system.render(mainProgram);
			return false;
		};
	}

	static class RenderContentState {
		float transparency = 1f;
		boolean inking = false;
	}

	/**
	 * skybox etc
	 */
	RenderContentState skybox = new RenderContentState();
	RenderContentState worldBackground = new RenderContentState();

	RenderContentState terrain = new RenderContentState();
	RenderContentState buildings = new RenderContentState();
	RenderContentState people = new RenderContentState();
	RenderContentState effects = new RenderContentState();
	RenderContentState groundClutter = new RenderContentState();
	RenderContentState extra = new RenderContentState();
	//debug draw
	RenderContentState axis = new RenderContentState();
	RenderContentState boundingBoxes = new RenderContentState();

}
