package com.meeple.citybuild;

import org.joml.Vector3f;

public class Entity {
	public final Vector3f position = new Vector3f();
	public final Vector3f rotation = new Vector3f();/*
													public Entity parent;
													
													public Entity getFull() {
													Entity e = new Entity();
													e.position.set(this.position);
													e.rotation.set(this.rotation);
													if (this.parent != null) {
													Entity e2 = this.parent.getFull();
													e.position.add(e2.position);
													e.rotation.add(e2.rotation);
													}
													return e;
													}*/
}
