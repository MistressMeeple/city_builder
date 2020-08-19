package com.meeple.display.views.WorldBuilding;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.lwjgl.util.par.ParShapes;
import org.lwjgl.util.par.ParShapesMesh;

import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.game.world.TerrainSampleInfo.TerrainType;
import com.meeple.backend.game.world.features.ParShapeMeshI;
import com.meeple.display.views.WorldBuildingView;
import com.meeple.shared.Direction2D;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.IndexBufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;

/**
 * A helper class to manage the terrain meshes for the {@link WorldBuildingView} class.
 * Optimise by creating mesh per terrain set like in minecraft
 * @author Megan
 *
 */

public class WorldBuildingTerrainMeshHelper {

	private Map<TerrainType, Mesh> terrainMeshes = new HashMap<>();
	private Map<Direction2D, Mesh> borderMeshes = new HashMap<>();

	/**
	 * This sets up the meshes and loads them into the gl context and appropriate shader program. 
	 * @param glContext
	 */
	public void setup(GLContext glContext) {

		// load meshes
		ParShapeMeshI randomisedPlane = new ParShapeMeshI() {

			@Override
			public ParShapesMesh generate() {
				ParShapesMesh topPlane = ParShapes.par_shapes_create_plane(3, 3);
				ParShapes.par_shapes_translate(topPlane, -0.5f, -0.5f, 0);
				float scaleXY = 0.9f;// 1f;
				ParShapes.par_shapes_scale(topPlane, scaleXY, scaleXY, 1);
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
			Mesh grass_var_1;
			grass_var_1 = randomisedPlane.convert(randomisedPlane.generate());
			grass_var_1.getAttribute(ShaderPrograms.colourAtt.name).data(new float[] { 0, 1, 0, 1 });
			grass_var_1.getAttribute(ShaderPrograms.transformAtt.name).data(new Matrix4f().get(new float[16]));
			ShaderProgramSystem2.loadVAO(glContext, ShaderPrograms.Program._3D_Unlit_Flat.program, grass_var_1);
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
			ShaderProgramSystem2.loadVAO(glContext, ShaderPrograms.Program._3D_Unlit_Flat.program, slantedSide_dir);

			borderMeshes.put(direction, slantedSide_dir);
		}
	}

	/**
	 * This iterates over the tiles and checks which meshes to render. including the border meshes. 
	 * @param map
	 * @param terrainMeshInstance
	 * @param borders
	 * @param newBufferSupplier
	 */
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

	/**
	 * This goes through the tiles and checks which meshes they will be using. 
	 * This resets the internal cache and ideally will only be called when the tile set has changed. 
	 * @param mapTest
	 */
	public void preRender(TerrainType[][] mapTest) {

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

	public void render() {

		for (Entry<TerrainType, Mesh> entry : terrainMeshes.entrySet()) {
			ShaderProgramSystem2.tryFullRenderMesh(entry.getValue());
		}
		for (Direction2D direction : Direction2D.values()) {
			ShaderProgramSystem2.tryFullRenderMesh(borderMeshes.get(direction));
		}
	}
}
