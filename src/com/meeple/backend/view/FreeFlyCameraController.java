package com.meeple.backend.view;

import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import com.meeple.backend.Client;
import com.meeple.backend.FrameTimings;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.window.UserInput;

public class FreeFlyCameraController extends BaseCameraController {
	private final static float ep = 0.00000000000000000000011f;
	// private final static double ep = 1E-22;

	float normalSpeed = 10f;
	/**
	 * when holding shift, or whatever the "sprint" key is
	 */
	float speedMult = 2f;
	float hightClimb = 10f;
	double pitch, yaw;

	public final Vector3f position = new Vector3f();
	private double oldX, oldY;
	private AtomicBoolean hasChanged = new AtomicBoolean();
	protected boolean mouseEnabled = true;

	@Override
	protected void cursorCallback(long window, double xpos, double ypos) {

		if (!mouseEnabled) {
			double nYaw = -Math.toRadians(xpos - oldX);
			double nPitch = -Math.toRadians(ypos - oldY);
			pitch += nPitch;
			yaw += nYaw;
			oldX = xpos;
			oldY = ypos;
			hasChanged.lazySet(true);
		}
		super.cursorCallback(window, xpos, ypos);
	}

	private void actualTick() {
		final int method = 1;

		switch (method) {
		case -1: {

			Matrix4f clone = new Matrix4f(operateOn);
			clone.invert();
			Vector3f translation2 = clone.getTranslation(new Vector3f());
			Vector2d angles = new Vector2d();
			{
				angles.x = Math.atan2(operateOn.m21(), operateOn.m22());
				angles.y = Math.atan2(operateOn.m10(), operateOn.m00());

			}
			pitch = angles.x;
			yaw = angles.y;
			position.set(translation2);
		}
		case 0: {
			/*
			 * This stores the actual complete variables inside this class. wasting
			 * resources and making it difficult to retrive information
			 */
			{
				/* Clamp the pitch yaw values */
				pitch = Math.min(Math.PI - ep, Math.max(ep, pitch % FrameUtils.TWOPI));
				yaw = yaw % FrameUtils.TWOPI;
			}

			operateOn.identity();
			operateOn.rotateX((float) -pitch);
			operateOn.rotateZ((float) yaw);
			operateOn.translate(position.mul(-1, new Vector3f()));

			break;
		}
		case 1: {

			/**
			 * This method gets all the variables wanted (pitch yaw position) from the
			 * matrix, resets it and increments accordingly. then resets the stored values
			 */
			Matrix4f clone = new Matrix4f(operateOn);
			clone.invert();
			Vector3f translation2 = clone.getTranslation(new Vector3f());
			Vector2d angles = new Vector2d();
			{
				angles.x = Math.atan2(operateOn.m21(), operateOn.m22());
				angles.y = Math.atan2(operateOn.m10(), operateOn.m00());

			}
			double tempPitch = angles.x + (1 * pitch);

			double upper = 3;
			double lower = +ep;

			{ /* Clamp the pitch yaw values */

				if (tempPitch > upper) {
					tempPitch = upper;
				}
				if (tempPitch < lower) {
					tempPitch = lower;
				}

				yaw = yaw % FrameUtils.TWOPI;
			}
			operateOn.identity();
			operateOn.rotateX((float) (-(tempPitch)));
			operateOn.rotateZ((float) (-angles.y + yaw));
			operateOn.translate(translation2.add(position).mul(-1f, new Vector3f()));

			{
				position.zero();
				pitch = 0;
				yaw = 0;
			}
			break;
		}
		case 2: {
			/**
			 * This method just increments the matrix provided, not resetting it to
			 * identity.
			 */

			operateOn.rotateLocalX((float) -pitch);
			Quaternionf quat = new Quaternionf();
			quat.rotateZ((float) yaw);

			Matrix4f clone = new Matrix4f(operateOn);
			clone.invert();
			Vector3f translation2 = clone.getTranslation(new Vector3f());
			operateOn.rotateAroundAffine(quat, translation2.x, translation2.y, translation2.z, operateOn);

			operateOn.translate(position.mul(-1, new Vector3f()));
			{
				position.zero();
				pitch = 0;
				yaw = 0;
			}
		}
			break;

		}

	}

	protected void handleCameraEscape(Client client, UserInput userInput) {

		Boolean pressed = userInput.hasPressed(client.options.openMenu);

		if (pressed != null && pressed) {
			if (mouseEnabled) {
				double midx = client.fbWidth / 2;
				double midy = client.fbHeight / 2;
				oldX = midx;
				oldY = midy;
				userInput.setCursorPos(midx, midy);
				userInput.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
			} else {

				userInput.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
				double midx = client.fbWidth / 2;
				double midy = client.fbHeight / 2;
				oldX = midx;
				oldY = midy;
				userInput.setCursorPos(midx, midy);

			}
			mouseEnabled = userInput.getInputMode(GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_DISABLED;
		}
	}

	private void handleInput(FrameTimings delta, Client client) {

		UserInput userInput = client.userInput;
		Vector3f movement = new Vector3f();
		float up = 0f;

		if (userInput.isPressed(client.options.playerFoward)) {
			movement.x += 1f;
		}
		if (userInput.isPressed(client.options.playerBack)) {
			movement.x -= 1f;
		}

		if (userInput.isPressed(client.options.playerLeft)) {
			movement.y -= 1f;
		}
		if (userInput.isPressed(client.options.playerRight)) {
			movement.y += 1f;
		}

		if (userInput.isPressed(client.options.playerJump)) {
			// up
			up += hightClimb;
		}

		if (userInput.isPressed(client.options.playerCrouch)) {
			// down
			up -= hightClimb;
		}

		// make the length equal to delta seconds
		movement = movement.normalize(delta.deltaSeconds);
		if (movement.isFinite()) {
			Vector3f foward = FrameUtils.getCurrentForwardVector(this.operateOn);
			foward.z = 0;
			foward.normalize();
			float rot = (float) (Math.atan2(foward.x, -foward.y) + Math.toRadians(270));
			Vector2f rotated = FrameUtils.rotateNew(new Vector2f(movement.x, movement.y), rot);
			rotated.mul(normalSpeed, rotated);
			if (userInput.isPressed(client.options.playerSprint)) {
				rotated.mul(this.speedMult * this.speedMult, rotated);
			}
//			operateOn.translate(-rotated.x, -rotated.y, 0);
			position.add(rotated.x, rotated.y, 0);
			hasChanged.lazySet(true);

		}
		if (up != 0) {
//			operateOn.translate(0, 0, -up * delta.deltaSeconds);
			position.add(0, 0, up * delta.deltaSeconds);
			hasChanged.lazySet(true);
		}

	}

	@Override
	public boolean tick(FrameTimings delta, Client client) {
		actualTick();

		// get and clear the hasChanged flag
		boolean HASMOVED = hasChanged.compareAndSet(true, false);
		handleCameraEscape(client, client.userInput);
		handleInput(delta, client);
		return HASMOVED;

	}
}
