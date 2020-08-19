package com.meeple.display.views;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkColorf;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;

import com.meeple.backend.Client;
import com.meeple.backend.FrameTimings;
import com.meeple.backend.ShaderPrograms.Program;
import com.meeple.backend.entity.ModelManager;
import com.meeple.backend.game.world.TerrainSampleInfo.TerrainType;
import com.meeple.backend.view.FreeFlyCameraController;
import com.meeple.backend.view.VPMatrix;
import com.meeple.backend.view.VPMatrix.CameraKey;
import com.meeple.display.views.WorldBuilding.WorldBuildingTerrainGenerator;
import com.meeple.display.views.WorldBuilding.WorldBuildingTerrainMeshHelper;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2.ShaderClosable;

public class WorldBuildingView {

	private static Logger logger = Logger.getLogger(WorldBuildingView.class);

	private NkColorf background = NkColorf.create().r(0.50f).g(0.09f).b(0.54f).a(1.0f);

	public FreeFlyCameraController playerController = new FreeFlyCameraController();
	private float fov = 60;
	private CameraKey primaryCamera;

	private ModelManager tileModels = new ModelManager();

	private WorldBuildingTerrainMeshHelper worldBuildingTerrainMeshHelper = new WorldBuildingTerrainMeshHelper();

	private TerrainType[][] mapTest = new TerrainType[160][160];
	private Map<Vector2i,TerrainType[][]> terrainTiles = new CollectionSuppliers.MapSupplier<Vector2i, TerrainType[][]>().get();

	public void setup(Client client, VPMatrix vpMatrix) {

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

	}

	private static boolean once = false;

	public void render(Client client, VPMatrix vpMatrix, FrameTimings delta) {

		//		GL46.glEnable(GL46.GL_CULL_FACE);
		//		GL46.glCullFace(GL46.GL_BACK);

		playerController.tick(delta, client);

		vpMatrix.setPerspective(fov, (float) client.windowWidth / client.windowHeight, 0.01f, 100.0f);
		vpMatrix.activeCamera(primaryCamera);
		vpMatrix.upload();

		Matrix4f clone2 = new Matrix4f(vpMatrix.getCamera(primaryCamera));
		clone2.invert();
		Vector3f translation3 = clone2.getTranslation(new Vector3f());
		String pos = translation3.toString(new DecimalFormat("0.000"));
		if (Nuklear.nk_begin(client.nkContext.context, "", NkRect.create().set(0, 0, 300, 300), 0)) {
			Nuklear.nk_layout_row_dynamic(client.nkContext.context, 30f, 1);
			Nuklear.nk_label(client.nkContext.context, pos, Nuklear.NK_TEXT_ALIGN_LEFT);

			Nuklear.nk_end(client.nkContext.context);
		}

		if (client.userInput.isKeyPressed(GLFW.GLFW_KEY_F5)) {
			vpMatrix.getCamera(primaryCamera).identity();
			vpMatrix.getCamera(primaryCamera).rotateX((float) Math.toDegrees(90));
			vpMatrix.getCamera(primaryCamera).translate(-mapTest.length / 2, -mapTest[0].length / 2, -5);

		}

		glClearColor(background.r(), background.g(), background.b(), background.a());
		// NOTE this denotes that GL is using a new frame.
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		// NOTE in the future this will be replaced with a "has terrain updated" check rather
		// than the first time it render  but this will do for now
		if (!once) {
			worldBuildingTerrainMeshHelper.preRender(mapTest);
			once = true;
		}

		try (ShaderClosable sc = ShaderProgramSystem2.useProgram(Program._3D_Unlit_Flat.program)) {
			worldBuildingTerrainMeshHelper.render();

		}
	}
}
