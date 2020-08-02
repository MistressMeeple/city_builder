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

/**
 * A helper class that holds all of the programs I use in the project and the attributes that are used in the programs. 
 * Sits on top of the {@link ShaderProgramSystem2} class
 * @author Megan
 *
 */
public class ShaderPrograms {
	private static Logger logger = Logger.getLogger(ShaderPrograms.class);

	/**
	 * Vertex position attribute used in all shaders.
	 * <ul>
	 * <li>Buffer Type: {@link BufferType.ArrayBuffer}</li>
	 * <li>Data Type: {@link GLCompoundDataType.Vec3f}</li>
	 * <li>Buffer Usage: {@link BufferUsage.StaticDraw}</li>
	 * </ul>
	 */

	public final static AttributeFactory vertAtt = new AttributeFactory().name("vertex").bufferType(BufferType.ArrayBuffer).dataType(GLCompoundDataType.Vec3f).bufferUsage(BufferUsage.StaticDraw);
	/**
	 * Vertex normal attribute used in some shaders.
	 * <ul>
	 * <li>Buffer Type: {@link BufferType.ArrayBuffer}</li>
	 * <li>Data Type: {@link GLCompoundDataType.Vec4f}</li>
	 * <li>Buffer Usage: {@link BufferUsage.StaticDraw}</li>
	 * </ul>
	 */
	public final static AttributeFactory normalAtt = new AttributeFactory().name("normal").bufferType(BufferType.ArrayBuffer).dataType(GLCompoundDataType.Vec4f).bufferUsage(BufferUsage.StaticDraw).dataSize(4);

	/**
	 * Model/vertex colour attribute used in some shaders.
	 * <ul>
	 * <li>Buffer Type: {@link BufferType.ArrayBuffer}</li>
	 * <li>Data Type: {@link GLCompoundDataType.Vec4f}</li>
	 * <li>Buffer Usage: {@link BufferUsage.StaticDraw}</li>
	 * </ul>
	 */
	public final static AttributeFactory colourAtt = new AttributeFactory().name("colour").bufferType(BufferType.ArrayBuffer).dataType(GLCompoundDataType.Vec4f).bufferUsage(BufferUsage.DynamicDraw).dataSize(4);

	/**
	 * Transform/model matrix used in most shaders.
	 * <ul>
	 * <li>Buffer Type: {@link BufferType.ArrayBuffer}</li>
	 * <li>Data Type: {@link GLCompoundDataType.Mat4f}</li>
	 * <li>Buffer Usage: {@link BufferUsage.DynamicDraw}</li>
	 * <li>Instanced: true, 1</li>
	 * </ul>
	 */
	public final static AttributeFactory transformAtt = new AttributeFactory().name("meshMatrix").bufferType(BufferType.ArrayBuffer).dataType(GLCompoundDataType.Mat4f).bufferUsage(BufferUsage.DynamicDraw).instanced(true, 1);
	/**
	 * Model's normal matrix attribute used in some shaders. Used with the
	 * {@link #normalAtt} and {@link #transformAtt}
	 * <ul>
	 * <li>Buffer Type: {@link BufferType.ArrayBuffer}</li>
	 * <li>Data Type: {@link GLCompoundDataType.Mat4f}</li>
	 * <li>Buffer Usage: {@link BufferUsage.DynamicDraw}</li>
	 * <li>Instanced: true, 1</li>
	 * </ul>
	 */
	public final static AttributeFactory normalMatrixAtt = new AttributeFactory().name("normalMatrix").bufferType(BufferType.ArrayBuffer).dataType(GLCompoundDataType.Mat4f).bufferUsage(BufferUsage.DynamicDraw).instanced(true, 1);
	/**
	 * Model's matieral index attribute used in some shaders. upload the materials
	 * seperated and use this to specify which material the model will use
	 * <ul>
	 * <li>Buffer Type: {@link BufferType.ArrayBuffer}</li>
	 * <li>Data Type: {@link GLCompoundDataType.Float}</li>
	 * <li>Buffer Usage: {@link BufferUsage.DynamicDraw}</li>
	 * <li>Instanced: true, 1</li>
	 * </ul>
	 */
	public final static AttributeFactory materialIndexAtt = new AttributeFactory().name("materialIndex").bufferType(BufferType.ArrayBuffer).dataType(GLDataType.Float).bufferUsage(BufferUsage.DynamicDraw).instanced(true, 1);

	public static enum InstancedAttribute {
		Transformation, NormalMatrix, MaterialIndex, Colour;
	}

	/**
	 * All of the shader programs used in this project stored in an enum. Before use
	 * ensure the program has been constructed and initialized. <br>
	 * This can be achieved in two ways. 
	 * <ol>
	 * <li>Calling both {@link ShaderPrograms#init(Program)} and {@link ShaderPrograms#create(GLContext, Program)}. These methods are both called in order in the {@link ShaderPrograms#initAndCreate(GLContext, Program)} method</li>
	 * <li>Or programming it yourself as direct access to the shader program is available with the {@linkplain #program} variable</li>
	 * </ol>
	 * @author Megan
	 */
	public static enum Program {
		_3D_Unlit_Flat,
		_3D_Lit_Flat,
		_3D_Lit_Material;
		public final ShaderProgram program;
		private boolean init = false;
		private boolean created = false;

		private Program() {
			this.program = new ShaderProgram();
		}
	}

	/**
	 * A helper class designed to manage the general instance data of models/meshes. 
	 * @author Megan
	 *
	 */
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

	/**
	 * Initialises the program by supplying it the correct shader sources. 
	 * @param program
	 */
	public static void init(Program program) {
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

	}

	/**
	 * Creates the shader program
	 * @param glc
	 * @param program
	 */
	public static void create(GLContext glc, Program program) {
		if (!program.created) {
			try {
				ShaderProgramSystem2.create(glc, program.program);
				program.created = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static ShaderProgram initAndCreate(GLContext glc, Program programEnum) {
		init(programEnum);
		create(glc, programEnum);
		return programEnum.program;
	}

}
