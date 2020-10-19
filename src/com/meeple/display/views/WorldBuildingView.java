package com.meeple.display.views;

import static org.lwjgl.nuklear.Nuklear.NK_TEXT_ALIGN_CENTERED;
import static org.lwjgl.nuklear.Nuklear.nk_begin;
import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_label;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_progress;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.function.BiFunction;

import org.apache.log4j.Logger;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkColorf;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL46;

import com.meeple.backend.Client;
import com.meeple.backend.FrameTimings;
import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.ShaderPrograms.Program;
import com.meeple.backend.events.RegionGenerationEvent;
import com.meeple.backend.events.TerrainGenerationEvent;
import com.meeple.backend.game.world.TerrainSampleInfo;
import com.meeple.backend.game.world.TerrainSampleInfo.TerrainType;
import com.meeple.backend.game.world.World;
import com.meeple.backend.game.world.World.WorldGeneratorType;
import com.meeple.backend.game.world.WorldClient;
import com.meeple.backend.view.FreeFlyCameraController;
import com.meeple.backend.view.VPMatrix;
import com.meeple.backend.view.VPMatrix.CameraKey;
import com.meeple.display.views.WorldBuilding.WorldBuildingTerrainGenerator;
import com.meeple.display.views.WorldBuilding.WorldBuildingTerrainMeshHelper;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.GLTexture;
import com.meeple.shared.frame.OGL.ShaderProgram.IndexBufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;

public class WorldBuildingView {

	private static Logger logger = Logger.getLogger(WorldBuildingView.class);

	private NkColorf background = NkColorf.create().r(0.50f).g(0.09f).b(0.54f).a(1.0f);

	public FreeFlyCameraController playerController = new FreeFlyCameraController();
	private float fov = 60;
	private CameraKey primaryCamera;

	private WorldBuildingTerrainMeshHelper worldBuildingTerrainMeshHelper = new WorldBuildingTerrainMeshHelper();

	private TerrainType[][] mapTest = new TerrainType[160][160];
	World world = new World("Test");
	WorldClient worldClient;

	Mesh textured = new Mesh();
	GLTexture texture = new GLTexture();

	public void setup(Client client, VPMatrix vpMatrix) {
		ShaderPrograms.initAndCreate(client.glContext, Program._3D_Unlit_Texture);
		VPMatrix.bindToProgram(Program._3D_Unlit_Texture.program.programID, vpMatrix.getBindingPoint());
		//setup the meshes 
		worldBuildingTerrainMeshHelper.setup(client.glContext);

		{
			//create the new camera that we will be using
			primaryCamera = vpMatrix.newCamera();

			//bind the camera to controller 
			playerController.register(client.windowID, client.userInput);
			playerController.operateOn(vpMatrix.getCamera(primaryCamera));
			vpMatrix.getCamera(primaryCamera).setTranslation(mapTest.length / 2, mapTest[0].length / 2, 5);
		}

		Random r = new Random(1);
		new WorldBuildingTerrainGenerator().setup(r, mapTest);
		textured = ShaderPrograms.constructMesh(Program._3D_Unlit_Texture, "texturedMeshTest", 6, GLDrawMode.Triangles);
		textured.getAttribute(ShaderPrograms.vertAtt.name).data(new float[] {
			-0.5f, 0.5f, 0,
			-0.5f, -0.5f, 0,
			0.5f, -0.5f, 0,
			0.5f, 0.5f, 0
		});
		textured.index(new IndexBufferObject().data(new int[] { 0, 1, 3, 3, 1, 2 }));
		textured.getAttribute(ShaderPrograms.textureAtt.name).data(new float[] {
			0, 0,
			0, 1,
			1, 1,
			1, 0
		});
		textured.getAttribute(ShaderPrograms.transformAtt.name).data(new Matrix4f().scale(10f).translate(0, 0, -2).get(new float[16]));

		Program prog = Program._3D_Unlit_Texture;
		logger.info(prog);
		ShaderProgramSystem2.loadVAO(client.glContext, ShaderPrograms.Program._3D_Unlit_Texture.program, textured);

		GL46.glProgramUniform1f(Program._3D_Unlit_Texture.program.programID, ShaderProgramSystem2.getUniformLocation(Program._3D_Unlit_Texture.program, "alphaDiscardThreshold"), 0.5f);

		texture = ShaderProgramSystem2.loadTexture(client.glContext, "resources/imgs/Wood_Raw.png");
		worldClient = new WorldClient(new BiFunction<Float, Float, TerrainSampleInfo>() {

			@Override
			public TerrainSampleInfo apply(Float t, Float u) {
				TerrainSampleInfo tsi = new TerrainSampleInfo();
				tsi.height = 1;
				tsi.type = TerrainType.Ground;
				return tsi;
			}
		}, client.service);

		world.registerListener(TerrainGenerationEvent.class, worldClient::terrainGenerated);
		world.registerListener(RegionGenerationEvent.class, worldClient::regionGenerated);
		worldClient.setupProgram(client.glContext);
		VPMatrix.bindToProgram(worldClient.getShaderProgram().programID, vpMatrix.getBindingPoint());
		world.setupGenerator("1");
		world.setGeneratorType(WorldGeneratorType.Flat);

		//		world.generate();

	}

	private static boolean once = false;

