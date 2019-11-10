package com.meeple.shared.main;

public enum Direction2D {

	UP(0, 1), DOWN(0, -1), LEFT(1, 0), RIGHT(-1, 0);
	public int x, y;

	Direction2D(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Direction2D opposite() {
		switch (this) {
			case DOWN:
				return UP;
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

	public Direction2D next() {
		int value = this.ordinal() + 1;
		Direction2D ret;
		if (value > Direction2D.values().length - 1) {
			value = 0;
		}
		ret = Direction2D.values()[value];
		return ret;
	}

}
