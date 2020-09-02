package com.meeple.backend.game.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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
import com.meeple.backend.game.world.World.Region;
import com.meeple.backend.game.world.features.TreeFeature;
import com.meeple.backend.noise.CircleNoise;
import com.meeple.backend.noise.MultiCircleNoise;
import com.meeple.backend.noise.NoiseStack;
import com.meeple.backend.noise.OctavedNoise;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.FrameUtils.GridMovement;
import com.meeple.temp.IslandOrig.IslandSize;

public class World extends EventHandler {

	public static final int TerrainSize = 4;//100;
	public static final int TerrainSampleSize = 4;//100;
	public static final int TerrainVertexCount = TerrainSampleSize - 1;//128;
	public static final float TerrainHeightScale = 10f;
	/**
	 * this number squared is how many terrains per region
	 */
	public static final int RegionSize = 4;
	public static final float RegionalWorldSize = RegionSize * TerrainSize;
	private static final float SampleScale = TerrainSampleSize / TerrainSize;
	private static final float gravityDamageScale = 1f;

	public class Region {
		public Map<Vector2i, Terrain> terrains;
		protected float minZ, maxZ;
		protected AtomicBoolean hasUpdated = new AtomicBoolean(false);
	}

	public class TerrainStorage {

		private Supplier<Map<Vector2i, Terrain>> regionSupplier = new CollectionSuppliers.MapSupplier<>();
		public Map<Vector2i, Region> terrains = new HashMap<>();

		public Terrain getTerrain(float worldX, float worldY) {

			Vector2i regionIndex = getRegionIndex(worldX, worldY);
			Region region = terrains.get(regionIndex);
			if (region != null) {
				Vector2i terrainIndex = getTerrainIndex(worldX, worldY);
				return region.terrains.get(terrainIndex);
			}
			return null;
		}

		public Terrain getTerrain(Vector2i terrainIndex) {
			Region region = terrains.get(getRegionIndex(terrainIndex.x, terrainIndex.y));
			if (region != null) {
				return region.terrains.get(getTerrainIndex(terrainIndex.x, terrainIndex.y));
			}
			return null;
		}

		public Vector2i getTerrainIndex(float worldX, float worldY) {
			return new Vector2i(FrameUtils.actualIndex((int) worldX, (int) World.TerrainSize), FrameUtils.actualIndex((int) worldY, (int) World.TerrainSize));
		}

		public Vector2i getRegionIndex(float worldX, float worldY) {
			return new Vector2i(FrameUtils.actualIndex((int) worldX, (int) (World.TerrainSize * World.RegionSize)), FrameUtils.actualIndex((int) worldY, (int) (World.TerrainSize * World.RegionSize)));
		}

		public Vector2i getRegionIndex(Vector2i terrainIndex) {
			return new Vector2i(FrameUtils.actualIndex((int) terrainIndex.x, (int) World.RegionSize), FrameUtils.actualIndex((int) terrainIndex.y, (int) World.RegionSize));

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
			Region region = terrains.get(key);
			if (force || region == null) {

				region = new Region();
				region.terrains = regionSupplier.get();
				for (int _x = 0; _x < World.RegionSize; _x++) {
					for (int _y = 0; _y < World.RegionSize; _y++) {
						int x = _x + (regionX * World.RegionSize);
						int y = _y + (regionY * World.RegionSize);
						Vector2i terrPos = new Vector2i(x, y);
						Terrain terr = generateNewTerrain(terrPos.x, terrPos.y);
						region.terrains.put(new Vector2i(_x, _y), terr);
					}
				}
				World.this.sendEventAsync(new RegionGenerationEvent(key, region));
				terrains.put(key, region);
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
			Region region = terrains.get(key);
			boolean anyUpdates = false;
			if (region != null) {
				Region updated = new Region();
				updated.terrains = regionSupplier.get();
				for (int _x = 0; _x < World.RegionSize; _x++) {
					for (int _y = 0; _y < World.RegionSize; _y++) {
						int x = _x + (regionX * World.RegionSize);
						int y = _y + (regionY * World.RegionSize);
						Vector2i terrPos = new Vector2i(x, y);
						Terrain terrain = region.terrains.get(terrPos);
						if (terrain == null) {
							terrain = generateNewTerrain(x, y);
							updated.terrains.put(new Vector2i(_x, _y), terrain);
							anyUpdates |= true;
						}
					}
				}
				if (anyUpdates) {
					World.this.sendEventAsync(new RegionGenerationEvent(key, updated));
					region.terrains.putAll(updated.terrains);
					terrains.put(key, region);
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

			for (int x = 0; x < World.TerrainSampleSize; x++) {
				for (int y = 0; y < World.TerrainSampleSize; y++) {
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
						terrain.tiles[x][y].worldX = (int) terrain.worldX + x;
						terrain.tiles[x][y].worldY = (int) terrain.worldY + y;

						break;

					}

					}
					terrain.minZ = Math.min(terrain.minZ, terrain.tiles[x][y].height);
					terrain.maxZ = Math.max(terrain.maxZ, terrain.tiles[x][y].height);

				}
				//				System.out.println();
			}
			//			System.out.println();

			int a = 0;
			if (a == 0) {

			}
		}

		public void setTile(int worldX, int worldY, TerrainSampleInfo tileInfo) {
			/*
						Region region1 = world.getStorage().terrains.get(new Vector2i(1, 1));
						Region region2 = world.getStorage().terrains.get(new Vector2i(1, 2));
						Terrain terrain1 = region1.terrains.get(new Vector2i(0,3));
						Terrain terrain2 = region2.terrains.get(new Vector2i(0, 0));
			
						TerrainSampleInfo tsi1 = terrain1.tiles[y][terrain1.tiles.length - 1];
						TerrainSampleInfo tsi2 = terrain2.tiles[y][0];
						System.out.println(tsi1.height == tsi2.height);*/

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

		terrainStorage.generateRegion(1, 0, true);
		terrainStorage.generateRegion(1, 1, true);
		terrainStorage.generateRegion(1, 2, true);

		Vector2i pos = new Vector2i();
		boolean flip = true;// leave alone
		GridMovement move = GridMovement.RIGHT;
		for (float range = 0; range < 3;) {

			for (int i = 0; i < range; i++) {

				//				terrainStorage.generateRegion(pos.x, pos.y, true);
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

	public TerrainSampleInfo getTileAt(float x, float y) {
		Terrain terrain = terrainStorage.getTerrain(x, y);
		int tileX = (int) (x - terrain.worldX);
		int tileY = (int) (y - terrain.worldY);
		return terrain.tiles[tileX][tileY];
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
