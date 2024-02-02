package com.meeple.shared.frame.camera;

import org.joml.Vector3f;

public class Camera {

    public static enum CameraMode {
        Normal,
        LookAt;
    }

    public CameraMode cameraMode = CameraMode.LookAt;
    public final Vector3f position = new Vector3f();
    public final Vector3f rotation = new Vector3f();
    public final CameraSpringArm springArm = new CameraSpringArm();

}
