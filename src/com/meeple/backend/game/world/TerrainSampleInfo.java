package com.meeple.backend.game.world;

public class TerrainSampleInfo {
	public static enum TerrainType {
		Void, Ground, Beach, Water
	}

	public float height;
	public TerrainType type = TerrainType.Ground;

	public void set(TerrainSampleInfo other) {
		this.height = other.height;
		if (other.type != null)
			this.type = other.type;
	}

}
