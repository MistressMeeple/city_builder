package com.meeple.backend.game.world;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.AABBf;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class Terrain {

	protected final int chunkX, chunkY;
	protected final float worldX, worldY;
	protected final Vector3f[] bounds;
	protected final Set<TerrainFeature> features = new HashSet<>();
	public final TerrainSampleInfo[][] tiles;

	private transient final AtomicBoolean needsRemesh = new AtomicBoolean(false);
	protected float minZ, maxZ;

	public Terrain(int gridX, int gridY) {
		this.chunkX = gridX;
		this.chunkY = gridY;
		this.worldX = gridX * World.TerrainSize;
		this.worldY = gridY * World.TerrainSize;

		this.bounds = new Vector3f[] {
			new Vector3f(
				worldX,
				worldY,
				0),
			new Vector3f(
				worldX + World.TerrainSize,
				worldY + World.TerrainSize,
				0) };
		tiles = new TerrainSampleInfo[World.TerrainSampleSize + 1][World.TerrainSampleSize + 1];

	}

	/**
	 * Returns a vector representation of the X and Y position within the region. <br>
	 * Altering this has <b>no</b> effect on the terrain.
	 * @return Vector2i holding terrains position in the region
	 */
	public Vector2i getChunkPos() {
		return new Vector2i(chunkX, chunkY);
	}

	/**
	 * Returns this terrains minimum world coordinates (starting from tile [0,0])<br>
	 * Altering this has <b>no</b> effect on the terrain.  
	 * @return Vector2f holding the world coordinates
	 */
	public Vector2f getWorldPos() {
		return new Vector2f(worldX, worldY);
	}

	/**
	 * Returns this terrains minimum and maximum world coordinates<br>
	 * Altering this has <b>no</b> effect on the terrain.  
	 * @return AABBf holding the AAB holding min and max world coordinates of this terrain
	 */
	public AABBf getAABB() {
		return new AABBf(bounds[0], bounds[1]);
	}

	/**
	 * Check whether or not the terrain needs to be remeshed. This is normally because a terrain's tiles have changed  
	 * @return 
	 */
	public boolean needsRemesh() {
		return needsRemesh.get();
	}

	/**
	 * Sets the needsRemesh flag to the parameter
	 * @param set - true if it needs a remesh or false if not
	 */
	public void needsRemesh(boolean set) {
		needsRemesh.lazySet(set);
	}


}
