package com.meeple.citybuild.client.render;

import org.joml.Vector4f;

import com.meeple.shared.Delta;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;

public abstract class Renderable {
	public String name;
	public final Vector4f colour = new Vector4f();

	Renderable parent;
	Renderable child;

	public final boolean isTransparent() {
		return colour.w < 1f;
	}

	public final Renderable getParent() {
		return parent;
	}

	public final void setParent(Renderable parent) {
		this.parent = parent;
		this.child = parent;
	}

	public final Renderable getChild() {
		return child;
	}

	public final void setChild(Renderable child) {
		this.child = child;
		child.parent = this;
	}

	public final Renderable getRootParent() {
		Renderable r = getParent();
		if (r == null) {
			return this;
		}
		while (r != null) {
			Renderable next = r.getParent();
			if (next == null) {
				break;
			} else {
				r = next;
			}
		}
		return r;
	}

	public final Renderable getRootChild() {
		Renderable r = getChild();
		if (r == null) {
			return this;
		}
		while (r != null) {
			Renderable next = r.getChild();
			if (next == null) {
				break;
			} else {
				r = next;
			}
		}
		return r;
	}

	/**
	 * Renders the entire tree using {@link #renderUp()}.<br>
	 * This can be called from any Renderable that is part of this rendering tree
	 */
	public void renderTree(NkContextSingleton nkContext, ClientWindow window, Delta delta) {
		Renderable child = getRootChild();
		child.renderUp(nkContext, window, delta);
	}

	/**
	 * recursive render call for whole tree
	 */
	private void renderUp(NkContextSingleton nkContext, ClientWindow window, Delta delta) {
		if (this.isTransparent()) {
			Renderable parent = getParent();
			if (parent != null) {
				parent.renderUp(nkContext, window, delta);
			}
		}
		this.render(nkContext, window, delta);
	}

	public void clearParent() {
		this.parent.clearChild();
		this.parent = null;
	}

	public void clearChild() {
		this.child.clearParent();
		this.child = null;
	}

	public abstract void render(NkContextSingleton nkContext, ClientWindow window, Delta delta);

}
