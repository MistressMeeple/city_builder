package com.meeple.citybuild;

import org.joml.Vector3f;

import com.meeple.citybuild.server.GameManager;
import com.meeple.citybuild.server.LevelData.Chunk.Tile;

public class RayHelper {

	private static final int RECURSION_COUNT = 500;
	private static final float RAY_RANGE = 1000;

	private Vector3f currentTerrainPoint;

	private Tile tile;

	public Vector3f getCurrentTerrainPoint() {
		return currentTerrainPoint;
	}

	public Tile getCurrentTile() {
		return tile;
	}

	public void update(Vector3f ray, Vector3f cameraPos, GameManager game) {
		if (intersectionInRange(0, RAY_RANGE, ray, cameraPos)) {
			currentTerrainPoint = binarySearch(0, 0, RAY_RANGE, ray, cameraPos, game);
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

	private Vector3f binarySearch(int count, float start, float finish, Vector3f ray, Vector3f camPos, GameManager game) {
		float half = start + ((finish - start) / 2f);
		if (count >= RECURSION_COUNT) {
			Vector3f endPoint = getPointOnRay(ray, half, camPos);
			Tile tile = game.getTile(endPoint);

			if (tile != null) {
				this.tile = tile;
				return endPoint;
			} else {
				return null;
			}
		}
		if (intersectionInRange(start, half, ray, camPos)) {
			return binarySearch(count + 1, start, half, ray, camPos, game);
		} else {
			return binarySearch(count + 1, half, finish, ray, camPos, game);
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
