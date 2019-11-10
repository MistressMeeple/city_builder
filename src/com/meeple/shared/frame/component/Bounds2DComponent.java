package com.meeple.shared.frame.component;

public class Bounds2DComponent {
	public Long posX = null, posY = null, width = 600L, height = 400L;

	public void set(Long x, Long y, Long width, Long height) {
		this.posX = x;
		this.posY = y;
		this.width = width;
		this.height = height;
	}

	public void size(Long width, Long height) {
		this.width = width;
		this.height = height;
	}

	public void pos(Long x, Long y) {
		this.posX = x;
		this.posY = y;
	}

	public void set(Number x, Number y, Number width, Number height) {
		if (x != null) {
			this.posX = x.longValue();
		} else {
			this.posX = null;
		}
		if (y != null) {
			this.posY = y.longValue();
		} else {
			this.posY = null;
		}
		if (width != null) {
			this.width = width.longValue();
		} else {
			this.width = null;
		}
		if (height != null) {
			this.height = height.longValue();
		} else {
			this.height = null;
		}
	}

	public void size(Number width, Number height) {
		this.width = width.longValue();
		this.height = height.longValue();
	}

	public void pos(Number x, Number y) {
		this.posX = x.longValue();
		this.posY = y.longValue();
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
