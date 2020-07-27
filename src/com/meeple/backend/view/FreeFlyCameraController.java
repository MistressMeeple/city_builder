package com.meeple.backend.view;

import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import com.meeple.backend.Client;
import com.meeple.backend.FrameTimings;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.window.UserInput;

public class FreeFlyCameraController extends BaseCameraController {
	float normalSpeed = 10f;
	/**
	 * when holding shift, or whatever the "sprint" key is
	 */
	float speedMult = 2f;
	float hightClimb = 10f;
	double pitch, yaw;
	private final Vector3f position = new Vector3f();
	private double oldX, oldY;
	private AtomicBoolean hasChanged = new AtomicBoolean();
	protected boolean mouseEnabled = true;

	@Override
	protected void cursorCallback(long window, double xpos, double ypos) {

		if (!mouseEnabled) {
			yaw += -Math.toRadians(xpos - oldX);
			pitch += -Math.toRadians(ypos - oldY);
			oldX = xpos;
			oldY = ypos;
			hasChanged.lazySet(true);
		}
		super.cursorCallback(window, xpos, ypos);
	}

	private void actualTick() {

		operateOn.identity();
		float ep = 0.0001f;

		pitch = Math.min(Math.PI - ep, Math.max(ep, pitch % FrameUtils.TWOPI));
		yaw = yaw % FrameUtils.TWOPI;

		operateOn.rotate((float) -pitch, 1, 0, 0);
		operateOn.rotate((float) yaw, 0, 0, 1);
		operateOn.translate(position.mul(-1f, new Vector3f()));

	}

	protected void handleCameraEscape(Client client, UserInput userInput) {

		Boolean pressed = userInput.keyPress(GLFW.GLFW_KEY_ESCAPE);
		if (pressed != null && !pressed) {
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

		if (userInput.isPressed(client.options.playerFoward.getKey())) {
			movement.x += 1f;
		}
		if (userInput.isPressed(client.options.playerBack.getKey())) {
			movement.x -= 1f;
		}

		if (userInput.isPressed(client.options.playerLeft.getKey())) {
			movement.y -= 1f;
		}
		if (userInput.isPressed(client.options.playerRight.getKey())) {
			movement.y += 1f;
		}

		if (userInput.isPressed(client.options.playerJump.getKey())) {
			// up
			up += hightClimb;
		}

		if (userInput.isPressed(client.options.playerCrouch.getKey())) {
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
			if (userInput.isPressed(client.options.playerSprint.getEventName())) {
				rotated.mul(this.speedMult * this.speedMult, rotated);
			}
			position.add(rotated.x, rotated.y, 0);
			hasChanged.lazySet(true);

		}
		if (up != 0) {
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
