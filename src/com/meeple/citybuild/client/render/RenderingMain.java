package com.meeple.citybuild.client.render;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL46;

import com.meeple.shared.frame.OGL.IShaderUniformUploadSystem;
import com.meeple.shared.frame.OGL.ShaderProgramSystem;
import com.meeple.shared.frame.OGL.ShaderProgramSystem.MultiUniformSystem;
import com.meeple.shared.frame.OGL.ShaderProgramSystem.SingleUniformSystem;
import com.meeple.shared.frame.OGL.UniformManager;

public class RenderingMain {
	/**
	 * Global shader program system
	 */

	public static ShaderProgramSystem system = new ShaderProgramSystem();

	/**
	 * Single upload uniform manager instance
	 */
	public static UniformManager<String, Integer> singleUpload = new SingleUniformSystem();
	/**
	 * Multiple upload uniform manager instance
	 */
	public static UniformManager<String[], Integer[]> multiUpload = new MultiUniformSystem();
	public static IShaderUniformUploadSystem<Matrix4f, Integer> mat4SingleUploader = (upload,uniformID,stack)->{
		GL46.glUniformMatrix4fv(uniformID, false, IShaderUniformUploadSystem.generateMatrix4fBuffer(stack, upload));		
	};
}
