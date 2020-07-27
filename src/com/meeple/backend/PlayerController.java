package com.meeple.backend;

import org.joml.Matrix4f;

import com.meeple.backend.entity.EntityBase;

public class PlayerController {
	public final Matrix4f transformation = new Matrix4f();
	EntityBase attachedToEntity;

	public Matrix4f getTransformation() {
		if (attachedToEntity == null) {
			return transformation;
		}
		return attachedToEntity.transformation().mul(transformation, new Matrix4f());
	}
}
