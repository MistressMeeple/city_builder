package com.meeple.citybuild.server;

import java.util.Map;
import java.util.Set;

import org.joml.Vector2i;

import com.meeple.citybuild.server.LevelData.Chunk;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.FrameUtils;

public class WorldGenerator {

	public static enum TileTypes {
		//		Hole, Ground, Other, Housing, FoodProduction, WaterProduction;
		Terrain,
		Stockpile,
		Housing,
		Food,
		Power,
		Resources;

	}

	public static enum TileSize {
		Small,
		Medium,
		Large;
	}

	public static Map<TileTypes, Set<Tiles>> typesByTypes = new CollectionSuppliers.MapSupplier<TileTypes, Set<Tiles>>().get();

	//TODO allow small kitchens to be put into buildings eg houses/factories
	public static enum Tiles {
		/*
		 * 
		 */
		Hole(TileTypes.Terrain),
		Ground(TileTypes.Terrain),
		Water(TileTypes.Terrain),
		/*
		 * 
		 */
		Tent(TileTypes.Housing),
		House(TileTypes.Housing),
		/*
		 * 
		 */
		CropFarm(TileTypes.Food),
		MeatFarm(TileTypes.Food),
		Kitchens(TileTypes.Food),
		/*
		 * 
		 */
		WaterWheel(TileTypes.Power),
		/*
		 * 
		 */
		TreeFarm(TileTypes.Resources),
		StoneMine(TileTypes.Resources),
		NormalMetalMine(TileTypes.Resources),
		SpecialMetalMine(TileTypes.Resources);

		TileTypes type;

		private Tiles(TileTypes type) {
			this.type = type;
			FrameUtils.addToSetMap(typesByTypes, type, this, new CollectionSuppliers.SetSupplier<>());
		}

	}

	public void create(LevelData level, long seed) {
		for (int x = 0; x < 2; x++) {
			for (int y = 0; y < 2; y++) {

				Chunk mainChunk = level.new Chunk();
				for (int tx = 0; tx < mainChunk.tiles.length; tx++) {
					for (int ty = 0; ty < mainChunk.tiles[tx].length; ty++) {

						if (tx == 0 || ty == 0 || tx == mainChunk.tiles.length - 1 || ty == mainChunk.tiles[0].length - 1) {
							mainChunk.tiles[tx][ty].type = Tiles.Hole;
						} else {
							mainChunk.tiles[tx][ty].type = Tiles.Ground;
						}
					}
				}

				level.chunks.put(new Vector2i(x, y), mainChunk);
			}
		}
	}
}
