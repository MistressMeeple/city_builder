package com.meeple.shared.frame.camera;

import java.util.function.Supplier;

import org.joml.Vector3f;

import com.meeple.shared.utils.FrameUtils;

public class CameraSpringArm {

    /**
     * Constants that define the max camera angles
     */
    public static float minDistance = 0.1f, minPitch = -80, maxPitch = 89;

    public Supplier<Vector3f> lookAt;
    public float distance = 10f, pitch = 0, yaw = 0f;

    public void addDistance(float toAdd) {
        distance += toAdd;
        distance = Math.max(distance, CameraSpringArm.minDistance);
    }

    public float getDistance() {
        return distance;
    }

    public void addPitch(float toAdd) {
        pitch += toAdd;
        pitch = FrameUtils.getClamped(minPitch, pitch, maxPitch);
    }

    public float getPitch() {
        return pitch;
    }

}
