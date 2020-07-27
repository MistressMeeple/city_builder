package com.meeple.backend;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.AttributeFactory;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLCompoundDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;

public class ShaderPrograms {
	private static Logger logger = Logger.getLogger(ShaderPrograms.class);

	public final static AttributeFactory vertAtt = new AttributeFactory().name("vertex").bufferType(BufferType.ArrayBuffer).dataType(GLCompoundDataType.Vec3f).bufferUsage(BufferUsage.StaticDraw);

	public final static AttributeFactory normalAtt = new AttributeFactory().name("normal").bufferType(BufferType.ArrayBuffer).dataType(GLCompoundDataType.Vec4f).bufferUsage(BufferUsage.StaticDraw).dataSize(4);

	public final static AttributeFactory colourAtt = new AttributeFactory().name("colour").bufferType(BufferType.ArrayBuffer).dataType(GLCompoundDataType.Vec4f).bufferUsage(BufferUsage.DynamicDraw).dataSize(4);

	public final static AttributeFactory transformAtt = new AttributeFactory().name("meshMatrix").bufferType(BufferType.ArrayBuffer).dataType(GLCompoundDataType.Mat4f).bufferUsage(BufferUsage.DynamicDraw).instanced(true, 1);

	public final static AttributeFactory normalMatrixAtt = new AttributeFactory().name("normalMatrix").bufferType(BufferType.ArrayBuffer).dataType(GLCompoundDataType.Mat4f).bufferUsage(BufferUsage.DynamicDraw).instanced(true, 1);

	public final static AttributeFactory materialIndexAtt = new AttributeFactory().name("materialIndex").bufferType(BufferType.ArrayBuffer).dataType(GLDataType.Float).bufferUsage(BufferUsage.DynamicDraw).instanced(true, 1);

	public static enum InstancedAttribute {
		Transformation, NormalMatrix, MaterialIndex, Colour;
	}

	public static enum Program {
		_3D_Unlit_Flat, _3D_Lit_Flat, _3D_Lit_Material;
		public final ShaderProgram program;
		private boolean init = false;
		private boolean created = false;

		private Program() {
			this.program = new ShaderProgram();
		}
	}

	public static class InstanceData {
		private final Set<Instance> instances = Collections.synchronizedSet(new HashSet<>());

		public Iterator<Instance> getIterator() {
			return instances.iterator();
		}

		public int size() {
			return instances.size();
		}

		public class Instance {
			Matrix4f meshMatrix = new Matrix4f();
			Vector4f colour;
			Integer materialIndex;
			boolean visible = true;

			public Instance() {
				InstanceData.this.instances.add(this);
			}

		}
	}

	public static ShaderProgram init(Program program) {
		String vert = "";
		String frag = "";

		if (!program.init) {
			if (program == Program._3D_Unlit_Flat) {
				vert = ShaderProgramSystem2.loadShaderSourceFromFile("resources/shaders/3D_nolit_flat.vert");
				frag = ShaderProgramSystem2.loadShaderSourceFromFile("resources/shaders/3D_nolit_flat.frag");
			} else if (program == Program._3D_Lit_Flat) {

				vert = ShaderProgramSystem2.loadShaderSourceFromFile("resources/shaders/3D_lit_flat.vert");
				frag = ShaderProgramSystem2.loadShaderSourceFromFile("resources/shaders/3D_lit_flat.frag");
			} else if (program == Program._3D_Lit_Material) {

				vert = ShaderProgramSystem2.loadShaderSourceFromFile("resources/shaders/3D_lit_mat.vert");
				frag = ShaderProgramSystem2.loadShaderSourceFromFile("resources/shaders/3D_lit_mat.frag");
			} else {
				logger.warn("Program was not the one of predefined programs and could not be created");
			}
			program.program.shaderSources.put(GLShaderType.VertexShader, vert);
			program.program.shaderSources.put(GLShaderType.FragmentShader, frag);
			program.init = true;
		}
		return program.program;
	}

	public static ShaderProgram initAndCreate(GLContext glc, Program programD) {
		ShaderProgram program = init(programD);
		if (!programD.created) {
			try {
				ShaderProgramSystem2.create(glc, program);
				programD.created = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return program;
	}

}
