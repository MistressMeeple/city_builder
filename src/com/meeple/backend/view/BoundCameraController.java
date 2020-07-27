package com.meeple.backend.view;

import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.meeple.backend.Client;
import com.meeple.backend.FrameTimings;
import com.meeple.backend.entity.EntityBase;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.window.UserInput;

public class BoundCameraController extends FreeFlyCameraController {
	protected EntityBase boundEntity;
	Vector3f deltaMovement = new Vector3f();
	Vector2f deltaRotation = new Vector2f();
	protected double oldX, oldY;
	protected AtomicBoolean hasChanged = new AtomicBoolean();

	@Override
	protected void cursorCallback(long window, double xpos, double ypos) {

		if (boundEntity != null) {
			if (!mouseEnabled) {

				yaw += -Math.toRadians(xpos - oldX);
				pitch += -Math.toRadians(ypos - oldY);
				deltaRotation.add((float) (xpos - oldX), (float) (ypos - oldY));

				oldX = xpos;
				oldY = ypos;
				hasChanged.lazySet(true);
			}
		} else {
			super.cursorCallback(window, xpos, ypos);
		}
	}

	public void bindTo(EntityBase entity) {
		this.boundEntity = entity;
	}

	public void unbind() {
		if (false) {
			Vector3f pos = this.boundEntity.transformation().getTranslation(new Vector3f());
			pos.add(1, 0, 2);
			this.operateOn.setLookAt(pos.add(1, 0, 2, new Vector3f()), pos, new Vector3f(0, 0, 1));
		}
		bindTo(null);
	}

	public EntityBase getBound() {
		return boundEntity;
	}

	private void actualTick() {

		Matrix4f updateRotZ = new Matrix4f();
		updateRotZ.rotateZ((float) Math.toRadians(deltaRotation.x));
		Matrix4f updateRotX = new Matrix4f();
		updateRotX.rotateX((float) Math.toRadians(deltaRotation.y));

		boundEntity.transformation().mul(updateRotZ);
		boundEntity.transformation().translate(deltaMovement);

		deltaMovement.zero();
		deltaRotation.zero();

		operateOn.identity();
		float ep = 0.0001f;

		pitch = Math.min(Math.PI - ep, Math.max(ep, pitch % FrameUtils.TWOPI));

		operateOn.rotate((float) -pitch, 1, 0, 0);
//		System.out.println(yaw);
		operateOn.rotate((float) yaw, 0, 0, 1);

		Vector3f pos = boundEntity.transformation().getTranslation(new Vector3f());
		this.operateOn.translate(-pos.x, -pos.y, -boundEntity.eyeHeight() - pos.z);

		// TODO mark the bound entity to not render0
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
		Boolean pressed = userInput.hasPressed(client.options.playerJump.getKey());
		if ((pressed != null && pressed == true) || (userInput.isPressed(client.options.playerJump.getKey()) && boundEntity.velocity().z == 0)) {
			boundEntity.tryJump();
		}

		if (userInput.isPressed(client.options.playerCrouch.getKey())) {
			// down
			up -= hightClimb;
		}

		AxisAngle4f a = boundEntity.transformation().getRotation(new AxisAngle4f());
		yaw = -(a.angle * a.z) + Math.toRadians(90);

		// make the length equal to delta seconds
		movement = movement.normalize(delta.deltaSeconds);
		if (movement.isFinite()) {
			Vector3f foward = FrameUtils.getCurrentForwardVector(this.boundEntity.transformation());
			foward.z = 0;
			if (foward.length() != 1) {
				foward.normalize();
			}

			float rot = 0;
			if (foward.isFinite()) {
				rot = (float) (Math.atan2(foward.x, -foward.y));
				yaw = rot + Math.toRadians(90);
				rot += Math.toRadians(270);
			}

			Vector2f rotated = FrameUtils.rotateNew(new Vector2f(movement.x, movement.y), rot);
			rotated.mul(normalSpeed, rotated);
			if (userInput.isPressed(client.options.playerSprint.getEventName())) {
				rotated.mul(this.speedMult * this.speedMult, rotated);
			}
			deltaMovement.add(rotated.x, rotated.y, 0);
			hasChanged.lazySet(true);

		}
		if (up != 0) {
			deltaMovement.add(0, 0, up * delta.deltaSeconds);
			hasChanged.lazySet(true);
		}
	}

	@Override
	public boolean tick(FrameTimings delta, Client client) {
		if (boundEntity != null) {
			actualTick();

			// get and clear the hasChanged flag
			boolean HASMOVED = hasChanged.compareAndSet(true, false);
			handleCameraEscape(client, client.userInput);
			handleInput(delta, client);
			return HASMOVED;
		} else {
			return super.tick(delta, client);
		}
	}
}
