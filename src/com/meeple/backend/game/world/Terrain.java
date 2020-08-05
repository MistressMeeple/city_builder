package com.meeple.backend.game.world;

import java.util.HashSet;
import java.util.Set;

import org.joml.Vector3f;

public class Terrain {

	protected final int chunkX, chunkY;
	protected final float worldX, worldY;
	protected final Vector3f[] bounds;
	protected final Set<TerrainFeature> features = new HashSet<>();
	protected final TerrainSampleInfo[][] tiles;

	public Terrain(int gridX, int gridY) {
		this.chunkX = gridX;
		this.chunkY = gridY;
		this.worldX = gridX * World.TerrainSize;
		this.worldY = gridY * World.TerrainSize;

		this.bounds = new Vector3f[] {
			new Vector3f(
				worldX,
				0,
				worldY),
			new Vector3f(
				worldX + World.TerrainSize,
				0,
				worldY + World.TerrainSize) };
		 tiles = new TerrainSampleInfo[World.TerrainSize][World.TerrainSize];

	}

}
