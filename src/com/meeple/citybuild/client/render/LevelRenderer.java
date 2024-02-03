package com.meeple.citybuild.client.render;

import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
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
import com.meeple.citybuild.server.GameManager;
import com.meeple.citybuild.server.LevelData;
import com.meeple.citybuild.server.LevelData.Chunk;
import com.meeple.citybuild.server.LevelData.Chunk.Tile;
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
import com.meeple.shared.frame.OGL.ShaderProgram.RenderableVAO;
import com.meeple.shared.frame.OGL.ShaderProgramSystem;
import com.meeple.shared.frame.camera.Camera;
import com.meeple.shared.frame.camera.CameraSpringArm;
import com.meeple.shared.frame.camera.CameraSystem;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.utils.CollectionSuppliers;
import com.meeple.shared.utils.FrameUtils;

public class LevelRenderer {
	public static Logger logger = Logger.getLogger(LevelRenderer.class);

	static class CubeMesh {
		Attribute colourAttrib = new Attribute();
		Attribute translationAttrib = new Attribute();
	}

	public void preRender(LevelData level, GLContext glContext, ShaderProgram program) {

		Set<Entry<Vector2i, Chunk>> set = level.chunks.entrySet();
		synchronized (level.chunks) {
			for (Iterator<Entry<Vector2i, Chunk>> i = set.iterator(); i.hasNext();) {
				Entry<Vector2i, Chunk> entry = i.next();
				Vector2i loc = entry.getKey();
				Chunk chunk = entry.getValue();
				Vector3f chunkPos = new Vector3f(loc.x * LevelData.fullChunkSize, loc.y * LevelData.fullChunkSize, 0);
				RenderableVAO m = baked.get(chunk);
				if (m == null) {
					m = bakeChunk(chunkPos, chunk);
					ShaderProgramSystem.loadVAO(glContext, program, m);
					//TODO until proper frustrum check implemented m.visible = false;
					currentlyVisibleChunks.add(loc);
					baked.put(chunk, m);
				}
				if (chunk.rebake.getAndSet(false)) {
					rebake(chunkPos, chunk, m);
				}

			}
		}
	}


	public Map<Chunk, RenderableVAO> baked = new CollectionSuppliers.MapSupplier<Chunk, RenderableVAO>().get();
	public Set<Chunk> needsToBeBaked = new CollectionSuppliers.SetSupplier<Chunk>().get();

