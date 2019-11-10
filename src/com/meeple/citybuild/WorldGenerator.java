package com.meeple.citybuild;

import org.joml.Vector2i;

import com.meeple.citybuild.LevelData.Chunk;

public class WorldGenerator {

	public static enum TileTypes {
		Hole, Ground,Other;
	}

	public void create(LevelData level, long seed) {
		Chunk mainChunk = level.new Chunk();
		for (int x = 0; x < mainChunk.tiles.length; x++) {
			for (int y = 0; y < mainChunk.tiles[x].length; y++) {
				if (x == 0 || y == 0 || x == mainChunk.tiles.length - 1 || y == mainChunk.tiles[0].length) {
					mainChunk.tiles[x][y].type = TileTypes.Hole;
				} else {
					mainChunk.tiles[x][y].type = TileTypes.Ground;
				}
			}
		}
		level.chunks.put(new Vector2i(0, 0), mainChunk);
	}
}
