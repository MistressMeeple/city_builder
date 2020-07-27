package com.meeple.display.views;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.util.par.ParShapes.par_shapes_create_parametric_sphere;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkColorf;
import org.lwjgl.util.par.ParShapes;
import org.lwjgl.util.par.ParShapesMesh;

import com.meeple.backend.Client;
import com.meeple.backend.FrameTimings;
import com.meeple.backend.Model;
import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.ShaderPrograms.InstancedAttribute;
import com.meeple.backend.ShaderPrograms.Program;
import com.meeple.backend.entity.EntityBase;
import com.meeple.backend.entity.NPCEntity;
import com.meeple.backend.events.RegionGenerationEvent;
import com.meeple.backend.events.TerrainGenerationEvent;
import com.meeple.backend.game.world.World;
import com.meeple.backend.game.world.WorldClient;
import com.meeple.backend.game.world.features.ParShapeMeshI;
import com.meeple.backend.view.BoundCameraController;
import com.meeple.backend.view.VPMatrix;
import com.meeple.backend.view.VPMatrix.CameraKey;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;

public class WorldView {

	public World world;
	public WorldClient worldClient;
	public EntityBase playerEntity;

	private float fov = 60;

	private String seedText = "0";
	public CameraKey primaryCamera;
	public BoundCameraController playerController = new BoundCameraController();
	private NkColorf background = NkColorf.create().r(0.20f).g(0.09f).b(0.54f).a(1.0f);

	public void setup(Client client, VPMatrix vpMatrix) {
		primaryCamera = vpMatrix.newCamera();
		world = new World();
		world.setupGenerator(seedText);

		{
			worldClient = new WorldClient(world::sample, client.service);
			world.registerListener(TerrainGenerationEvent.class, worldClient::terrainGenerated);
			world.registerListener(RegionGenerationEvent.class, worldClient::regionGenerated);
			worldClient.setupProgram(client.glContext);
		}

		{
			playerEntity = new NPCEntity();
			playerEntity.transformation().translate(0, 0, 100);
			world.addEntity(playerEntity);
		}

		VPMatrix.bindToProgram(worldClient.getShaderProgram().programID, vpMatrix.getBindingPoint());
		vpMatrix.getCamera(primaryCamera).setTranslation(-1, 0, 10f);

		ParShapeMeshI spherePMesh = new ParShapeMeshI() {

			@Override
			public ParShapesMesh generate() {
				ParShapesMesh mesh = par_shapes_create_parametric_sphere(6, 6);
				ParShapes.par_shapes_scale(mesh, 0.25f, 0.25f, 0.25f);
				ParShapes.par_shapes_translate(mesh, 0, 0, (0.25f * 5f) + (-0.05f));
				return mesh;
			}

		};
		Mesh sphereMesh = spherePMesh.convert(spherePMesh.generate());
		// https://www.schemecolor.com/real-skin-tones-color-palette.php
		float[][] colours = new float[][] { { 141, 85, 36, 1 }, { 198, 134, 66, 1 }, { 224, 172, 105, 1 }, { 241, 194, 125, 1 }, { 255, 219, 172, 1 } };
		Random random = ThreadLocalRandom.current();

		int index = random.nextInt(colours.length - 1);
		float[] col1 = colours[index];
		float[] col2 = colours[random.nextInt(colours.length - 1)];
		sphereMesh.getAttribute(ShaderPrograms.colourAtt.name).data(new float[] { col1[0], col1[1], col1[2], col1[3], col2[0], col2[1], col2[2], col2[3] }).instanced(true, 1000);

		ParShapeMeshI conePMesh = new ParShapeMeshI() {

			@Override
			public ParShapesMesh generate() {
				ParShapesMesh mesh = ParShapes.par_shapes_create_cone(6, 6);

				ParShapes.par_shapes_scale(mesh, 0.25f, 0.25f, -3.5f * 0.25f);
				ParShapes.par_shapes_translate(mesh, 0, 0, (0.125f * 7.5f) + (-0.05f));
				return mesh;
			}

		};
		{
			Model human = new Model();
			human.addMesh(conePMesh.convert(conePMesh.generate()));
			human.addMesh(sphereMesh, InstancedAttribute.Colour);
			human.loadVAOs(client.glContext, Program._3D_Unlit_Flat.program);
			human.enableAttributes(ShaderPrograms.InstancedAttribute.Colour, ShaderPrograms.InstancedAttribute.Transformation);

			worldClient.modelManager.register(NPCEntity.class, human);
		}

	}

	public void preRender() {

		glClearColor(background.r(), background.g(), background.b(), background.a());
		// NOTE this denotes that GL is using a new frame.
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

	}

	public void render(Client client, VPMatrix vpMatrix, FrameTimings delta) {

		background.set(0.1f, 0.1f, 0.1f, 1f);
		// NOTE this is a way to mess with world speed
		delta.deltaSeconds = delta.deltaSeconds / 1f;

		world.tick(delta);
		vpMatrix.setPerspective(fov, (float) client.windowWidth / client.windowHeight, 0.01f, 10000.0f);
		vpMatrix.activeCamera(primaryCamera);

		if (playerController.tick(delta, client)) {
			worldClient.cameraCheck(world, vpMatrix.getVPMatrix(), playerController.getBound());
		}

		{
			Boolean pressed = client.userInput.keyPress(GLFW.GLFW_KEY_EQUAL);
			if ((pressed != null && pressed == true) || client.userInput.isKeyPressed(GLFW.GLFW_KEY_EQUAL) && playerEntity.velocity().z == 0) {
				playerEntity.velocity().z += playerEntity.jumpStrength();
			}
		}
		{
			Boolean switchCamera = client.userInput.keyPress(GLFW.GLFW_KEY_F5);
			if (switchCamera != null && switchCamera == true) {

				if (playerController.getBound() == null) {
					playerController.bindTo(playerEntity);
					worldClient.cameraCheck(world, vpMatrix.getVPMatrix(), playerController.getBound());
				} else {
					playerController.unbind();

					worldClient.cameraCheck(world, vpMatrix.getVPMatrix(), playerController.getBound());
				}
			}
		}
		vpMatrix.upload();
		worldClient.preRender(client.glContext);

		worldClient.render();

	}
}
