package com.meeple.backend.entity;

import org.joml.AABBf;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class EntityBase {
	private final Matrix4f transformation = new Matrix4f();
	private final Vector3f velocity = new Vector3f();
	private float gravityPull = 0f;
	private float maxVelocity = 100f;
	private float jumpStrength = 50f;
	private int maxJumps = 1;
	private int currentJumps = 0;
	private final AABBf bounds = new AABBf(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
	private boolean useGravity = true;
	private float eyeHeight = 1f;

	public AABBf bounds() {
		return bounds;
	}

	public Matrix4f transformation() {
		return transformation;
	}

	public boolean useGravity() {
		return useGravity;
	}

	public void useGravity(boolean useGravity) {
		this.useGravity = useGravity;
	}

	public Vector3f velocity() {
		return velocity;
	}

	public float maxVelocity() {
		return maxVelocity;
	}

	public void maxVelocity(float maxVelocity) {
		this.maxVelocity = maxVelocity;
	}

	public float jumpStrength() {
		return jumpStrength;
	}

	public void jumpStrength(float jumpStrength) {
		this.jumpStrength = jumpStrength;
	}

	public float gravityPull() {
		return gravityPull;
	}

	public void incGravityPull(float gravityPull) {
		this.gravityPull += gravityPull;
	}

	public void clearGravityPull() {
		this.gravityPull = 0;
	}

	public float eyeHeight() {
		return eyeHeight;
	}

	public void eyeHeight(float eyeHeight) {
		this.eyeHeight = eyeHeight;
	}

	public int maxJumps() {
		return maxJumps;
	}

	public int currentJumps() {
		return currentJumps;
	}

	public void maxJumps(int maxJumps) {
		this.maxJumps = maxJumps;
	}

	public void currentJumps(int currentJumps) {
		this.currentJumps = currentJumps;
	}

	public void resetJumps() {
		currentJumps = 0;
	}

	public boolean canJump() {
		return currentJumps < maxJumps;
	}

	public void tryJump() {
		if (canJump()) {
			clearGravityPull();
			velocity.z += jumpStrength();

			currentJumps += 1;
		}
	}
}
