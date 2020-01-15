package com.meeple.citybuild.client.render;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46;

import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;

public class WorldRenderingSPC {

	class MeshBuilder {
		float[] verts;
		int[] indices;
		float[] normals;
		float[] colour;
		int zIndex;

		//per instance
		Matrix4f translation;
		Integer materialIndex;

		public MeshBuilder vertices(float[] vertices) {
			return this;
		}

		public MeshBuilder elements(int[] elements) {
			return this;
		}

		public MeshBuilder normals(float[] normals) {
			return this;
		}

		public MeshBuilder colour(float[] colour) {
			return this;
		}

	}

	private void m() {
	}

	/*
	* builder
	* 	.vertices(float[] vertexPosiitons)
	* 	.elements(int[] elements)
	* 	.normals(float[] normals)
	* 	.newInstance(Matrix4f translation,Vector3f colour)
	* 	.newInstance(Matrix4f translation,Vector3f colour);
	*/
	class BaseMeshUnlitFlat {
		Vector3f vPosition;
		Vector4f[] mColour;
		Matrix4f[] mTranslation;
	}

	class BaseMeshLitFlat {

	}

	class BaseMeshUnlitMaterial {

	}

	class BaseMeshLitMaterial {

	}

	enum UIProgramType {
		Primary;
	}

	enum WorldProgramType {
		/**
		 * This is the debug drawing, same setup as world unlit 
		 */
		WorldDebug(false, false),
		WorldUnlitFlat(false, false),
		WorldLitFlat(true, false),
		WorldUnlitMaterial(false, true),
		WorldLitMaterial(true, true);

		boolean usesLighting, usesMaterials;

		private WorldProgramType(boolean usesLighting, boolean usesMaterials) {
			this.usesLighting = usesLighting;
			this.usesMaterials = usesMaterials;
		}

		public WorldProgramType create(boolean usesLighting, boolean usesMaterials) {
			WorldProgramType ret = null;
			for (WorldProgramType pt : WorldProgramType.values()) {
				if (pt.usesLighting == usesLighting && pt.usesMaterials == usesMaterials) {
					ret = pt;
					break;
				}
			}
			return ret;
		}
	}

	private static final int maxMaterials = 10;
	private static final int maxLights = 2;

	private static int matrixBufferBindingPoint = 2;
	public Map<WorldProgramType, ShaderProgram> worldPrograms = new CollectionSuppliers.MapSupplier<WorldProgramType, ShaderProgram>().get();
	public Map<UIProgramType, ShaderProgram> uiPrograms = new CollectionSuppliers.MapSupplier<UIProgramType, ShaderProgram>().get();

	public void setupPrograms(GLContext glc) {
		for (WorldProgramType type : WorldProgramType.values()) {
			ShaderProgram program = new ShaderProgram();

			String vertexSource = "";
			String fragmentSource = "";

			switch (type) {
				case WorldLitFlat:
					vertexSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_lit_flat.vert"));
					fragmentSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_lit_flat.frag"));
					break;
				case WorldLitMaterial:
					vertexSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_lit_mat.vert"));
					fragmentSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_lit_mat.frag"));
					break;
				case WorldDebug:
				case WorldUnlitFlat:
					vertexSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_unlit_flat.vert"));
					fragmentSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_unlit_flat.frag"));
					break;
				case WorldUnlitMaterial:
					vertexSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_unlit_mat.vert"));
					fragmentSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_unlit_mat.frag"));
					break;
			}
			if (type.usesLighting) {
				vertexSource = vertexSource.replaceAll("\\{maxlights\\}", maxLights + "");
				fragmentSource = fragmentSource.replaceAll("\\{maxlights\\}", maxLights + "");
			}
			if (type.usesMaterials) {
				vertexSource = vertexSource.replaceAll("\\{maxmats\\}", "" + maxMaterials);
				fragmentSource = fragmentSource.replaceAll("\\{maxmats\\}", "" + maxMaterials);
			}
			program.shaderSources.put(GLShaderType.VertexShader, vertexSource);
			program.shaderSources.put(GLShaderType.FragmentShader, fragmentSource);
			ShaderProgramSystem2.create(glc, program);
			worldPrograms.put(type, program);
		}
		for (UIProgramType type : UIProgramType.values()) {
			ShaderProgram program = new ShaderProgram();

			String vertexSource = "";
			String fragmentSource = "";
			switch (type) {
				case Primary: {

					break;
				}

			}

			program.shaderSources.put(GLShaderType.VertexShader, vertexSource);
			program.shaderSources.put(GLShaderType.FragmentShader, fragmentSource);
			ShaderProgramSystem2.create(glc, program);
			uiPrograms.put(type, program);
		}

	}

