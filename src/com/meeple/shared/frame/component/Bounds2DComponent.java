package com.meeple.shared.frame.component;

public class Bounds2DComponent {
	public Integer posX = null, posY = null, width = 600, height = 400;

	public void set(Integer x, Integer y, Integer width, Integer height) {
		this.posX = x;
		this.posY = y;
		this.width = width;
		this.height = height;
	}

	public void size(Integer width, Integer height) {
		this.width = width;
		this.height = height;
	}

	public void pos(Integer x, Integer y) {
		this.posX = x;
		this.posY = y;
	}

	public void set(Number x, Number y, Number width, Number height) {
		if (x != null) {
			this.posX = x.intValue();
		} else {
			this.posX = null;
		}
		if (y != null) {
			this.posY = y.intValue();
		} else {
			this.posY = null;
		}
		if (width != null) {
			this.width = width.intValue();
		} else {
			this.width = null;
		}
		if (height != null) {
			this.height = height.intValue();
		} else {
			this.height = null;
		}
	}

	public void size(Number width, Number height) {
		this.width = width.intValue();
		this.height = height.intValue();
	}

	public void pos(Number x, Number y) {
		this.posX = x.intValue();
		this.posY = y.intValue();
	}

	public void set(Bounds2DComponent other) {
		this.width = other.width;
		this.height = other.height;
		this.posX = other.posX;
		this.posY = other.posY;
	}

	@Override
	public String toString() {

		return "Bounds 2D component. \r\n\tX: " + posX + "\r\n\tY: " + posY + "\r\n\tWidth: " + width + "\r\n\tHeight: " + height;
	}

}
