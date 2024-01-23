package com.meeple.citybuild.client.render;

import org.joml.Vector2f;

import com.meeple.shared.utils.FrameUtils;

public class RendererBase {
	/*
		public static  void uploadVP( VPMatrixSystem  vpSystem, VP vpMatrix) {
	
			vpSystem.preMult(vpMatrix.proj.getWrapped(), vpMatrix.view.getWrapped());
	
			Set<Entry<ShaderProgram, UniformManager<String[], Integer[]>.Uniform<VP>>> set = vpSystem.uniformCache.entrySet();
			synchronized (vpSystem.uniformCache) {
				for (Iterator<Entry<ShaderProgram, UniformManager<String[], Integer[]>.Uniform<VP>>> iterator = set.iterator(); iterator.hasNext();) {
					Entry<ShaderProgram, UniformManager<String[], Integer[]>.Uniform<VP>> entry = iterator.next();
					ShaderProgram key = entry.getKey();
					UniformManager<String[], Integer[]>.Uniform<VP> value = entry.getValue();
					RenderingMain.system.queueUniformUpload(key, RenderingMain.	multiUpload, value, vpMatrix);
	
				}
			}
		}*/

	public static int circleSegments = 16;

	public static Vector2f[] generateCircle(Vector2f argCenter, float argRadius) {
		return generateCircle(argCenter, argRadius, circleSegments);
	}

	public static Vector2f[] generateCircle(Vector2f argCenter, float argRadius, int segments) {
		Vector2f[] points = new Vector2f[segments];

		float inc = FrameUtils.TWOPI / segments;

		for (int i = 0; i < segments; i++) {
			points[i] = new Vector2f();
			points[i].x = (float) (argCenter.x + Math.cos(i * inc) * argRadius);
			points[i].y = (float) (argCenter.y + Math.sin(i * inc) * argRadius);
		}
		return points;
	}
}
