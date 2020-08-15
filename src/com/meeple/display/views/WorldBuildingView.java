package com.meeple.display.views;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkColorf;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.util.par.ParShapes;
import org.lwjgl.util.par.ParShapesMesh;

import com.meeple.backend.Client;
import com.meeple.backend.FrameTimings;
import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.ShaderPrograms.Program;
import com.meeple.backend.entity.ModelManager;
import com.meeple.backend.game.world.TerrainSampleInfo.TerrainType;
import com.meeple.backend.game.world.features.ParShapeMeshI;
import com.meeple.backend.view.FreeFlyCameraController;
import com.meeple.backend.view.VPMatrix;
import com.meeple.backend.view.VPMatrix.CameraKey;
import com.meeple.shared.Direction2D;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.IndexBufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2.ShaderClosable;

public class WorldBuildingView {

	private static Logger logger = Logger.getLogger(WorldBuildingView.class);
	DecimalFormat decimalFormat = new DecimalFormat("0.00");

	private NkColorf background = NkColorf.create().r(0.50f).g(0.09f).b(0.54f).a(1.0f);

	public FreeFlyCameraController playerController = new FreeFlyCameraController();
	private float fov = 60;
	private CameraKey primaryCamera;

	ModelManager tileModels = new ModelManager();

	Mesh grass_var_1;

	Map<TerrainType, Mesh> terrainMeshes = new HashMap<>();
	Map<Direction2D, Mesh> borderMeshes = new HashMap<>();

	TerrainType[][] mapTest = new TerrainType[160][160];

