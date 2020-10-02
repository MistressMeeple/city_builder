package com.meeple.backend.game.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.AABBf;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.meeple.backend.FrameTimings;
import com.meeple.backend.entity.EntityBase;
import com.meeple.backend.events.EventHandler;
import com.meeple.backend.events.RegionGenerationEvent;
import com.meeple.backend.events.TerrainGenerationEvent;
import com.meeple.backend.events.TerrainPopulationEvent;
import com.meeple.backend.game.world.TerrainSampleInfo.TerrainType;
import com.meeple.backend.noise.CircleNoise;
import com.meeple.backend.noise.MultiCircleNoise;
import com.meeple.backend.noise.NoiseStack;
import com.meeple.backend.noise.OctavedNoise;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.FrameUtils.GridMovement;
import com.meeple.temp.IslandOrig.IslandSize;

public class World extends EventHandler {

	public static final int TerrainSize = 200;
	public static final int TerrainSampleSize = 128;
	//	public static final int TerrainVertexCount = TerrainSampleSize - 1;//128;
	private static final float sizePerTile = ((float) TerrainSize / (float) TerrainSampleSize);
	public static final float TerrainHeightScale = 100f;
	/**
	 * this number squared is how many terrains per region
	 */
	public static final int TerrainsPerRegion = 4;
	public static final int RegionalWorldSize = TerrainsPerRegion * TerrainSize;
	private static final float SampleScale = TerrainSampleSize / TerrainSize;
	private static final float gravityDamageScale = 1f;

	public class Region {
		public final Vector2f[] bounds = { new Vector2f(), new Vector2f() };
		public Terrain[][] terrains = new Terrain[TerrainsPerRegion][TerrainsPerRegion];
		protected float minZ, maxZ;

		protected AtomicBoolean needsRemesh = new AtomicBoolean(false);
		protected AtomicBoolean hasUpdated = new AtomicBoolean(false);
	}

	public class TerrainStorage {

		public Map<Vector2i, Region> regions = new HashMap<>();

		public Terrain getTerrain(float worldX, float worldY) {

			Vector2i regionIndex = getRegionIndex(worldX, worldY);
			Region region = regions.get(regionIndex);
			if (region != null) {
				Vector2i terrainIndex = getTerrainIndex(worldX, worldY);
				return region.terrains[terrainIndex.x][terrainIndex.y];
			}
			return null;
		}

		public Vector2i getTerrainIndex(float worldX, float worldY) {
			float modX = worldX % RegionalWorldSize;
			float modY = worldY % RegionalWorldSize;
			//			modX += (sizePerTile / 2);
			//			modY += (sizePerTile / 2);
			float actualX = FrameUtils.actualIndex(modX, World.TerrainSize);
			float actualY = FrameUtils.actualIndex(modY, World.TerrainSize);
			int finalX = (int) actualX;
			int finalY = (int) actualY;
			return new Vector2i(finalX, finalY);
		}

		public Vector2i getTileIndex(float worldX, float worldY) {

			float regionModX = (worldX) % RegionalWorldSize;
			float regionModY = (worldY) % RegionalWorldSize;
			float terrainModX = regionModX % TerrainSize;
			float terrainModY = regionModY % TerrainSize;
			if (terrainModX < TerrainSize - (sizePerTile / 2)) {
				//				terrainModX += +(sizePerTile / 2);
			}
			if (terrainModY < TerrainSize - (sizePerTile / 2)) {
				//				terrainModY += +(sizePerTile / 2);
			}

			//			modX += (sizePerTile / 2);
			//			modY += (sizePerTile / 2);

			float actualX = FrameUtils.actualIndex(terrainModX, World.sizePerTile);
			float actualY = FrameUtils.actualIndex(terrainModY, World.sizePerTile);
			int correctedX = (int) Math.round(actualX);// % World.RegionSize;
			int correctedY = (int) Math.round(actualY);// % World.RegionSize;
			int minX = Math.min(TerrainSampleSize, correctedX);
			int maxX = Math.max(0, minX);

			int minY = Math.min(TerrainSampleSize, correctedY);
			int maxY = Math.max(0, minY);
			int finalX = maxX;
			int finalY = maxY;
			return new Vector2i(finalX, finalY);
		}

