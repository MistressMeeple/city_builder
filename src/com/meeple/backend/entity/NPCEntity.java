package com.meeple.backend.entity;

public class NPCEntity extends LivingEntity {

	public NPCEntity() {
		transformation().translate(0, 0, 100);
		useGravity(true);
		bounds().setMin(-.25f, -.25f, -.0f);
		bounds().setMax(0.25f, 0.25f, 1.5f);
	}
}