	public void setup(Client client, VPMatrix vpMatrix) {

		primaryCamera = vpMatrix.newCamera();
		// load meshes
		ParShapeMeshI randomisedPlane = new ParShapeMeshI() {

			@Override
			public ParShapesMesh generate() {
				ParShapesMesh topPlane = ParShapes.par_shapes_create_plane(3, 3);
				ParShapes.par_shapes_translate(topPlane, -0.5f, -0.5f, 0);
				ParShapes.par_shapes_scale(topPlane, 0.9f, 0.9f, 1);
				ParShapes.par_shapes_rotate(topPlane, (float) Math.toRadians(180), new float[] { 1, 0, 0 });
				return topPlane;
			}

			@Override
			public Mesh convert(ParShapesMesh meshIn) {
				Mesh result = new Mesh();
				int count = meshIn.npoints() * 3;

				FloatBuffer vertices = meshIn.points(count);
				// TODO: make the random better fit the outer mesh
				if (false) {
					FloatBuffer verticesNew = vertices.duplicate();

					ThreadLocalRandom random = ThreadLocalRandom.current();
					for (int i = 0; i < count; i++) {
						float original = vertices.get(i);
						float updated = original;
						if ((i + 1) % 3 == 0) {
							updated += (random.nextFloat() - 0.5f) * 0.05f;
						}
						verticesNew.put(updated);

					}
					result.addAttribute(ShaderPrograms.vertAtt.build().data(verticesNew));
				} else {
					result.addAttribute(ShaderPrograms.vertAtt.build().data(vertices));
				}

				result.addAttribute(ShaderPrograms.vertAtt.build().data(vertices));
				result.addAttribute(ShaderPrograms.colourAtt.build().instanced(true, 1000));
				result.addAttribute(ShaderPrograms.transformAtt.build());

				int tc = meshIn.ntriangles();
				IndexBufferObject ibo = new IndexBufferObject().bufferUsage(BufferUsage.StaticDraw).data(meshIn.triangles(tc * 3));
				result.index(ibo);
				result.vertexCount = tc * 3 * 3;

				return result;
			}
		};

		{
			grass_var_1 = randomisedPlane.convert(randomisedPlane.generate());
			grass_var_1.getAttribute(ShaderPrograms.colourAtt.name).data(new float[] { 0, 1, 0, 1 });
			grass_var_1.getAttribute(ShaderPrograms.transformAtt.name).data(new Matrix4f().get(new float[16]));
			ShaderProgramSystem2.loadVAO(client.glContext, ShaderPrograms.Program._3D_Unlit_Flat.program, grass_var_1);
			terrainMeshes.put(TerrainType.Ground, grass_var_1);
		}

		float size = 0.5f;
		float heightUpper = 0f;
		float heightLower = -0.5f;
		float size_Outer = 0.75f;
		Map<Direction2D, Float[]> slantedSideVertices = new HashMap<>();
		Map<Direction2D, Float[]> slantedSideColours = new HashMap<>();
		slantedSideVertices.put(Direction2D.UP, new Float[] {
			-size, size, heightUpper,
			-size_Outer, size_Outer, heightLower,
			size_Outer, size_Outer, heightLower,
			size, size, heightUpper
		});
		slantedSideColours.put(Direction2D.UP, new Float[] {
			1f, 1f, 0f, 1f
		});
		slantedSideVertices.put(Direction2D.LEFT, new Float[] {
			size, size, heightUpper,
			size_Outer, size_Outer, heightLower,
			size_Outer, -size_Outer, heightLower,
			size, -size, heightUpper

		});
		slantedSideColours.put(Direction2D.LEFT, new Float[] {
			1f, 1f, 0f, 1f
		});
		slantedSideVertices.put(Direction2D.DOWN, new Float[] {
			size, -size, heightUpper,
			size_Outer, -size_Outer, heightLower,
			-size_Outer, -size_Outer, heightLower,
			-size, -size, heightUpper

		});
		slantedSideColours.put(Direction2D.DOWN, new Float[] {
			1f, 1f, 0f, 1f
		});
		slantedSideVertices.put(Direction2D.RIGHT, new Float[] {
			-size, -size, heightUpper,
			-size_Outer, -size_Outer, heightLower,
			-size_Outer, size_Outer, heightLower,
			-size, size, heightUpper

		});
		slantedSideColours.put(Direction2D.RIGHT, new Float[] {
			1f, 1f, 0f, 1f
		});
		float[] IBO = new float[] {
			0, 1, 2,
			2, 3, 0
		};
		for (Direction2D direction : Direction2D.values()) {
			Mesh slantedSide_dir = new Mesh();

			slantedSide_dir.addAttribute(ShaderPrograms.vertAtt.build().data(slantedSideVertices.get(direction)));
			slantedSide_dir.addAttribute(ShaderPrograms.colourAtt.build().instanced(true, 1000));
			slantedSide_dir.addAttribute(ShaderPrograms.transformAtt.build());

			IndexBufferObject ibo = new IndexBufferObject().bufferUsage(BufferUsage.StaticDraw).data(IBO);
			slantedSide_dir.index(ibo);
			slantedSide_dir.vertexCount = 2 * 3 * 3;

			slantedSide_dir.getAttribute(ShaderPrograms.colourAtt.name).data(slantedSideColours.get(direction));

			float[] matrixArray = new float[32];
			Matrix4f matrix = new Matrix4f();
			matrix.get(matrixArray, 0);
			matrix.translate(2, 0, 0);
			matrix.get(matrixArray, 16);

			slantedSide_dir.getAttribute(ShaderPrograms.transformAtt.name).data(matrixArray);
			slantedSide_dir.renderCount(2);
			ShaderProgramSystem2.loadVAO(client.glContext, ShaderPrograms.Program._3D_Unlit_Flat.program, slantedSide_dir);

			borderMeshes.put(direction, slantedSide_dir);
		}

		playerController.register(client.windowID, client.userInput);
		playerController.operateOn(vpMatrix.getCamera(primaryCamera));
		vpMatrix.getCamera(primaryCamera).setTranslation(mapTest.length / 2, mapTest[0].length / 2, 5);

		Random r = new Random(1);
		int totalTiles = (mapTest.length * mapTest[0].length);
		int half = totalTiles / 2;

		logger.info(vpMatrix.getCamera(primaryCamera).getTranslation(new Vector3f()).toString(decimalFormat));

		int quater = half / 2;
		int max = half + r.nextInt(quater);
		logger.info("Using " + max + " tiles as ground");

		for (int x = 0; x < mapTest.length; x++) {
			for (int y = 0; y < mapTest[x].length; y++) {
				mapTest[x][y] = TerrainType.Void;
			}
		}

		List<Vector2i> toSearch = new ArrayList<>();
		Set<Vector2i> hasSearched = new HashSet<>();
		List<Vector2i> lakeSeeds = new ArrayList<>();
		List<Vector2i> featureSeeds = new ArrayList<>();

		float lakeChance = 0.001f;

		int seedCount = 4;
		int seedMinX = mapTest.length / 4;
		int seedMaxX = mapTest.length / 2;
		int seedMinY = mapTest[0].length / 4;
		int seedMaxY = mapTest[0].length / 2;

		for (int i = 0; i < seedCount; i++) {
			int x = seedMinX + r.nextInt(seedMaxX);
			int y = seedMinY + r.nextInt(seedMaxY);
			Vector2i coord = new Vector2i(x, y);
			toSearch.add(coord);
		}
		toSearch.add(new Vector2i(mapTest.length / 2, mapTest[0].length / 2));
		for (int i = 0; i < max; i++) {
			Collections.shuffle(toSearch, r);
			Vector2i searching = null;
			try {
				searching = toSearch.remove(0);
			} catch (IndexOutOfBoundsException exception) {
				logger.info(toSearch.size() + " " + i);
			}
			hasSearched.add(searching);
			for (Direction2D dir : Direction2D.values()) {
				if (toSearch.contains(new Vector2i(searching).add(dir.x, dir.y))) {
					mapTest[searching.x + dir.x][searching.y + dir.y] = TerrainType.Ground;
					i++;
				}
			}
			mapTest[searching.x][searching.y] = TerrainType.Ground;

			int radius = 2;
			for (int x = -radius; x < radius; x++) {
				for (int y = -radius; y < radius; y++) {
					if (x != 0 && y != 0) {

						Vector2i next = new Vector2i(searching.x + x, searching.y + y);
						if (FrameUtils.inArray(mapTest, next.x, next.y) && !hasSearched.contains(next)) {
							toSearch.add(next);
							if (i > r.nextInt(max) && r.nextFloat() < lakeChance) {
								lakeSeeds.add(next);
							}
						}
					}
				}
			}
		}

		if (false) {
			logger.info(lakeSeeds.size());
			for (Vector2i coord : lakeSeeds) {

				List<Vector2i> toSearchLake = new ArrayList<>();
				Set<Vector2i> hasSearchedLake = new HashSet<>();
				toSearchLake.add(coord);
				int lakeSize = r.nextInt(15);
				for (int i = 0; i < lakeSize; i++) {
					Collections.shuffle(toSearchLake, r);
					Vector2i current = toSearchLake.remove(0);
					mapTest[current.x][current.y] = TerrainType.Water;
					hasSearchedLake.add(current);

					for (Direction2D dir : Direction2D.values()) {
						Vector2i next = new Vector2i(current.x + dir.x, coord.y + dir.y);
						if (!hasSearchedLake.contains(next) && FrameUtils.inArray(mapTest, next.x, next.y)) {
							toSearchLake.add(next);
						}
					}
				}

			}
		}

	}

