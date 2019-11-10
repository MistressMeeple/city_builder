package com.meeple.shared.frame.lighting;

import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.frame.OGL.MultiUniformUploadSystemBase;
import com.meeple.shared.frame.OGL.UniformManager;

public class LightSystem extends MultiUniformUploadSystemBase<Light> {

	private static final int positionIndex = 0;
	private static final int colourIndex = 1;
	private static final int intensityIndex = 2;

	public UniformManager<String[], Integer[]>.Uniform<Light> register(UniformManager<String[], Integer[]> system, String lightPosName, String lightColourName, String lightIntensity) {
		return register(system, new ArrayBuilder().add(positionIndex, lightPosName).add(colourIndex, lightColourName).add(intensityIndex, lightIntensity));
	}

	@Override
	public void uploadToShader(Light upload, Integer[] uniformIDs, MemoryStack stack) {
		GL46.glUniform3f(uniformIDs[colourIndex], upload.colour.x, upload.colour.y, upload.colour.z);
		GL46.glUniform3f(uniformIDs[positionIndex], upload.position.x, upload.position.y, upload.position.z);
		GL46.glUniform3f(uniformIDs[intensityIndex], upload.strength.x, upload.strength.y, upload.strength.z);
	}
}