	public Mesh setupMesh(WorldProgramType progType) {
		Mesh ret = null;
		switch (progType) {
			case WorldLitFlat: {
				ret = new Mesh();
				{
					Attribute vPositionAttrib = new Attribute();
					vPositionAttrib.name = "vPosition";
					vPositionAttrib.bufferType = BufferType.ArrayBuffer;
					vPositionAttrib.dataType = GLDataType.Float;
					vPositionAttrib.bufferUsage = BufferUsage.StaticDraw;
					vPositionAttrib.dataSize = 3;
					vPositionAttrib.normalised = false;

					ret.VBOs.add(vPositionAttrib);
					ret.attributes.put("vPosition", vPositionAttrib);
				}
				{
					Attribute vNormalAttrib = new Attribute();
					vNormalAttrib.name = "vNormal";
					vNormalAttrib.bufferType = BufferType.ArrayBuffer;
					vNormalAttrib.dataType = GLDataType.Float;
					vNormalAttrib.bufferUsage = BufferUsage.StaticDraw;
					vNormalAttrib.dataSize = 3;
					vNormalAttrib.normalised = false;

					ret.VBOs.add(vNormalAttrib);
					ret.attributes.put("vNormal", vNormalAttrib);
				}

				{
					Attribute colourAttrib = new Attribute();
					colourAttrib.name = "colour";
					colourAttrib.bufferType = BufferType.ArrayBuffer;
					colourAttrib.dataType = GLDataType.Float;
					colourAttrib.bufferUsage = BufferUsage.StaticDraw;
					colourAttrib.dataSize = 4;
					colourAttrib.normalised = false;
					colourAttrib.instanced = true;
					colourAttrib.instanceStride = 1;

					ret.VBOs.add(colourAttrib);
					ret.attributes.put("colour", colourAttrib);
				}

				{
					Attribute modelMatrixAttrib = new Attribute();
					modelMatrixAttrib.name = "modelMatrix";
					modelMatrixAttrib.bufferType = BufferType.ArrayBuffer;
					modelMatrixAttrib.dataType = GLDataType.Float;
					modelMatrixAttrib.bufferUsage = BufferUsage.StaticDraw;
					modelMatrixAttrib.dataSize = 16;
					modelMatrixAttrib.normalised = false;
					modelMatrixAttrib.instanced = true;
					modelMatrixAttrib.instanceStride = 1;

					ret.VBOs.add(modelMatrixAttrib);
					ret.attributes.put("modelMatrix", modelMatrixAttrib);
				}
				{
					Attribute modelNormalMatrixAttrib = new Attribute();
					modelNormalMatrixAttrib.name = "normalMatrix";
					modelNormalMatrixAttrib.bufferType = BufferType.ArrayBuffer;
					modelNormalMatrixAttrib.dataType = GLDataType.Float;
					modelNormalMatrixAttrib.bufferUsage = BufferUsage.StaticDraw;
					modelNormalMatrixAttrib.dataSize = 16;
					modelNormalMatrixAttrib.normalised = false;
					modelNormalMatrixAttrib.instanced = true;
					modelNormalMatrixAttrib.instanceStride = 1;

					ret.VBOs.add(modelNormalMatrixAttrib);
					ret.attributes.put("normalMatrix", modelNormalMatrixAttrib);
				}
				ret.modelRenderType = GLDrawMode.Triangles;

				break;
			}
			case WorldLitMaterial: {

				ret = new Mesh();
				{
					Attribute vPositionAttrib = new Attribute();
					vPositionAttrib.name = "vPosition";
					vPositionAttrib.bufferType = BufferType.ArrayBuffer;
					vPositionAttrib.dataType = GLDataType.Float;
					vPositionAttrib.bufferUsage = BufferUsage.StaticDraw;
					vPositionAttrib.dataSize = 3;
					vPositionAttrib.normalised = false;

					ret.VBOs.add(vPositionAttrib);
					ret.attributes.put("vPosition", vPositionAttrib);
				}
				{
					Attribute vNormalAttrib = new Attribute();
					vNormalAttrib.name = "vNormal";
					vNormalAttrib.bufferType = BufferType.ArrayBuffer;
					vNormalAttrib.dataType = GLDataType.Float;
					vNormalAttrib.bufferUsage = BufferUsage.StaticDraw;
					vNormalAttrib.dataSize = 3;
					vNormalAttrib.normalised = false;

					ret.VBOs.add(vNormalAttrib);
					ret.attributes.put("vNormal", vNormalAttrib);
				}

				{
					Attribute matIndexAttrib = new Attribute();
					matIndexAttrib.name = "materialIndex";
					matIndexAttrib.bufferType = BufferType.ArrayBuffer;
					matIndexAttrib.dataType = GLDataType.Float;
					matIndexAttrib.bufferUsage = BufferUsage.StaticDraw;
					matIndexAttrib.dataSize = 1;
					matIndexAttrib.normalised = false;
					matIndexAttrib.instanced = true;
					matIndexAttrib.instanceStride = 1;

					ret.VBOs.add(matIndexAttrib);
					ret.attributes.put("colour", matIndexAttrib);
				}

				{
					Attribute modelMatrixAttrib = new Attribute();
					modelMatrixAttrib.name = "modelMatrix";
					modelMatrixAttrib.bufferType = BufferType.ArrayBuffer;
					modelMatrixAttrib.dataType = GLDataType.Float;
					modelMatrixAttrib.bufferUsage = BufferUsage.StaticDraw;
					modelMatrixAttrib.dataSize = 16;
					modelMatrixAttrib.normalised = false;
					modelMatrixAttrib.instanced = true;
					modelMatrixAttrib.instanceStride = 1;

					ret.VBOs.add(modelMatrixAttrib);
					ret.attributes.put("modelMatrix", modelMatrixAttrib);
				}
				{
					Attribute modelNormalMatrixAttrib = new Attribute();
					modelNormalMatrixAttrib.name = "normalMatrix";
					modelNormalMatrixAttrib.bufferType = BufferType.ArrayBuffer;
					modelNormalMatrixAttrib.dataType = GLDataType.Float;
					modelNormalMatrixAttrib.bufferUsage = BufferUsage.StaticDraw;
					modelNormalMatrixAttrib.dataSize = 16;
					modelNormalMatrixAttrib.normalised = false;
					modelNormalMatrixAttrib.instanced = true;
					modelNormalMatrixAttrib.instanceStride = 1;

					ret.VBOs.add(modelNormalMatrixAttrib);
					ret.attributes.put("normalMatrix", modelNormalMatrixAttrib);
				}
				ret.modelRenderType = GLDrawMode.Triangles;
				break;
			}
			case WorldDebug:
			case WorldUnlitFlat: {
				ret = new Mesh();
				{
					Attribute vPositionAttrib = new Attribute();
					vPositionAttrib.name = "vPosition";
					vPositionAttrib.bufferType = BufferType.ArrayBuffer;
					vPositionAttrib.dataType = GLDataType.Float;
					vPositionAttrib.bufferUsage = BufferUsage.StaticDraw;
					vPositionAttrib.dataSize = 3;
					vPositionAttrib.normalised = false;

					ret.VBOs.add(vPositionAttrib);
					ret.attributes.put("vPosition", vPositionAttrib);
				}
				{
					Attribute colourAttrib = new Attribute();
					colourAttrib.name = "colour";
					colourAttrib.bufferType = BufferType.ArrayBuffer;
					colourAttrib.dataType = GLDataType.Float;
					colourAttrib.bufferUsage = BufferUsage.StaticDraw;
					colourAttrib.dataSize = 4;
					colourAttrib.normalised = false;
					colourAttrib.instanced = true;
					colourAttrib.instanceStride = 1;

					ret.VBOs.add(colourAttrib);
					ret.attributes.put("colour", colourAttrib);
				}
				ret.modelRenderType = GLDrawMode.Triangles;

				break;
			}
			case WorldUnlitMaterial: {
				ret = new Mesh();
				//vPosition
				//material index
				//model matrix

				break;
			}
			default:
				break;

		}
		return ret;
	}

