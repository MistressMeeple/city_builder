package com.meeple.citybuild.client.render;

import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL46;

import com.meeple.citybuild.client.CityBuilderMain;
import com.meeple.citybuild.client.render.ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat;
import com.meeple.citybuild.client.render.ShaderProgramDefinitions.ViewMatrices;
import com.meeple.citybuild.server.Entity;
import com.meeple.citybuild.server.LevelData;
import com.meeple.citybuild.server.LevelData.Chunk;
import com.meeple.citybuild.server.LevelData.Chunk.Tile;
import com.meeple.citybuild.server.WorldGenerator.TileTypes;
import com.meeple.citybuild.server.WorldGenerator.Tiles;
import com.meeple.shared.RayHelper;
import com.meeple.shared.Tickable;
import com.meeple.shared.frame.CursorHelper;
import com.meeple.shared.frame.CursorHelper.SpaceState;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.KeyInputSystem;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferDataManagementType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.ShaderProgram.RenderableVAO;
import com.meeple.shared.frame.OGL.ShaderProgramSystem;
import com.meeple.shared.frame.OGL.ShaderProgramSystem;
import com.meeple.shared.frame.OGL.UniformManager;
import com.meeple.shared.frame.camera.VPMatrixSystem;
import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem.ProjectionMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.VPMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.CameraSpringArm;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.utils.CollectionSuppliers;
import com.meeple.shared.utils.FrameUtils;

import javafx.scene.Cursor;

public class LevelRenderer {
	public static Logger logger = Logger.getLogger(LevelRenderer.class);

	static class CubeMesh {
		Attribute colourAttrib = new Attribute();
		Attribute translationAttrib = new Attribute();
	}

	public void preRender(LevelData level, VPMatrix vp, GLContext glContext, ShaderProgram program) {
		FrustumIntersection fi = new FrustumIntersection(vp.cache);

		Set<Entry<Vector2i, Chunk>> set = level.chunks.entrySet();
		synchronized (level.chunks) {
			for (Iterator<Entry<Vector2i, Chunk>> i = set.iterator(); i.hasNext();) {
				Entry<Vector2i, Chunk> entry = i.next();
				Vector2i loc = entry.getKey();
				Chunk chunk = entry.getValue();
				Vector3f chunkPos = new Vector3f(loc.x * LevelData.fullChunkSize, loc.y * LevelData.fullChunkSize, 0);
				RenderableVAO m = baked.get(chunk);
				if (m == null || chunk.rebake.getAndSet(false)) {
					if (m != null) {
						m.singleFrameDiscard = true;
					}
					m = bakeChunk(chunkPos, chunk);
					ShaderProgramSystem.loadVAO(glContext, program, m);
					m.visible = false;
					baked.put(chunk, m);
				}
				switch (fi.intersectAab(chunkPos,
						chunkPos.add(LevelData.fullChunkSize, LevelData.fullChunkSize, 0, new Vector3f()))) {

					case FrustumIntersection.INSIDE:
					case FrustumIntersection.INTERSECT:
						m.visible = true;
						// render chunk
						break;
					case FrustumIntersection.OUTSIDE:
						m.visible = false;
						break;
					default:
						break;
				}

			}
		}
	}

	Map<Chunk, RenderableVAO> baked = new CollectionSuppliers.MapSupplier<Chunk, RenderableVAO>().get();
	Map<TileTypes, Map<String, RenderableVAO>> tileMeshes = new CollectionSuppliers.MapSupplier<TileTypes, Map<String, RenderableVAO>>()
			.get();

	/*
	 * private void bakeTile(Tile tile) {
	 * switch (tile.type) {
	 * 
	 * }
	 * }
	 */

