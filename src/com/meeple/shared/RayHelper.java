package com.meeple.shared;

import org.joml.Vector2i;
import org.joml.Vector3f;

import com.meeple.citybuild.server.GameManager;
import com.meeple.citybuild.server.LevelData;
import com.meeple.citybuild.server.LevelData.Chunk;
import com.meeple.citybuild.server.LevelData.Chunk.Tile;

public class RayHelper {

	private static final int RECURSION_COUNT = 500;
	private static final float RAY_RANGE = 1000;

	private Vector3f currentTerrainPoint;

	private Chunk chunk;
	private Tile tile;
	private Vector2i tileIndex;

	public Vector3f getCurrentTerrainPoint() {
		return currentTerrainPoint;
	}

	public Vector2i getCurrentTileIndex() {
		return tileIndex;
	}

	public Tile getCurrentTile() {
		return tile;
	}

	public Chunk getCurrentChunk() {
		return chunk;
	}

	public void update(Vector3f ray, Vector3f cameraPos, LevelData level) {
		if (intersectionInRange(0, RAY_RANGE, ray, cameraPos)) {
			currentTerrainPoint = binarySearch(0, 0, RAY_RANGE, ray, cameraPos, level);
		} else {

			currentTerrainPoint = null;
		}
	}

	//**********************************************************

	private Vector3f getPointOnRay(Vector3f ray, float distance, Vector3f camPos) {
		Vector3f start = new Vector3f(camPos.x, camPos.y, camPos.z);
		Vector3f scaledRay = new Vector3f(ray.x * distance, ray.y * distance, ray.z * distance);
		return start.add(scaledRay);
	}

	private Vector3f binarySearch(int count, float start, float finish, Vector3f ray, Vector3f camPos, LevelData level) {
		float half = start + ((finish - start) / 2f);
		if (count >= RECURSION_COUNT) {
			Vector3f endPoint = getPointOnRay(ray, half, camPos);

			Tile tile = null;
			Vector2i index = null;
			Chunk c = GameManager.getChunk(level,endPoint);
			if (c != null) {
				chunk = c;
				index = GameManager.getTileIndex(c, endPoint);
				if (index != null) {
					this.tileIndex = index;
					tile = GameManager.getTile(c, index);
					if (tile != null) {
						this.tile = tile;
						return endPoint;
					}
				}
			}
			return null;
		}
		if (intersectionInRange(start, half, ray, camPos)) {
			return binarySearch(count + 1, start, half, ray, camPos, level);
		} else {
			return binarySearch(count + 1, half, finish, ray, camPos, level);
		}
	}

	private boolean intersectionInRange(float start, float finish, Vector3f ray, Vector3f camPos) {
		Vector3f startPoint = getPointOnRay(ray, start, camPos);
		Vector3f endPoint = getPointOnRay(ray, finish, camPos);
		if (startPoint.z >= 0 && endPoint.z < 0) {
			return true;
		} else {
			return false;
		}
	}

	/*
		private Tile getTile(float worldX, float worldY) {
			return terrain;
		}*/
}