		public Region getRegion(float worldX, float worldY) {
			Vector2i regionIndex = getRegionIndex(worldX, worldY);
			Region region = regions.get(regionIndex);
			return region;
		}

		public Vector2i getRegionIndex(float worldX, float worldY) {
			return new Vector2i(FrameUtils.actualIndex((int) worldX, (int) (World.TerrainSize * World.TerrainsPerRegion)), FrameUtils.actualIndex((int) worldY, (int) (World.TerrainSize * World.TerrainsPerRegion)));
		}

		public Vector2i getRegionIndex(Vector2i terrainIndex) {
			return new Vector2i(FrameUtils.actualIndex((int) terrainIndex.x, (int) World.TerrainsPerRegion), FrameUtils.actualIndex((int) terrainIndex.y, (int) World.TerrainsPerRegion));

		}

		/**
		 * Generates the requested region if absent or the force flag is true. <br>
		 * Returns true if generated, false if not
		 * 
		 * @param regionX coord X of requested region
		 * @param regionY coord Y of requested region
		 * @param force   whether or not to force the region to generate (true to force
		 *                gen)
		 * @return true if a new region was created, false if not
		 */
		public boolean generateRegion(int regionX, int regionY, boolean force) {
			Vector2i key = new Vector2i(regionX, regionY);
			Region region = regions.get(key);
			if (force || region == null) {

				region = new Region();

				float region_minX = regionX * World.RegionalWorldSize;
				float region_minY = regionY * World.RegionalWorldSize;
				float region_maxX = (regionX + 1) * World.RegionalWorldSize;
				float region_maxY = (regionY + 1) * World.RegionalWorldSize;

				region.bounds[0].x = region_minX;
				region.bounds[0].y = region_minY;
				region.bounds[1].x = region_maxX;
				region.bounds[1].y = region_maxY;

				for (int _x = 0; _x < World.TerrainsPerRegion; _x++) {
					for (int _y = 0; _y < World.TerrainsPerRegion; _y++) {
						int x = _x + (regionX * World.TerrainsPerRegion);
						int y = _y + (regionY * World.TerrainsPerRegion);
						Vector2i terrPos = new Vector2i(x, y);
						Terrain terr = generateNewTerrain(terrPos.x, terrPos.y);
						region.terrains[_x][_y] = terr;
					}
				}
				region.needsRemesh.set(true);
				World.this.sendEventAsync(new RegionGenerationEvent(key, region));
				regions.put(key, region);
				region.hasUpdated.lazySet(true);
				return true;
			}
			return false;
		}