	public void render(Client client, VPMatrix vpMatrix, FrameTimings delta) {

		if (!world.generated) {

			worldClient.StartHold();
			world.generate();
			playerController.register(client.windowID, client.userInput);
			playerController.operateOn(vpMatrix.getCamera(primaryCamera));
		}
		float prog = worldClient.progress();

		if (prog < 1f) {
			if (nk_begin(client.nkContext.context, "loading", NkRect.create().set(50, 50, 500, 500), 0)) {

				nk_layout_row_dynamic(client.nkContext.context, 50, 1);
				nk_label(client.nkContext.context, "Loading", NK_TEXT_ALIGN_CENTERED);
				long max = 100;
				PointerBuffer pb = BufferUtils.createPointerBuffer(1);
				pb.put((long) (max * worldClient.progress()));
				pb.flip();
				nk_progress(client.nkContext.context, pb, max, false);

			}
			nk_end(client.nkContext.context);
		} else {
			worldClient.free();
		}
		if (world.generated && prog == 1f) {
			innerRender(client, vpMatrix, delta);
		}
	}

	private void innerRender(Client client, VPMatrix vpMatrix, FrameTimings delta) {

		//		GL46.glEnable(GL46.GL_CULL_FACE);
		//		GL46.glCullFace(GL46.GL_BACK);
		glClearColor(background.r(), background.g(), background.b(), background.a());
		// NOTE this denotes that GL is using a new frame.
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		Vector3f translation3 = vpMatrix.getViewPosition(primaryCamera);
		String pos = translation3.toString(new DecimalFormat("0.000"));
		
		if (Nuklear.nk_begin(client.nkContext.context, "", NkRect.create().set(0, 0, 300, 60), Nuklear.NK_WINDOW_NO_SCROLLBAR)) {
			Nuklear.nk_layout_row_dynamic(client.nkContext.context, 30f, 1);
			Nuklear.nk_label(client.nkContext.context, pos, Nuklear.NK_TEXT_ALIGN_LEFT);
			Nuklear.nk_label(client.nkContext.context, vpMatrix.getCamera(primaryCamera).getRotation(new AxisAngle4f()).x+ " " , Nuklear.NK_TEXT_ALIGN_LEFT);
			Nuklear.nk_end(client.nkContext.context);
		}
		boolean forceTerrainUpdate = false;
		if (client.userInput.isKeyPressed(GLFW.GLFW_KEY_E)) {
			TerrainSampleInfo tsi = new TerrainSampleInfo();
			tsi.set(world.getStorage().getTile(translation3.x, translation3.y));
			//			TerrainSampleInfo tsi = new TerrainSampleInfo();
			tsi.height += 1f * delta.deltaSeconds;
			world.getStorage().setTile(translation3.x, translation3.y, tsi);
			forceTerrainUpdate = true;
		}
		if (client.userInput.isKeyPressed(GLFW.GLFW_KEY_Q)) {
			TerrainSampleInfo tsi = new TerrainSampleInfo();
			tsi.type = TerrainType.Ground;
			tsi.height = 0f;
			world.getStorage().setTile(translation3.x, translation3.y, tsi);
			forceTerrainUpdate = true;
		}

		if (client.userInput.isKeyPressed(GLFW.GLFW_KEY_F5)) {
			vpMatrix.getCamera(primaryCamera).identity();
			vpMatrix.getCamera(primaryCamera).rotateX((float) Math.toDegrees(90));
			vpMatrix.getCamera(primaryCamera).translate(-World.TerrainSize, -World.TerrainSize, -5);

		}
		if (client.userInput.isKeyPressed(GLFW.GLFW_KEY_F6)) {
			vpMatrix.getCamera(primaryCamera).identity();
			vpMatrix.getCamera(primaryCamera).rotateX((float) -Math.toDegrees(85));
			//			vpMatrix.getCamera(primaryCamera).rotateX((float) Math.toDegrees(90));
			vpMatrix.getCamera(primaryCamera).translate(0, 0, -1);
		}

		if (client.userInput.isKeyPressed(GLFW.GLFW_KEY_F9)) {
			try {
				world.save();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		{

			world.tick(delta);
			vpMatrix.setPerspective(fov, (float) client.windowWidth / client.windowHeight, 0.01f, 1000.0f);
			vpMatrix.activeCamera(primaryCamera);

			vpMatrix.upload();

			worldClient.cameraCheck(world, vpMatrix.getVPMatrix(),translation3);
			if (playerController.tick(delta, client) || forceTerrainUpdate) {

			}

			worldClient.preRender(client.glContext);

			worldClient.render();
		}

		// NOTE in the future this will be replaced with a "has terrain updated" check rather
		// than the first time it render  but this will do for now
		if (!once) {
			worldBuildingTerrainMeshHelper.preRender(mapTest);
			once = true;
		}
		/*
				try (ShaderClosable sc = ShaderProgramSystem2.useProgram(Program._3D_Unlit_Flat.program)) {
					worldBuildingTerrainMeshHelper.render();
				}
		
				try (ShaderClosable sc = ShaderProgramSystem2.useProgram(Program._3D_Unlit_Texture.program)) {
					try (ShaderClosable tc = ShaderProgramSystem2.useTexture(texture)) {
						ShaderProgramSystem2.tryFullRenderMesh(textured);
					}
				}*/
	}
}