	private void rebake(Vector3f chunkPos, Chunk chunk, RenderableVAO chunkMesh) {
		Attribute meshTransformAttribute, colourAttribute;
		if (chunkMesh instanceof ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat.Mesh) {
			meshTransformAttribute = ((ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat.Mesh) chunkMesh).meshTransformAttribute;
			colourAttribute = ((ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat.Mesh) chunkMesh).colourAttribute;
		} else {
			meshTransformAttribute = chunkMesh.instanceAttributes
					.get(ShaderProgramDefinitions.meshTransform_AttributeName).get();
			colourAttribute = chunkMesh.instanceAttributes.get(ShaderProgramDefinitions.colour_AttributeName).get();
		}
		meshTransformAttribute.data.clear();
		colourAttribute.data.clear();
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
						FrameUtils.appendToList(meshTransformAttribute.data, tilePosition);
						colourAttribute.data.add(colour.x);
						colourAttribute.data.add(colour.y);
						colourAttribute.data.add(colour.z);
						colourAttribute.data.add(colour.w);
						chunkMesh.renderCount += 1;
						break;
					case Water:
						colour = new Vector4f(0.1f, 0f, 0.7f, 1f);
						FrameUtils.appendToList(meshTransformAttribute.data, tilePosition);
						colourAttribute.data.add(colour.x);
						colourAttribute.data.add(colour.y);
						colourAttribute.data.add(colour.z);
						colourAttribute.data.add(colour.w);
						chunkMesh.renderCount += 1;
						break;

				}

			}
		}
		meshTransformAttribute.update.set(true);
		colourAttribute.update.set(true);
	}

	private RenderableVAO bakeChunk(Vector3f chunkPos, Chunk chunk) {
		// MeshExt m = new MeshExt();
		ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat.Mesh m2 = ShaderProgramDefinitions.collection._3D_unlit_flat
				.createMesh();
		m2.colourAttribute.instanced = true;

		// WorldRenderer.setupDiscardMesh3D(m, 4);
		m2.modelRenderType = GLDrawMode.TriangleFan;
		m2.name = "chunk_" + (int) chunkPos.x + "_" + (int) chunkPos.y;
		m2.vertexCount = 4;
		m2.renderCount = 0;

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
		rebake(chunkPos, chunk, m2);
		return m2;
	}

	Set<Vector2i> currentlyVisibleChunks = new CollectionSuppliers.SetSupplier<Vector2i>().get();

	private void onCameraChange(Vector3f cameraPosition, Matrix4f viewFrustrum, GameManager game) {

		FrustumIntersection fi = new FrustumIntersection(viewFrustrum);
		List<Vector2i> toSearch = new CollectionSuppliers.ListSupplier<Vector2i>().get();
		Set<Vector2i> searched = new CollectionSuppliers.SetSupplier<Vector2i>().get();

		//initial search area
		{
			Vector2i start = new Vector2i(GameManager.chunk(cameraPosition.x), GameManager.chunk(cameraPosition.y));
			toSearch.add(start);
			//TODO better search radius start
			//atm just get the current lookAt vector and do a small grid around and flood fill
			//should calculate visible chunk indices based on view frustrum and flood fill with those
			int radi = 3;
			for(int x = -radi; x < radi; x++){
				for(int y = -radi; y < radi; y++){
					Vector2i next = start.add(x, y, new Vector2i());
					toSearch.add(next);
				}
			}
			for(Vector2i visible: currentlyVisibleChunks){
				toSearch.add(visible);
			}
		}

		while(!toSearch.isEmpty()){
			//pop
			Vector2i current = toSearch.remove(0);

			//check
			Vector3f chunkPos = new Vector3f(current.x * LevelData.fullChunkSize, current.y * LevelData.fullChunkSize, 0);
			RenderableVAO chunk = baked.get(game.getChunk(
				chunkPos.add(
					LevelData.fullChunkSize/2,
					LevelData.fullChunkSize/2,
					0,
					new Vector3f()
					)
					));
			int intersectCode  = fi.intersectAab(chunkPos, chunkPos.add(LevelData.fullChunkSize, LevelData.fullChunkSize, LevelData.fullChunkSize, new Vector3f()));

			switch (intersectCode) {

				case FrustumIntersection.INSIDE:
				case FrustumIntersection.INTERSECT:
					//if visible, check neighbours
					if(chunk != null) {
						//push
						for(int x = -1; x < 1; x++){
							for(int y = -1; y < 1; y++){
								Vector2i next = current.add(x, y, new Vector2i());
								if(!searched.contains(next) && game.level.chunks.containsKey(next)){
									toSearch.add(next);
								}
							}
						}
						chunk.visible = true;
						currentlyVisibleChunks.add(current);
					}
					// render chunk
					break;
				case FrustumIntersection.PLANE_NX:
				case FrustumIntersection.PLANE_PX:
				case FrustumIntersection.PLANE_NY:
				case FrustumIntersection.PLANE_PY:
				case FrustumIntersection.PLANE_NZ:
				case FrustumIntersection.PLANE_PZ:
				default:
					if(currentlyVisibleChunks.remove(current)){
						if(chunk!=null){
							chunk.visible = false;
						}
					}
					break;
			}

			searched.add(current);
		}
	}

	private ShaderProgramDefinition_3D_unlit_flat.Mesh drawAxis(int size) {
		ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat.Mesh mesh = ShaderProgramDefinitions.collection._3D_unlit_flat.createMesh();

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

		CameraSystem vpSystem = new CameraSystem();
		//VPMatrix vpMatrix = new VPMatrix();
		Camera camera = new Camera();
		CameraSpringArm arm = camera.springArm;
		arm.addDistance(1f);
		arm.addPitch(30);

		Entity cameraAnchorEntity = new Entity();
		arm.lookAt = () -> cameraAnchorEntity.position;


		ViewMatrices viewMatrices = new ViewMatrices();
		FrameUtils.calculateProjectionMatrixPerspective(cityBuilder.window.bounds.width, cityBuilder.window.bounds.height, 90, 0.001f, 1000f, viewMatrices.projectionMatrix);
		Matrix4f orthoMatrix = FrameUtils.calculateProjectionMatrixOrtho(cityBuilder.window.bounds.width, cityBuilder.window.bounds.height, 1, 10.0f, 0.0125f, 10000.0f, new Matrix4f());

		cityBuilder.window.events.postCreation.add(() -> {

			ShaderProgramSystem.create(glContext, ShaderProgramDefinitions.collection._3D_unlit_flat);
			ShaderProgramDefinitions.collection.setupMatrixUBO(glContext, ShaderProgramDefinitions.collection._3D_unlit_flat);

			ShaderProgramSystem.create(glContext, ShaderProgramDefinitions.collection.UI);

			ShaderProgramDefinitions.collection.setupUIProjectionMatrixUBO(glContext,
					ShaderProgramDefinitions.collection.UI);
			ShaderProgramDefinitions.collection.updateUIProjectionMatrix(orthoMatrix);
			cityBuilder.gameUI.init(cityBuilder.window.getID(), cityBuilder.window.nkContext.context,
					() -> orthoMatrix);

			cityBuilder.window.callbacks.scrollCallbackSet.add(cityBuilder.gameUI.scrollCallback);
			cityBuilder.window.callbacks.mouseButtonCallbackSet.add(cityBuilder.gameUI.mouseButtonCallback);
			cityBuilder.window.callbacks.cursorPosCallbackSet.add(cityBuilder.gameUI.cursorposCallback);

			cityBuilder.gameUI.setupCompas(glContext, uiProgram);
			cityBuilder.gameUI.setupCompasLine(glContext, uiProgram);

			ShaderProgram debugProgram = ShaderProgramDefinitions.collection._3D_unlit_flat;
			ShaderProgramSystem.create(glContext, debugProgram);
			ShaderProgramDefinitions.collection.setupMatrixUBO(glContext, debugProgram, program);
			ShaderProgramDefinition_3D_unlit_flat.Mesh axis = drawAxis(1);
			ShaderProgramSystem.loadVAO(glContext, debugProgram, axis);

		});


		return (time) -> {
			// vpSystem.projSystem.update(vpMatrix.proj.getWrapped());
			vpSystem.update(camera, viewMatrices.viewMatrix);
			viewMatrices.viewMatrixUpdate.set(true);
			ShaderProgramDefinitions.collection.writeVPMatrix(viewMatrices);
			// TODO change line thickness
			GL46.glLineWidth(3f);
			GL46.glPointSize(3f);

			if (cityBuilder.level != null) {
				// TODO better testing for if mouse controls should be enabled. eg when over a
				// gui

				onCameraChange(camera.springArm.lookAt.get(), viewMatrices.viewProjectionMatrix, cityBuilder);

				Vector4f mousePos = CursorHelper.getMouse(SpaceState.Eye_Space, cityBuilder.window.getID(), cityBuilder.window.bounds.width, cityBuilder.window.bounds.height, orthoMatrix, null);
				cityBuilder.gameUI.handlePanningTick(cityBuilder.window, mousePos, camera, cameraAnchorEntity);
				cityBuilder.gameUI.handlePitchingTick(cityBuilder.window, mousePos, arm);
				cityBuilder.gameUI.handleScrollingTick(arm);

				long mouseLeftClick = cityBuilder.window.mousePressTicks.getOrDefault(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0l);
				if (mouseLeftClick > 0) {
					Vector4f cursorRay = CursorHelper.getMouse(SpaceState.World_Space, cityBuilder.window.getID(),
							viewMatrices.projectionMatrix, viewMatrices.viewMatrix);
					rh.update(new Vector3f(cursorRay.x, cursorRay.y, cursorRay.z),
							new Vector3f(camera.position), cityBuilder);
				}

				// TODO level clear colour
				cityBuilder.window.clearColour.set(0f, 0f, 0f, 0f);
				preRender(cityBuilder.level, glContext, program);
				cityBuilder.gameUI.preRenderMouseUI(cityBuilder.window.mousePressTicks, uiProgram, rh);

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
