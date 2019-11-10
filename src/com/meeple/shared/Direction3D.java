package com.meeple.shared;

public enum Direction3D {
	UP(0, 0, 1), DOWN(0, 0, -1), FRONT(0, 1, 0), BACK(0, -1, 0), LEFT(1, 0, 0), RIGHT(-1, 0, 0);
	public int x, y, z;

	Direction3D(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Direction3D opposite() {
		switch (this) {
			case BACK:
				return FRONT;
			case DOWN:
				return UP;
			case FRONT:
				return BACK;
			case LEFT:
				return RIGHT;
			case RIGHT:
				return LEFT;
			case UP:
				return DOWN;
			default:
				return null;

		}
	}
}
