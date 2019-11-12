package com.meeple.citybuild.server;

import org.joml.Vector2i;

import com.meeple.citybuild.server.LevelData.Chunk;

public class WorldGenerator {

	public static enum TileTypes {
		Hole, Ground, Other;
	}

	public void create(LevelData level, long seed) {
		for (int x = 0; x < 2; x++) {
			for (int y = 0; y < 2; y++) {

				Chunk mainChunk = level.new Chunk();
				for (int tx = 0; tx < mainChunk.tiles.length; tx++) {
					for (int ty = 0; ty < mainChunk.tiles[tx].length; ty++) {

						if (tx == 0 || ty == 0 || tx == mainChunk.tiles.length - 1 || ty == mainChunk.tiles[0].length - 1) {
							mainChunk.tiles[tx][ty].type = TileTypes.Hole;
						} else {
							mainChunk.tiles[tx][ty].type = TileTypes.Ground;
						}
					}
				}

				level.chunks.put(new Vector2i(x, y), mainChunk);
			}
		}
	}
}
