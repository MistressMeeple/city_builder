package com.meeple.citybuild.client.render;

import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
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
import com.meeple.citybuild.server.WorldGenerator.TerrainType;
import com.meeple.shared.RayHelper;
import com.meeple.shared.Tickable;
import com.meeple.shared.frame.CursorHelper;
import com.meeple.shared.frame.CursorHelper.SpaceState;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.KeyInputSystem;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferDataManagementType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.RenderableVAO;
import com.meeple.shared.frame.OGL.ShaderProgramSystem;
import com.meeple.shared.frame.camera.Camera;
import com.meeple.shared.frame.camera.CameraSpringArm;
import com.meeple.shared.frame.camera.CameraSystem;
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

				if (chunk.rebake.get()) {
					logger.debug("rebake");
					if(m!=null && m instanceof ShaderProgramDefinition_3D_unlit_flat.Mesh){
						ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat.Mesh m2 = (ShaderProgramDefinition_3D_unlit_flat.Mesh) m;
						rebakeChunkV2(m2.vertexAttribute, m2.colourAttribute, m2.elementAttribute, chunk);
						m2.vertexCount = m2.elementAttribute.data.size();
						chunk.rebake.set(false);
					}
				}
				if (m == null) {
					m = ShaderProgramDefinitions.collection._3D_unlit_flat.createMesh();
					bakeChunkV2((ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat.Mesh) m, chunkPos, chunk);
					logger.debug("bake");
					//rebakeChunkV2((ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat.Mesh) m, chunk);

					ShaderProgramSystem.loadVAO(glContext, program, m);
					m.visible = false;
					currentlyVisibleChunks.add(loc);
					baked.put(chunk, m);
				}

			}
		}
	}


	public Map<Chunk, RenderableVAO> baked = new CollectionSuppliers.MapSupplier<Chunk, RenderableVAO>().get();
	public Set<Chunk> needsToBeBaked = new CollectionSuppliers.SetSupplier<Chunk>().get();

	private Vector3f calculateFromVecI(Vector3i key, int subdivisions){
		float x = ((float) key.x / (float) subdivisions) * LevelData.tileSize;
		float y = ((float) key.y / (float) subdivisions) * LevelData.tileSize;
		float z = ((float) key.z / 50f);
		Vector3f vertex = new Vector3f(x, y, z);
		return vertex;
	}

	private List<Integer> processFace(Map<Vector3i, Integer> face, Vector2f mid){
		ArrayList<Vector3i> sortedList = new ArrayList<>(face.keySet());
		Collections.sort(sortedList, (e1, e2) ->
			FrameUtils.rotationCompare2D(e2.x, e2.y, mid.x,mid.y,e1.x,e1.y, -45f)
		);
		List<Integer> faceIndicies = new ArrayList<>();
		boolean flip = false;
		while(sortedList.size() >= 3)
		{
			Vector3i a = sortedList.remove(0);
			Vector3i b = sortedList.get(0);
			Vector3i c = sortedList.get(sortedList.size() -1);
			faceIndicies.add(face.get(a));
			if(flip)
				faceIndicies.add(face.get(b));
			faceIndicies.add(face.get(c));
			if(!flip)
				faceIndicies.add(face.get(b));
			flip = !flip;
		}
		return faceIndicies;
	}

	private void bakeChunkV2(ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat.Mesh m2, Vector3f chunkPosition, Chunk chunk) {
		FrameUtils.appendToList(m2.meshTransformAttribute.data, new Matrix4f().translate(chunkPosition));
		m2.modelRenderType = GLDrawMode.Triangles;
		m2.vertexCount = 0;
		m2.colourAttribute.instanced = false;
		m2.index = new WeakReference<ShaderProgram.BufferObject>(m2.elementAttribute);
	}

	private void rebakeChunkV2(Attribute vertexAttribute, Attribute colourAttribute, BufferObject elementAttribute, Chunk chunk) {
		//Clear
		{
			for(BufferObject buffer : new BufferObject[]{vertexAttribute, colourAttribute, elementAttribute}){
				buffer.data.clear();
			}
		}


		Map<TerrainType, Map<Vector3i, Integer>> visibleVerticesByTileType = new HashMap<>();
		int runningFaceIndex = 0;

		final int subdivisions = 1;
		for(int x = 0; x < LevelData.chunkSize ; x++ ){
			for(int y = 0; y < LevelData.chunkSize ; y++ ){
				TerrainType tile = chunk.tiles[x][y].terrain;
				if(tile != TerrainType.Empty){
					Map<Vector3i, Integer> visibleVertices = visibleVerticesByTileType.get(tile);
					if(visibleVertices == null){
						visibleVertices = new CollectionSuppliers.MapSupplier<Vector3i,Integer>().get();
						visibleVerticesByTileType.put(tile, visibleVertices);
					}


					Map<Vector3i, Integer> face = new CollectionSuppliers.MapSupplier<Vector3i, Integer>().get();


					for(int iy = 0; iy < subdivisions + 1; iy++){
						for(int ix = 0 ; ix < subdivisions + 1; ix++){
							//only the outside edges, dont need middle points
							if( ix == 0 || ix == subdivisions || iy == 0 || iy == subdivisions ){
								Vector3i v = new Vector3i(x * subdivisions + ix, y * subdivisions + iy, chunk.tiles[x][y].height);
								Vector3f vertex = calculateFromVecI(v, subdivisions);

								//get face index, or set it if it didnt have one
								Integer value = visibleVertices.get(v);
								if(value == null){
									value = runningFaceIndex;
									runningFaceIndex += 1;
									visibleVertices.putIfAbsent(v, value);
									//if it didnt have one, we can put it into the data array too
									FrameUtils.appendToList(vertexAttribute.data, vertex);
									Vector4f colour = new Vector4f(1, 0, 0, 1);
									switch (tile) {
										case Empty:
											break;
										case Grass:
											colour = new Vector4f(0.1f, 0.7f, 0.1f, 1f);
											break;
										case Water:
											colour = new Vector4f(0.1f, 0f, 0.7f, 1f);
											break;
										default:
											break;

									}
									FrameUtils.appendToList(colourAttribute.data, colour);
								}
								face.put(v, value);

							}
						}
					}
					Vector2f mid = new Vector2f(x * subdivisions, y * subdivisions).add((float) subdivisions / 2f, (float) subdivisions / 2);
					List<Integer> faceIndicies = processFace(face, mid);
					elementAttribute.data.addAll(faceIndicies);
				}
			}
		}
		//FrameUtils.appendToList(meshNormalMatrixAttribute.data, new Matrix4f());
		//update
		{
			for(BufferObject buffer : new BufferObject[]{vertexAttribute, colourAttribute, elementAttribute}){
				buffer.update.set(true);
			}
		}
	}

	Set<Vector2i> currentlyVisibleChunks = new CollectionSuppliers.SetSupplier<Vector2i>().get();

	private void onCameraChange(Vector3f cameraPosition, Matrix4f viewFrustrum, LevelData level) {

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
			RenderableVAO chunk = baked.get(GameManager.getChunk(
				level,
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
								if(!searched.contains(next) && level.chunks.containsKey(next)){
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

	public Tickable renderGame(CityBuilderMain cityBuilder, GLContext _glContext, RayHelper rh, KeyInputSystem keyInput) {

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

		cityBuilder.window.events.postCreation.add((glContext) -> {

			ShaderProgramSystem.create(glContext, ShaderProgramDefinitions.collection._3D_unlit_flat);
			ShaderProgramDefinitions.collection.setupMatrixUBO(glContext, ShaderProgramDefinitions.collection._3D_unlit_flat);

			ShaderProgramSystem.create(glContext, ShaderProgramDefinitions.collection.UI);

			ShaderProgramDefinitions.collection.setupUIProjectionMatrixUBO(glContext, ShaderProgramDefinitions.collection.UI);
			ShaderProgramDefinitions.collection.updateUIProjectionMatrix(orthoMatrix);

			ShaderProgram debugProgram = ShaderProgramDefinitions.collection._3D_unlit_flat;
			ShaderProgramSystem.create(glContext, debugProgram);
			ShaderProgramDefinitions.collection.setupMatrixUBO(glContext, debugProgram, program);
			ShaderProgramDefinition_3D_unlit_flat.Mesh axis = drawAxis(1);
			ShaderProgramSystem.loadVAO(glContext, debugProgram, axis);

			cityBuilder.window.callbacks.scrollCallbackSet.add(cityBuilder.gameUI::GLFWScrollCallbackI);
			cityBuilder.window.callbacks.mouseButtonCallbackSet.add(cityBuilder.gameUI::GLFWMouseButtonCallbackI);
			cityBuilder.window.callbacks.cursorPosCallbackSet.add(cityBuilder.gameUI::GLFWCursorPosCallback);

			cityBuilder.gameUI.init(cityBuilder.window.getID(), cityBuilder.window.nkContext.context, () -> orthoMatrix);
			cityBuilder.gameUI.setupCompas(glContext, uiProgram);
			cityBuilder.gameUI.setupCompasLine(glContext, uiProgram);


			ShaderProgramSystem.create(glContext, ShaderProgramDefinitions.collection._3D_lit_flat);
			ShaderProgramSystem.findAllUniforms(ShaderProgramDefinitions.collection._3D_lit_flat);

		});


		return (glContext, time) -> {
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

				onCameraChange(camera.springArm.lookAt.get(), viewMatrices.viewProjectionMatrix, cityBuilder.level);

				Vector4f mousePos = CursorHelper.getMouse(SpaceState.Eye_Space, cityBuilder.window.getID(), cityBuilder.window.bounds.width, cityBuilder.window.bounds.height, orthoMatrix, null);
				cityBuilder.gameUI.handlePanningTick(cityBuilder.window, mousePos, camera, cameraAnchorEntity);
				cityBuilder.gameUI.handlePitchingTick(cityBuilder.window, mousePos, arm);
				cityBuilder.gameUI.handleScrollingTick(arm);

				/*long mouseLeftClick = cityBuilder.window.mousePressTicks.getOrDefault(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0l);
				if (mouseLeftClick > 0) {
					Vector4f cursorRay = CursorHelper.getMouse(SpaceState.World_Space, cityBuilder.window.getID(),viewMatrices.projectionMatrix, viewMatrices.viewMatrix);
					rh.update(new Vector3f(cursorRay.x, cursorRay.y, cursorRay.z), new Vector3f(camera.position), cityBuilder.level);
				}*/
				Vector4f cursorRay = CursorHelper.getMouse(SpaceState.World_Space, cityBuilder.window.getID(),viewMatrices.projectionMatrix, viewMatrices.viewMatrix);
				rh.update(new Vector3f(cursorRay.x, cursorRay.y, cursorRay.z), new Vector3f(camera.position), cityBuilder.level);

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