	public void bind(GLContext glc) {
		glc.bindUBONameToIndex("", matrixBufferBindingPoint, worldPrograms.values());
	}

	private int storeMatrixBuffer(GLContext glc, Matrix4f fix, Matrix4f projection, Matrix4f view, Matrix4f vpMult, Matrix4f vpf, int bindingPoint) {
		int matrixBuffer = glc.genUBO(bindingPoint, 64 * 5);
		writeVPFMatrix(matrixBuffer, fix, projection, view, vpMult, vpf);

		//binds the buffer to a binding index
		glBindBufferBase(GL_UNIFORM_BUFFER, bindingPoint, matrixBuffer);
		return matrixBuffer;
	}

	/**
	 * update the VP matrix to the GPU buffers
	 * @param buffer named location to upload to
	 * @param view view matrix
	 * @param viewProjection VP matrix
	 */
	private void writeVPFMatrix(int buffer, Matrix4f fix, Matrix4f projection, Matrix4f view, Matrix4f vp, Matrix4f vpf) {

		//no need to be in a program bidning, since this is shared between multiple programs
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
		float[] store = new float[16];
		if (fix != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 0, fix.get(store));
		if (projection != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 1, projection.get(store));
		if (view != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 2, view.get(store));
		if (vp != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 3, vp.get(store));
		if (vpf != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 4, vpf.get(store));

		glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
	}
}
