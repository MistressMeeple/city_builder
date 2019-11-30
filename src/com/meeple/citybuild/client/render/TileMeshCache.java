package com.meeple.citybuild.client.render;

import java.util.Map;

import com.meeple.citybuild.client.render.WorldRenderer.MeshExt;
import com.meeple.citybuild.server.LevelData.Chunk.Tile;
import com.meeple.citybuild.server.WorldGenerator.Tiles;
import com.meeple.shared.CollectionSuppliers;

public class TileMeshCache {
	class TileMesh {
		MeshExt floorMesh;
		MeshExt building;
		MeshExt extra;
	}

	public static final Map<Tile, MeshExt> cache = new CollectionSuppliers.MapSupplier<Tile,MeshExt>().get();

	public static final void init() {
		for (Tiles tile : Tiles.values()) {
			switch (tile) {
				case CropFarm:
					break;
				case Ground:
					break;
				case Hole:
					break;
				case House:
					break;
				case Kitchens:
					break;
				case MeatFarm:
					break;
				case NormalMetalMine:
					break;
				case SpecialMetalMine:
					break;
				case StoneMine:
					break;
				case Tent:
					break;
				case TreeFarm:
					break;
				case Water:
					break;
				case WaterWheel:
					break;
				default:
					break;

			}
		}
	}

}
