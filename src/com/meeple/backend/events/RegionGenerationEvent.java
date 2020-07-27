package com.meeple.backend.events;

import java.util.Map;

import org.joml.Vector2i;

import com.meeple.backend.game.world.Terrain;

/**
 * Region generation event is called after all the chunks within have been
 * generated. the contained map of chunks are those corresponding to the region
 * AND have just been generated
 * 
 * @author Megan
 *
 */
public class RegionGenerationEvent extends EventBase {
	private final Vector2i regionIndex;
	private final Map<Vector2i, Terrain> region;

	public RegionGenerationEvent(Vector2i key, Map<Vector2i, Terrain> region) {
		this.region = region;
		this.regionIndex = key;
	}

	public Map<Vector2i, Terrain> getRegion() {
		return region;
	}

	public Vector2i getRegionIndex() {
		return regionIndex;
	}
}
