package com.meeple.backend.events;

import com.meeple.backend.game.world.Terrain;

public class TerrainGenerationEvent extends EventBase {
	public class Start {

	}

	public class End {

	}

	private final Terrain terrain;

	public TerrainGenerationEvent(Terrain terrain) {
		this.terrain = terrain;
	}

	public Terrain getTerrain() {
		return terrain;
	}

}
