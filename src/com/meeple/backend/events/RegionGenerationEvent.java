package com.meeple.backend.events;

import org.joml.Vector2i;

import com.meeple.backend.game.world.World.Region;

/**
 * Region generation event is called after all the chunks within have been generated. 
 * @author Megan
 *
 */
public class RegionGenerationEvent extends EventBase {
	private final Vector2i regionIndex;
	private final Region region;


	public RegionGenerationEvent(Vector2i key, Region region) {
		this.region = region;
		this.regionIndex = key;
	}

	public Region getRegion() {
		return region;
	}

	public Vector2i getRegionIndex() {
		return regionIndex;
	}
}