	private void recursive(List<Vector2i> toSearch, Set<Vector2i> hasSearched, int max, Random shuffler, BiFunction<Vector2i, Set<Vector2i>, Boolean> canSearchCheck) {

		for (int i = 0; i < max; i++) {
			Collections.shuffle(toSearch, shuffler);
			Vector2i current = toSearch.remove(0);
			mapTest[current.x][current.y] = TerrainType.Water;
			hasSearched.add(current);
			for (Direction2D dir : Direction2D.values()) {
				Vector2i next = new Vector2i(current.x + dir.x, current.y + dir.y);
				if (canSearchCheck.apply(next, hasSearched)) {
					toSearch.add(next);
				}

			}
		}

	}

	private void meshCheck(TerrainType[][] map, Map<TerrainType, List<Number>> terrainMeshInstance, Map<Direction2D, List<Number>> borders, Supplier<List<Number>> newBufferSupplier) {

		Matrix4f reusable = new Matrix4f();
		float[] arrTemp = new float[16];
		for (int x = 0; x < map.length; x++) {
			for (int y = 0; y < map[x].length; y++) {
				reusable = reusable.identity();
				TerrainType current = map[x][y];

				List<Number> list = terrainMeshInstance.get(current);
				if (list == null) {
					list = newBufferSupplier.get();
					terrainMeshInstance.put(current, list);
				}

				reusable.translate(x, y, 0);
				reusable.get(arrTemp);
				for (float f : arrTemp) {
					list.add(f);
				}

				switch (current) {
				case Ground:
					// search the cardinals to check if we need to do borders
					for (Direction2D direction : Direction2D.values()) {
						TerrainType search = FrameUtils.getOr(map, x + direction.x, y + direction.y, TerrainType.Void);
						if (search == TerrainType.Void || search == TerrainType.Water) {
							List<Number> borderBuffer = borders.get(direction);
							if (borderBuffer == null) {
								borderBuffer = newBufferSupplier.get();
								borders.put(direction, borderBuffer);
							}
							for (float f : arrTemp) {
								borderBuffer.add(f);
							}

						}
					}
					break;
				case Void:
					break;
				case Water:
					break;
				}

			}
		}
	}

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
//			vpMatrix.getCamera(primaryCamera).rotateX((float) Math.toDegrees(90));
			vpMatrix.getCamera(primaryCamera).translate(-mapTest.length / 2, -mapTest[0].length / 2, -5);

		}

		glClearColor(background.r(), background.g(), background.b(), background.a());
		// NOTE this denotes that GL is using a new frame.
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		{
			Map<TerrainType, List<Number>> terrainMeshInstances = new HashMap<>();
			Map<Direction2D, List<Number>> borders = new HashMap<>();

			meshCheck(mapTest, terrainMeshInstances, borders, () -> new ArrayList<>(16 * 5));

			// grass_var_1.getAttribute(ShaderPrograms.transformAtt.name).data(grassBuffer).update.set(true);
			// grass_var_1.renderCount(grassBuffer.size() / 16);

			for (Entry<TerrainType, List<Number>> entry : terrainMeshInstances.entrySet()) {
				if (entry.getValue() != null) {
					Mesh terrainMesh = terrainMeshes.get(entry.getKey());
					List<Number> list = entry.getValue();
					if (list != null && terrainMesh != null) {
						terrainMesh.renderCount(list.size() / 16);
						terrainMesh.getAttribute(ShaderPrograms.transformAtt.name).data(list).update.set(true);
					}

				}
			}

			for (Entry<Direction2D, List<Number>> entry : borders.entrySet()) {
				Mesh mesh = borderMeshes.get(entry.getKey());
				mesh.renderCount(entry.getValue().size() / 16);
				mesh.getAttribute(ShaderPrograms.transformAtt.name).data(entry.getValue()).update.set(true);

			}

		}
		try (ShaderClosable sc = ShaderProgramSystem2.useProgram(Program._3D_Unlit_Flat.program)) {
			ShaderProgramSystem2.tryFullRenderMesh(grass_var_1);
//			ShaderProgramSystem2.tryFullRenderMesh(slantedSide);

			for (Entry<TerrainType, Mesh> entry : terrainMeshes.entrySet()) {
				ShaderProgramSystem2.tryFullRenderMesh(entry.getValue());
			}
			for (Direction2D direction : Direction2D.values()) {
				ShaderProgramSystem2.tryFullRenderMesh(borderMeshes.get(direction));
			}

		}
	}
}