	private RenderableVAO bakeChunk(Vector3f chunkPos, Chunk chunk) {
		//MeshExt m = new MeshExt();
		ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat.Mesh m2 = ShaderProgramDefinitions.collection._3D_unlit_flat.createMesh(1);
		m2.colourAttribute.instanced = true;

		//WorldRenderer.setupDiscardMesh3D(m, 4);
		m2.modelRenderType = GLDrawMode.TriangleFan;
		m2.name = "chunk_" + (int) chunkPos.x + "_" + (int) chunkPos.y;
		m2.vertexCount = 4;
		m2.renderCount = 0;
		m2.vertexAttribute.bufferResourceType = BufferDataManagementType.List;
		m2.colourAttribute.bufferResourceType = BufferDataManagementType.List;
		m2.meshTransformAttribute.bufferResourceType = BufferDataManagementType.List;

		m2.vertexAttribute.data.add(0f);
		m2.vertexAttribute.data.add(0f);
		m2.vertexAttribute.data.add(0f);

		m2.vertexAttribute.data.add(LevelData.tileSize);
		m2.vertexAttribute.data.add(0f);
		m2.vertexAttribute.data.add(0f);

		m2.vertexAttribute.data.add(LevelData.tileSize);
		m2.vertexAttribute.data.add(LevelData.tileSize);
		m2.vertexAttribute.data.add(0f);

		m2.vertexAttribute.data.add(0f);
		m2.vertexAttribute.data.add(LevelData.tileSize);
		m2.vertexAttribute.data.add(0f);

		// TODO bake chunk instead
		for (int x = 0; x < chunk.tiles.length; x++) {
			for (int y = 0; y < chunk.tiles[x].length; y++) {
				Vector3f tilePos = chunkPos.add(x * LevelData.tileSize, y * LevelData.tileSize, 0, new Vector3f());
				Matrix4f tilePosition = new Matrix4f().translate(tilePos);
				Vector4f colour = new Vector4f();
				Tile tile = chunk.tiles[x][y];
				if (tile == null) {
					chunk.tiles[x][y] = chunk.new Tile();
					tile = chunk.tiles[x][y];
				}
				if (tile.type == null) {
					tile.type = Tiles.Hole;
				}

				switch (tile.type) {
					case Hole:

						break;
					case Ground:

						colour = new Vector4f(0.1f, 0.7f, 0.1f, 1f);
						FrameUtils.appendToList(m2.meshTransformAttribute.data, tilePosition);
						m2.colourAttribute.data.add(colour.x);
						m2.colourAttribute.data.add(colour.y);
						m2.colourAttribute.data.add(colour.z);
						m2.colourAttribute.data.add(colour.w);
						m2.renderCount += 1;
						break;
					case Water:
						colour = new Vector4f(0.1f, 0f, 0.7f, 1f);
						FrameUtils.appendToList(m2.meshTransformAttribute.data, tilePosition);
						m2.colourAttribute.data.add(colour.x);
						m2.colourAttribute.data.add(colour.y);
						m2.colourAttribute.data.add(colour.z);
						m2.colourAttribute.data.add(colour.w);
						m2.renderCount += 1;
						break;

				}

			}
		}
		return m2;
	}

	private ShaderProgramDefinition_3D_unlit_flat.Mesh drawAxis(int size) {
		ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat.Mesh mesh = ShaderProgramDefinitions.collection._3D_unlit_flat
				.createMesh(1);

		int count = 3;
		FloatBuffer verts = BufferUtils.createFloatBuffer(2 * 3 * count);
		FloatBuffer colours = BufferUtils.createFloatBuffer(2 * 4 * count);
		verts.put(new float[] { 0, 0, 0 });
		verts.put(new float[] { size, 0, 0 });
		colours.put(new float[] { 1, 0, 0, 1 });
		colours.put(new float[] { 1, 0, 0, 1 });

		verts.put(new float[] { 0, 0, 0 });
		verts.put(new float[] { 0, size, 0 });
		colours.put(new float[] { 0, 1, 0, 1 });
		colours.put(new float[] { 0, 1, 0, 1 });

		verts.put(new float[] { 0, 0, 0 });
		verts.put(new float[] { 0, 0, size });
		colours.put(new float[] { 0, 0, 1, 1 });
		colours.put(new float[] { 0, 0, 1, 1 });
		verts.flip();
		colours.flip();

		mesh.vertexAttribute.bufferResourceType = BufferDataManagementType.Buffer;
		mesh.vertexAttribute.buffer = verts;

		mesh.colourAttribute.bufferResourceType = BufferDataManagementType.Buffer;
		mesh.colourAttribute.buffer = colours;

		FrameUtils.appendToList(mesh.meshTransformAttribute.data, new Matrix4f().identity());
		mesh.meshTransformAttribute.bufferResourceType = BufferDataManagementType.List;

		mesh.vertexCount = count * 2;
		mesh.name = "axis";
		mesh.modelRenderType = GLDrawMode.Line;

		return mesh;
	}
	