		/**
		 * Attempts to fix any missing/deleted chunks in an existing region. Returns
		 * true if any have been updated. Use a listener for
		 * {@link RegionGenerationEvent} for a list of the updated chunks
		 * 
		 * @param regionX
		 * @param regionY
		 * @return true if any chunks have been generated, false if no work done
		 */
		public boolean fixChunksInRegion(int regionX, int regionY) {
			Vector2i key = new Vector2i(regionX, regionY);
			Region region = regions.get(key);

			//TODO use a different event for updating a region

			boolean anyUpdates = false;
			if (region != null) {
				Region updated = new Region();
				for (int _x = 0; _x < World.TerrainsPerRegion; _x++) {
					for (int _y = 0; _y < World.TerrainsPerRegion; _y++) {
						int x = _x + (regionX * World.TerrainsPerRegion);
						int y = _y + (regionY * World.TerrainsPerRegion);
						Vector2i terrPos = new Vector2i(x, y);
						Terrain terrain = region.terrains[terrPos.x][terrPos.y];
						if (terrain == null) {
							terrain = generateNewTerrain(x, y);
							updated.terrains[_x][_y] = terrain;
							anyUpdates |= true;
						}
					}
				}
				if (anyUpdates) {
					World.this.sendEventAsync(new RegionGenerationEvent(key, updated));
					for (int x = 0; x < World.TerrainsPerRegion; x++) {
						for (int y = 0; y < World.TerrainsPerRegion; y++) {
							if (updated.terrains[x][y] != null) {
								region.terrains[x][y] = updated.terrains[x][y];
							}
						}
					}
					regions.put(key, region);
					region.hasUpdated.lazySet(true);
					return anyUpdates;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}

		protected Terrain generateNewTerrain(int x, int y) {
			Terrain terrain = new Terrain(x, y);
			generateTiles(terrain);
			World.this.sendEventAsync(new TerrainPopulationEvent(terrain), new TerrainGenerationEvent(terrain));
			return terrain;
		}

		protected void generateTiles(Terrain terrain) {
			//TODO get the edges from the other existing terrains

			for (int x = 0; x < World.TerrainSampleSize + 1; x++) {
				for (int y = 0; y < World.TerrainSampleSize + 1; y++) {
					terrain.tiles[x][y] = new TerrainSampleInfo();
					switch (generatorType) {
					case Flat: {
						terrain.tiles[x][y].height = 0;
						terrain.tiles[x][y].type = TerrainType.Ground;
						break;
					}
					case Random: {

						float normX = (float) x / (float) World.TerrainSampleSize;
						float normY = (float) y / (float) World.TerrainSampleSize;

						float actualX = normX * World.TerrainSize;
						float actualY = normY * World.TerrainSize;

						float sampleX = terrain.worldX + actualX;
						float sampleY = terrain.worldY + actualY;

						//						System.out.print("[" + sampleX + " " + sampleY + "] ");

						terrain.tiles[x][y] = sample(sampleX, sampleY);

						break;

					}

					}
					terrain.minZ = Math.min(terrain.minZ, terrain.tiles[x][y].height);
					terrain.maxZ = Math.max(terrain.maxZ, terrain.tiles[x][y].height);

				}

			}
			terrain.needsRemesh(true);

		}

		public TerrainSampleInfo getTile(float worldX, float worldY) {
			TerrainSampleInfo result = null;
			float worldXFix = worldX;//- (sizePerTile / 2);
			float worldYFix = worldY;// + (sizePerTile / 2);

			Vector2i tileIndex = getTileIndex(worldXFix, worldYFix);

			Vector2i regionIndex = getRegionIndex(worldXFix, worldYFix);
			Region region = regions.get(regionIndex);
			Terrain terrain = null;
			if (region == null) {
				if (tileIndex.x == 0) {
					regionIndex = getRegionIndex(worldX - sizePerTile, worldY);
					region = regions.get(regionIndex);
				}
				if (tileIndex.y == 0) {
					regionIndex = getRegionIndex(worldX, worldY + sizePerTile);
					region = regions.get(regionIndex);
				}
				System.out.println("bo");
			}
			if (region != null) {
				Vector2i terrainIndex = getTerrainIndex(worldXFix, worldYFix);
				try {
					terrain = region.terrains[terrainIndex.x][terrainIndex.y];
				} catch (Exception e) {
					System.out.println("terrain index wrong");
				}

				Vector2i terrainTileIndex = new Vector2i();
				//extract tile index coords
				float x = (float) (worldXFix - terrain.worldX) / sizePerTile;
				float y = (float) (worldYFix - terrain.worldY) / sizePerTile;

				int iX = (int) x;
				int iY = (int) y;
				terrainTileIndex.x = FrameUtils.actualIndex(iX, 1);
				terrainTileIndex.y = FrameUtils.actualIndex(iY, 1);

				terrainTileIndex = tileIndex;

				result = terrain.tiles[terrainTileIndex.x][terrainTileIndex.y];

			}
			return result;

		}

		public void setTile(float worldX, float worldY, TerrainSampleInfo tileInfo) {
			//OPTIMISE possibly by not getting region each check. the neighbourgh still might be in same region

			float worldXFix = worldX;//- (sizePerTile / 2);
			float worldYFix = worldY;// + (sizePerTile / 2);

			Vector2i tileIndex = getTileIndex(worldXFix, worldYFix);

			Vector2i regionIndex = getRegionIndex(worldXFix, worldYFix);
			Region region = regions.get(regionIndex);
			Terrain terrain = null;
			if (region == null) {
				if (tileIndex.x == 0) {
					regionIndex = getRegionIndex(worldX - sizePerTile, worldY);
					region = regions.get(regionIndex);
				}
				if (tileIndex.y == 0) {
					regionIndex = getRegionIndex(worldX, worldY + sizePerTile);
					region = regions.get(regionIndex);
				}
				System.out.println("bo");
			}
			if (region != null) {
				Vector2i terrainIndex = getTerrainIndex(worldXFix, worldYFix);
				try {
					terrain = region.terrains[terrainIndex.x][terrainIndex.y];
				} catch (Exception e) {
					System.out.println("terrain index wrong");
				}

				Vector2i terrainTileIndex = new Vector2i();
				//extract tile index coords
				float x = (float) (worldXFix - terrain.worldX) / sizePerTile;
				float y = (float) (worldYFix - terrain.worldY) / sizePerTile;

				int iX = (int) x;
				int iY = (int) y;
				terrainTileIndex.x = FrameUtils.actualIndex(iX, 1);
				terrainTileIndex.y = FrameUtils.actualIndex(iY, 1);

				terrainTileIndex = tileIndex;

				float oldHeigh = terrain.tiles[terrainTileIndex.x][terrainTileIndex.y].height;
				terrain.tiles[terrainTileIndex.x][terrainTileIndex.y].set(tileInfo);

				terrain.needsRemesh(true);
				System.out.println(terrainTileIndex.x + " " + terrainTileIndex.y + " " + oldHeigh + " " + tileInfo.height);
				if (true) {
					boolean botEdge = terrainTileIndex.y == 0;
					boolean rightEdge = terrainTileIndex.x == 0;

					boolean topEdge = terrainTileIndex.y > World.TerrainSampleSize - 1;
					boolean leftEdge = terrainTileIndex.x > World.TerrainSampleSize - 1;

					//-- == bot == --
					if (botEdge) {

						System.out.println("bot");
						if (terrainIndex.y == 0) {
							region = regions.get(regionIndex.add(0, -1, new Vector2i()));
							terrain = region.terrains[terrainIndex.x][World.TerrainsPerRegion - 1];
							//FIND NEXT REGION
						} else {
							terrain = region.terrains[terrainIndex.x][terrainIndex.y - 1];
						}

						TerrainSampleInfo tsi = terrain.tiles[terrainTileIndex.x][World.TerrainSampleSize];
						tsi.set(tileInfo);
						terrain.needsRemesh(true);
					}

					//-- == left == --
					if (leftEdge) {

						System.out.println("left");
						if (terrainIndex.x + 1 >= TerrainsPerRegion) {
							region = regions.get(regionIndex.add(+1, 0, new Vector2i()));
							terrain = region.terrains[0][terrainIndex.y];
						} else {
							terrain = region.terrains[terrainIndex.x + 1][terrainIndex.y];
						}
						TerrainSampleInfo tsi = terrain.tiles[0][terrainTileIndex.x];
						tsi.set(tileInfo);
						terrain.needsRemesh(true);
					}

					//-- == top == --
					if (topEdge) {
						System.out.println("top");
						if (terrainIndex.y + 1 >= TerrainsPerRegion) {
							region = regions.get(regionIndex.add(0, +1, new Vector2i()));
							terrain = region.terrains[terrainIndex.x][0];
						} else {
							terrain = region.terrains[terrainIndex.x][terrainIndex.y + 1];
						}
						TerrainSampleInfo tsi = terrain.tiles[terrainTileIndex.x][0];
						tsi.set(tileInfo);
						terrain.needsRemesh(true);
					}
					if (rightEdge) {

						System.out.println("right ");
						if (terrainIndex.x == 0) {
							region = regions.get(regionIndex.add(-1, 0, new Vector2i()));
							terrain = region.terrains[0][terrainIndex.y];
						} else {
							terrain = region.terrains[terrainIndex.x - 1][terrainIndex.y];
						}
						TerrainSampleInfo tsi = terrain.tiles[World.TerrainSampleSize][terrainTileIndex.x];
						tsi.set(tileInfo);
						terrain.needsRemesh(true);
					} /*
						if (botEdge && leftEdge) {
						
						System.out.println("bot left");
						boolean shouldGetLeftRegion = terrainIndex.x + 1 >= TerrainsPerRegion;
						boolean shouldGetBotRegion = terrainIndex.y == 0;
						
						if (shouldGetBotRegion && shouldGetLeftRegion) {
							region = regions.get(regionIndex.add(+1, -1, new Vector2i()));
							terrain = region.terrains[0][0];
						} else {
							if (shouldGetLeftRegion) {
								region = regions.get(regionIndex.add(+1, 0, new Vector2i()));
								terrain = region.terrains[0][terrainIndex.y];
							} else if (shouldGetBotRegion) {
								region = regions.get(regionIndex.add(0, -1, new Vector2i()));
								terrain = region.terrains[terrainIndex.x][World.TerrainsPerRegion - 1];
							} else {
						
								terrain = region.terrains[terrainIndex.x - 1][terrainIndex.y - 1];
							}
						}
						TerrainSampleInfo tsi = terrain.tiles[World.TerrainSampleSize][World.TerrainSampleSize];
						tsi.set(tileInfo);
						terrain.needsRemesh(true);
						}
						if (botEdge && rightEdge) {
						System.out.println("bot right");
						boolean shouldGetBotRegion = terrainIndex.y == 0;
						boolean shouldGetRightRegion = terrainIndex.x == 0;
						
						if (shouldGetBotRegion && shouldGetRightRegion) {
							region = regions.get(regionIndex.add(-1, -1, new Vector2i()));
							terrain = region.terrains[0][0];
						} else {
							if (shouldGetBotRegion) {
								region = regions.get(regionIndex.add(0, -1, new Vector2i()));
								terrain = region.terrains[terrainIndex.x][World.TerrainsPerRegion - 1];
							} else if (shouldGetRightRegion) {
								region = regions.get(regionIndex.add(-1, 0, new Vector2i()));
								terrain = region.terrains[0][terrainIndex.y];
							} else {
						
								terrain = region.terrains[terrainIndex.x - 1][terrainIndex.y - 1];
							}
						}
						TerrainSampleInfo tsi = terrain.tiles[World.TerrainSampleSize][World.TerrainSampleSize];
						tsi.set(tileInfo);
						terrain.needsRemesh(true);
						}
						if (topEdge && leftEdge) {
						System.out.println("top left");
						boolean shouldGetLeftRegion = terrainIndex.x + 1 >= TerrainsPerRegion;
						boolean shouldGetTopRegion = terrainIndex.y + 1 >= TerrainsPerRegion;
						
						if (shouldGetLeftRegion && shouldGetTopRegion) {
							region = regions.get(regionIndex.add(+1, +1, new Vector2i()));
							terrain = region.terrains[0][0];
						} else {
							if (shouldGetLeftRegion) {
								region = regions.get(regionIndex.add(+1, 0, new Vector2i()));
								terrain = region.terrains[0][terrainIndex.y];
							} else if (shouldGetTopRegion) {
								region = regions.get(regionIndex.add(0, +1, new Vector2i()));
								terrain = region.terrains[terrainIndex.x][0];
							} else {
						
								terrain = region.terrains[terrainIndex.x + 1][terrainIndex.y + 1];
							}
						}
						TerrainSampleInfo tsi = terrain.tiles[0][0];
						tsi.set(tileInfo);
						terrain.needsRemesh(true);
						}
						if (topEdge && rightEdge) {
						System.out.println("top right");
						boolean shouldGetTopRegion = terrainIndex.y + 1 >= TerrainsPerRegion;
						boolean shouldGetRightRegion = terrainIndex.x == 0;
						
						if (shouldGetTopRegion && shouldGetRightRegion) {
							region = regions.get(regionIndex.add(-1, +1, new Vector2i()));
							terrain = region.terrains[World.TerrainSampleSize][terrainIndex.y];
						} else {
							if (shouldGetTopRegion) {
								region = regions.get(regionIndex.add(0, +1, new Vector2i()));
								terrain = region.terrains[terrainIndex.x][0];
							} else if (shouldGetRightRegion) {
								region = regions.get(regionIndex.add(-1, 0, new Vector2i()));
								terrain = region.terrains[0][terrainIndex.y];
							} else {
						
								terrain = region.terrains[terrainIndex.x - 1][terrainIndex.y + 1];
							}
						}
						TerrainSampleInfo tsi = terrain.tiles[0][World.TerrainSampleSize];
						tsi.set(tileInfo);
						terrain.needsRemesh(true);
						}
						*/
				}

			}
		}

	}

	public enum WorldGeneratorType {
		Flat, Random, Custom;
	}

	protected TerrainStorage terrainStorage = new TerrainStorage();

	public Set<EntityBase> entities = new HashSet<>();
	public float gravity = 100f;

	private NoiseStack terrainHeightGenerator = new NoiseStack();
	private NoiseStack featureGenerator = new NoiseStack();
	private WorldGeneratorType generatorType = WorldGeneratorType.Random;

	public boolean generated = false;;

	public World() {

	}

	public TerrainStorage getStorage() {
		return this.terrainStorage;
	}

	public void generate() {

		/*
		 * int size = 10; for (int x = -size; x < size; x++) { for (int y = -size; y <
		 * size; y++) { Vector2i pos = new Vector2i(x, y); Terrain terrain =
		 * genNewTerrain(x, y); this.terrains.put(pos, terrain);
		 * 
		 * } }
		 */

		terrainStorage.generateRegion(0, 0, true);
		//		terrainStorage.generateRegion(0, 1, true);
		//		terrainStorage.generateRegion(0, 2, true);

		Vector2i pos = new Vector2i();
		boolean flip = true;// leave alone
		GridMovement move = GridMovement.RIGHT;
		for (float range = 0; range < 3;) {

			for (int i = 0; i < range; i++) {

				terrainStorage.generateRegion(pos.x, pos.y, true);
				switch (move) {
				case RIGHT:// right
					pos.x += 1;
					break;
				case DOWN:// down
					pos.y += 1;
					break;
				case LEFT: // left
					pos.x -= 1;
					break;
				case UP: // up
					pos.y -= 1;
					break;
				}
			}
			// NOTE controls.
			{
				range += 0.5f;
				flip = !flip;
				move = move.next();
			}
		}
		generated = true;
	}

	public TerrainSampleInfo sample(float x, float y) {
		TerrainSampleInfo tsi = new TerrainSampleInfo();
		tsi.height = terrainHeightGenerator.sample(x, y);

		if (tsi.height < .25f) {
			tsi.type = TerrainType.Water;
		} else if (tsi.height < 0.3f) {
			tsi.type = TerrainType.Beach;
		} else {
			tsi.type = TerrainType.Ground;
		}
		return tsi;
	}

	public void setGeneratorType(WorldGeneratorType generatorType) {
		this.generatorType = generatorType;
	}

	public void setupGenerator(String seedText) {
		{

			CircleNoise cNoise = new CircleNoise(new Vector2f(), 5000).setCubic(0.5f, -1f, 1f, 0.25f);

			terrainHeightGenerator.addNoise(cNoise);

			MultiCircleNoise sNoise = new MultiCircleNoise().setCubic(0, -0.3f, 1, 0);

			long seed = 0;

			try {
				seed = Long.parseLong(seedText.trim());
			} catch (NumberFormatException e) {
				seed = seedText.hashCode();
				e.printStackTrace();
			}
			Random r = new Random(seed);
			int max = 10 + r.nextInt(20) + r.nextInt(20);
			for (int i = 0; i < max; i++) {
				float x = ((r.nextFloat() * 2) - 1) * (1250 / 2);
				float y = ((r.nextFloat() * 2) - 1) * (1250 / 2);
				int maxSizes = IslandSize.values().length * 10;
				int sizeIndex = r.nextInt(maxSizes - 1) / 10;

				IslandSize size = IslandSize.values()[sizeIndex];

				sNoise.addCircle(i, new Vector4f(x, y, size.pythag * 2f, r.nextFloat() * 2f - 1f));
			}
			terrainHeightGenerator.addNoise(sNoise);
			OctavedNoise oNoise = new OctavedNoise(6f, 500, 0.5f).setCubic(0, 0, 1, 0.5f);
			terrainHeightGenerator.addNoise(oNoise);

		} /*
			 * terrainHeightGenerator.setPresample(new Function<Vector3f, Float>() {
			 * 
			 * @Override public Float apply(Vector3f t) { float in = t.z; if (in < .25f) {
			 * return -.1f; } else if (in < 0.3f) { return 0f; } else { float result = .1f +
			 * (0.5f * (t.z - 0.3f)); return result; }
			 * 
			 * } });
			 */

		// NOTE setup the feature generator
		{
			org.lwjgl.util.par.ParShapes.par_shapes_create_lsystem("", 5, 10);
		}
	}

	public void addEntity(EntityBase entity) {
		this.entities.add(entity);

	}

	public void tick(FrameTimings delta) {
		synchronized (entities) {
			for (Iterator<EntityBase> i = entities.iterator(); i.hasNext();) {
				EntityBase entityBase = i.next();
				Vector3f pos = entityBase.transformation().getTranslation(new Vector3f());
				Vector3f velocity = entityBase.velocity();
				AABBf bounds = entityBase.bounds();

				Vector3f plannedMove = new Vector3f(velocity).mul(delta.deltaSeconds);
				velocity = velocity.sub(plannedMove);

				float groundAtPlanned = terrainHeightGenerator.sample(pos.x + plannedMove.x, pos.y + plannedMove.y, SampleScale, TerrainHeightScale) - (bounds.minZ);

				if (pos.z + plannedMove.z < groundAtPlanned) {
					plannedMove.z = 0;
					velocity.zero();
				}
				if (velocity.lengthSquared() < 0.0001f) {
					velocity.zero();
				}

				entityBase.transformation().translate(plannedMove);

				if (entityBase.useGravity()) {
					if (pos.z + plannedMove.z - (entityBase.gravityPull() * delta.deltaSeconds) > groundAtPlanned) {

						entityBase.incGravityPull(gravity * delta.deltaSeconds);
						entityBase.transformation().translate(0, 0, -entityBase.gravityPull() * delta.deltaSeconds);
					} else {
						entityBase.clearGravityPull();
						float diff = groundAtPlanned - (pos.z + plannedMove.z);
						entityBase.transformation().translate(0, 0, diff);
						entityBase.resetJumps();
						// IF NOT BOUNCY
						velocity.zero();
					}

				}

			}
		}

	}

}
