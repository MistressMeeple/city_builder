package com.meeple.backend.events;

import com.meeple.backend.game.world.Terrain;

public class TerrainPopulationEvent extends EventBase {
	private final Terrain terrain;

	public TerrainPopulationEvent(Terrain terrain) {
		this.terrain = terrain;
	}

	public Terrain getTerrain() {
		return terrain;
	}

}