	public Tickable renderGame(CityBuilderMain cityBuilder, GLContext glContext, RayHelper rh, KeyInputSystem keyInput,
			NkContextSingleton nkContext) {

		// ShaderProgram mainProgram = new ShaderProgram();
		ShaderProgram program = ShaderProgramDefinitions.collection._3D_unlit_flat;
		ShaderProgram uiProgram = ShaderProgramDefinitions.collection.UI;

		VPMatrixSystem vpSystem = new VPMatrixSystem();
		VPMatrix vpMatrix = new VPMatrix();

		CameraSpringArm arm = vpMatrix.view.get().springArm;
		arm.addDistance(15f);
		arm.addPitch(45);

		Entity cameraAnchorEntity = new Entity();
		arm.lookAt = () -> cameraAnchorEntity.position;

		vpMatrix.proj.get().window = cityBuilder.window;
		vpMatrix.proj.get().FOV = 90;
		vpMatrix.proj.get().nearPlane = 0.001f;
		vpMatrix.proj.get().farPlane = 10000f;
		vpMatrix.proj.get().orthoAspect = 10f;
		vpMatrix.proj.get().perspectiveOrOrtho = true;
		vpMatrix.proj.get().scale = 1f;
		vpSystem.projSystem.update(vpMatrix.proj.get()); 

		ViewMatrices viewMatrices = new ViewMatrices();
		FrameUtils.calculateProjectionMatrixPerspective(cityBuilder.window.bounds.width, cityBuilder.window.bounds.height, 90, 0.001f, 1000f, viewMatrices.projectionMatrix);
		Matrix4f orthoMatrix = FrameUtils.calculateProjectionMatrixOrtho(cityBuilder.window.bounds.width, cityBuilder.window.bounds.height, 1, 10.0f, 0.0125f, 10000.0f, new Matrix4f());

		cityBuilder.window.events.postCreation.add(() -> {


			ShaderProgramSystem.create(glContext, ShaderProgramDefinitions.collection._3D_unlit_flat);
			ShaderProgramDefinitions.collection.setupMatrixUBO(glContext, ShaderProgramDefinitions.collection._3D_unlit_flat);

			ShaderProgramSystem.create(glContext, ShaderProgramDefinitions.collection.UI);
			
			ShaderProgramDefinitions.collection.setupUIProjectionMatrixUBO(glContext, ShaderProgramDefinitions.collection.UI);
			ShaderProgramDefinitions.collection.updateUIProjectionMatrix(orthoMatrix);
			cityBuilder.gameUI.init(cityBuilder.window.getID(), cityBuilder.window.nkContext.context, ()->orthoMatrix);

			cityBuilder.window.callbacks.scrollCallbackSet.add(cityBuilder.gameUI.scrollCallback);
			cityBuilder.window.callbacks.mouseButtonCallbackSet.add(cityBuilder.gameUI.mouseButtonCallback);
			cityBuilder.window.callbacks.cursorPosCallbackSet.add(cityBuilder.gameUI.cursorposCallback);

			cityBuilder.gameUI.setupCompas(glContext, uiProgram);
			cityBuilder.gameUI.setupCompasLine(glContext, uiProgram);

			ShaderProgram debugProgram = ShaderProgramDefinitions.collection._3D_unlit_flat;
			ShaderProgramSystem.create(glContext, debugProgram);
			ShaderProgramDefinitions.collection.setupMatrixUBO(glContext, debugProgram, program);
			ShaderProgramDefinition_3D_unlit_flat.Mesh axis = drawAxis(100);
			ShaderProgramSystem.loadVAO(glContext, debugProgram, axis);

		});

		vpSystem.preMult(vpMatrix);

		return (time) -> {
			//vpSystem.projSystem.update(vpMatrix.proj.getWrapped());
			vpSystem.viewSystem.update(vpMatrix.view.get());
			vpSystem.preMult(vpMatrix);

			//ShaderProgramDefinitions.collection.writeVPFMatrix(null, null, null, vpMatrix.cache);
			viewMatrices.viewMatrix.set(vpMatrix.view.get().cache);
			viewMatrices.viewMatrixUpdate.set(true);
			ShaderProgramDefinitions.collection.writeVPMatrix(viewMatrices);
			// TODO change line thickness
			GL46.glLineWidth(3f);
			GL46.glPointSize(3f);
			keyInput.tick(cityBuilder.window.mousePressTicks, cityBuilder.window.mousePressMap, time.nanos);
			keyInput.tick(cityBuilder.window.keyPressTicks, cityBuilder.window.keyPressMap, time.nanos);
			if (cityBuilder.level != null) {
				// TODO better testing for if mouse controls should be enabled. eg when over a gui


				Vector4f mousePos = CursorHelper.getMouse(SpaceState.Eye_Space, cityBuilder.window.getID(), cityBuilder.window.bounds.width, cityBuilder.window.bounds.height, orthoMatrix, null);


				cityBuilder.gameUI.handlePanningTick(cityBuilder.window, mousePos, vpMatrix.view.get(), cameraAnchorEntity);
				cityBuilder.gameUI.handlePitchingTick(cityBuilder.window, mousePos, arm);
				cityBuilder.gameUI.handleScrollingTick(arm);

				long mouseLeftClick = cityBuilder.window.mousePressTicks.getOrDefault(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0l);
				if (mouseLeftClick > 0) {
					Vector4f cursorRay = CursorHelper.getMouse(SpaceState.World_Space, cityBuilder.window.getID(), vpMatrix.proj.get().cache, vpMatrix.view.get().cache);
					rh.update(new Vector3f(cursorRay.x, cursorRay.y, cursorRay.z), new Vector3f(vpMatrix.view.get().position), cityBuilder);
				}

				// TODO level clear colour
				cityBuilder.window.clearColour.set(0f, 0f, 0f, 0f);
				preRender(cityBuilder.level, vpMatrix, glContext, program);
				cityBuilder.gameUI.preRenderMouseUI(cityBuilder.window, uiProgram, rh);

				// MeshExt mesh = new MeshExt();
				// bakeChunk(level.chunks.get(new Vector2i()), mesh);
				// ShaderProgramSystem.loadVAO(program, mesh.mesh);

			}

			ShaderProgramSystem.tryRender(program);
			ShaderProgramSystem.tryRender(ShaderProgramDefinitions.collection._3D_unlit_flat);
			ShaderProgramSystem.tryRender(uiProgram);
			// this is the cube test rendering program
			// ShaderProgramSystem.render(mainProgram);
			return false;
		};
	}
}
