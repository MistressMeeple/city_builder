package com.meeple.display.views.WorldBuilding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joml.Vector2i;

import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.game.world.Terrain;
import com.meeple.backend.game.world.TerrainSampleInfo.TerrainType;
import com.meeple.shared.Direction2D;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;

public class WorldBuildingTerrainGenerator {

	private static Logger logger = Logger.getLogger(WorldBuildingTerrainGenerator.class);
	/**
	 * This is the width of the entire terrain
	 */
	private int terrainWorldSize = 100;
	/**
	 * This number squared is how many vertices per terrain. Vertex count per side
	 */
	private int terrainVertexCount = 128;

	private Map<Terrain, Float[][]> terrainHeightCache = new HashMap<>();
	private Map<Terrain, Boolean> shouldRebuild = new HashMap<>();

	private void setHeight(Terrain terrain, int x, int y, float height) {
		Float[][] retrieve = terrainHeightCache.get(terrain);
		if (retrieve == null) {
			retrieve = new Float[terrainVertexCount][terrainVertexCount];
		}
		retrieve[x][y] = height;
		terrainHeightCache.put(terrain, retrieve);
		shouldRebuild.put(terrain, true);
	}

	private void setHeight(Terrain terrain, Float[][] heights) {
		terrainHeightCache.put(terrain, heights);
		shouldRebuild.put(terrain, true);
	}

	private Mesh rebuild(Terrain terrain) {
		Mesh mesh = new Mesh();
		return mesh;
	}

	private Mesh initMesh(Mesh mesh) {
		mesh.addAttribute(ShaderPrograms.vertAtt.build());
		mesh.addAttribute(ShaderPrograms.transformAtt.build());
		
		return mesh;
	}

	public void setup(Random random, TerrainType[][] mapTest) {

		int totalTiles = (mapTest.length * mapTest[0].length);
		int half = totalTiles / 2;

		int quater = half / 2;
		int max = half + random.nextInt(quater);
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
			int x = seedMinX + random.nextInt(seedMaxX);
			int y = seedMinY + random.nextInt(seedMaxY);
			Vector2i coord = new Vector2i(x, y);
			toSearch.add(coord);
		}
		toSearch.add(new Vector2i(mapTest.length / 2, mapTest[0].length / 2));
		for (int i = 0; i < max; i++) {
			Collections.shuffle(toSearch, random);
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
							if (i > random.nextInt(max) && random.nextFloat() < lakeChance) {
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
				int lakeSize = random.nextInt(15);
				for (int i = 0; i < lakeSize; i++) {
					Collections.shuffle(toSearchLake, random);
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
}
