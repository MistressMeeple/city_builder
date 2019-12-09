package com.meeple.citybuild.client.render;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.ShaderProgram.VBO;
import com.meeple.shared.frame.OGL.ShaderProgram.VBOBufferType;
import com.meeple.shared.frame.OGL.ShaderProgramSystem;
import com.meeple.shared.frame.nuklear.IOUtil;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;

public class ModelLoader {

	private static Logger logger = Logger.getLogger(ModelLoader.class);

	private static String debugLayout = "[%r][%d{HH:mm:ss:SSS}][%t][%p] (%F:%L) %m%n";
	private static final int maxMaterials = 10;

	private static final int maxLights = 10;
	private static final int vpMatrixBindingpoint = 2;
	private static final int lightBufferBindingPoint = 3;

	private static final int vertexAttribIndex = 0;
	private static final int normalAttribIndex = 0;
	private static final int matIndexAttribIndex = 0;
	private static final int transoforMatAttribIndex = 0;
	private static final int normalMatAttribIndex = 0;

	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(org.apache.log4j.Level.ALL);
		Appender a = new ConsoleAppender(new PatternLayout(debugLayout));
		BasicConfigurator.configure(a);

		new ModelLoader().run();
	}

	long window;
	int width = 1024;
	int height = 768;
	int fbWidth = 1024;
	int fbHeight = 768;
	float fov = 60;

	//shader program
	ShaderProgram program;
	//vertex shader uniforms
	private int matrixBuffer;
	private int lightBuffer;

	//fragment shader uniforms
	private int materialsUniformBlock[] = new int[maxMaterials];

	private int lightPositionUniform;
	private int lightColourUniform;

	//TODO store all meshes to be able to render multiple/generate their buffers per frame
	//at the moment i only have control over meshes through the model. this is wrong. since meshes can be used in multiple models
	Model model = new Model();
	Model light = new Model();

	WeakReference<Attribute> meshTransform;
	WeakReference<Attribute> meshNormal;
	WeakReference<Attribute> materialIndexAttrib;

	Matrix4f viewMatrix = new Matrix4f();
	Matrix4f projectionMatrix = new Matrix4f();
	Matrix4f viewProjectionMatrix = new Matrix4f();
	Vector3f viewPosition = new Vector3f();
	Vector3f lightPosition = new Vector3f(5f, 0f, 1f);

	private FloatBuffer lightPositionBuffer = BufferUtils.createFloatBuffer(3);

	GLCapabilities caps;
	GLFWKeyCallback keyCallback;
	GLFWFramebufferSizeCallback fbCallback;
	GLFWWindowSizeCallback wsCallback;

	GLFWScrollCallback sCallback;
	Callback debugProc;
	AIScene scene;

	void loop() {
		while (!glfwWindowShouldClose(window)) {
			glfwPollEvents();
			glViewport(0, 0, fbWidth, fbHeight);
			update();
			render();
			glfwSwapBuffers(window);
		}
	}

	void run() {
		try {
			init();
			Callback c = GLUtil.setupDebugMessageCallback();
			loop();
			aiReleaseImport(scene);
			if (debugProc != null) {
				debugProc.free();
			}

			c.free();
			keyCallback.free();
			fbCallback.free();
			wsCallback.free();
			glfwDestroyWindow(window);

			ShaderProgramSystem.close(program);

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			glfwTerminate();
		}
	}

	void init() throws IOException {

		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
		window = glfwCreateWindow(
			width,
			height,
			"Wavefront obj model loading with Assimp demo",
			NULL,
			NULL);
		if (window == NULL)
			throw new AssertionError("Failed to create the GLFW window");

		glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (width > 0 && height > 0 && (ModelLoader.this.fbWidth != width || ModelLoader.this.fbHeight != height)) {
					ModelLoader.this.fbWidth = width;
					ModelLoader.this.fbHeight = height;
				}
			}
		});
		glfwSetWindowSizeCallback(window, wsCallback = new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (width > 0 && height > 0 && (ModelLoader.this.width != width || ModelLoader.this.height != height)) {
					ModelLoader.this.width = width;
					ModelLoader.this.height = height;
				}
			}
		});
		glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (action != GLFW_RELEASE) {
					return;
				}
				if (key == GLFW_KEY_ESCAPE) {
					glfwSetWindowShouldClose(window, true);
				}
				if (key == GLFW_KEY_SPACE) {

					for (Entry<ShaderProgram.Mesh, Integer> entry : model.meshToMaterials.entrySet()) {

						if (entry.getValue() == maxMaterials - 1) {
							entry.setValue(0);
						} else {
							entry.setValue(entry.getValue() + 1);
						}
						Attribute matIndex = entry.getKey().instanceAttributes.get("materialIndex").get();
						matIndex.data.clear();
						matIndex.data.add(entry.getValue());
						matIndex.update.set(true);
					}

				}
			}
		});
		glfwSetScrollCallback(window, sCallback = new GLFWScrollCallback() {
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				if (yoffset < 0) {
					fov *= 1.05f;
				} else {
					fov *= 1f / 1.05f;
				}
				if (fov < 10.0f) {
					fov = 10.0f;
				} else if (fov > 120.0f) {
					fov = 120.0f;
				}
			}
		});

		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
		glfwMakeContextCurrent(window);
		glfwSwapInterval(0);
		glfwSetCursorPos(window, width / 2, height / 2);

		try (MemoryStack frame = MemoryStack.stackPush()) {
			IntBuffer framebufferSize = frame.mallocInt(2);
			nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
			width = framebufferSize.get(0);
			height = framebufferSize.get(1);
		}
		caps = GL.createCapabilities();

		/*
		if (!caps.GL_shader_objects) {
			throw new AssertionError("This demo requires the _shader_objects extension.");
		}
		if (!caps.GL_vertex_shader) {
			throw new AssertionError("This demo requires the _vertex_shader extension.");
		}
		if (!caps.GL_fragment_shader) {
			throw new AssertionError("This demo requires the _fragment_shader extension.");
		}*/
		debugProc = GLUtil.setupDebugMessageCallback();

		glClearColor(0f, 0f, 0f, 1f);
		glEnable(GL_DEPTH_TEST);

		/* Create all needed GL resources */
		createProgram();

		loadModel();

		/* Show window */
		glfwShowWindow(window);
	}

	void loadModel() throws IOException {

		//TODO import
		ByteBuffer file = IOUtil.ioResourceToByteBuffer("resources/magnet.obj", 2048 * 8);

		scene =
			aiImportFileFromMemory(
				file,
				0 |
					//				aiProcess_JoinIdenticalVertices | 
					aiProcess_Triangulate
				//				aiProcessPreset_TargetRealtime_MaxQuality | 
				//					| aiProcess_FindDegenerates | aiProcess_GenNormals | aiProcess_FixInfacingNormals | aiProcess_GenSmoothNormals
				//				aiProcess_ImproveCacheLocality |
				//				aiProcess_SortByPType,
				,
				(ByteBuffer) null);
		if (scene == null) {
			throw new IllegalStateException(aiGetErrorString());
		}
		storeMaterialBuffer();

		if (true) {
			int meshCount = scene.mNumMeshes();
			PointerBuffer meshesBuffer = scene.mMeshes();
			for (int i = 0; i < meshCount; ++i) {
				AIMesh mesh = AIMesh.create(meshesBuffer.get(i));
				int mIndex = 0;
				ShaderProgram.Mesh dmesh = setupDiscard(mesh, mIndex, model.translation);
				ShaderProgramSystem.loadVAO(program, dmesh);

				System.out.println(i);
				model.meshToMaterials.put(dmesh, mIndex);
				light.meshToMaterials.put(dmesh, mIndex + 1);
				dmesh.renderCount = 2;

			}
		}
	}

	static final String transformMatName = "meshTransformMatrix";
	static final String normalMatName = "meshNormalMatrix";
	static final String materialIndexName = "meshMaterialIndex";

	void createProgram() throws IOException {

		program = new ShaderProgram();
		String fragSource = ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/assimp.frag"));
		fragSource = fragSource.replaceAll("\\{maxmats\\}", "" + maxMaterials);
		fragSource = fragSource.replaceAll("\\{maxlights\\}", maxLights + "");
		String vertSource = ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/assimp.vert"));
		vertSource = vertSource.replaceAll("\\{maxlights\\}", maxLights + "");
		program.shaderSources.put(GLShaderType.VertexShader, vertSource);
		program.shaderSources.put(GLShaderType.FragmentShader, fragSource);

		ShaderProgramSystem.create(program);

		glUseProgram(program.programID);

		if (true) {

			int actualIndex = GL46.glGetUniformBlockIndex(program.programID, "Matrices");
			matrixBuffer = GL46.glGenBuffers();
			glBindBuffer(GL46.GL_UNIFORM_BUFFER, matrixBuffer);
			glBufferData(GL_UNIFORM_BUFFER, 64 * 3, GL_DYNAMIC_DRAW);
			float[] store = new float[16];
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 0, projectionMatrix.get(store));
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64, viewMatrix.get(store));
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 128, viewProjectionMatrix.get(store));
			glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);

			//binds the buffer to a binding index
			glBindBufferBase(GL_UNIFORM_BUFFER, vpMatrixBindingpoint, matrixBuffer);
			//binds the binding index to the interface block (by index)
			glUniformBlockBinding(program.programID, actualIndex, vpMatrixBindingpoint);

		}
		/*
				if (true) {
		
					int actualIndex = GL46.glGetUniformBlockIndex(program.programID, "LightBlock");
					lightBuffer = GL46.glGenBuffers();
					glBindBuffer(GL46.GL_UNIFORM_BUFFER, lightBuffer);
					glBufferData(GL_UNIFORM_BUFFER, maxLights * (3 + 1) * 2 * 4, GL_DYNAMIC_DRAW);
		
					glBufferSubData(GL46.GL_UNIFORM_BUFFER, 0, new float[] { 0, 0, 0 });
					glBufferSubData(GL46.GL_UNIFORM_BUFFER, 4, new float[] { 0, 0, 0 });
					glBufferSubData(GL46.GL_UNIFORM_BUFFER, 7, new float[] { 1 });
					glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
		
					//binds the buffer to a binding index
					glBindBufferBase(GL_UNIFORM_BUFFER, lightBufferBindingPoint, lightBuffer);
					//binds the binding index to the interface block (by index)
					glUniformBlockBinding(program.programID, actualIndex, lightBufferBindingPoint);
		
				}*/

		lightPositionUniform = glGetUniformLocation(program.programID, "uLightPosition");
		lightColourUniform = glGetUniformLocation(program.programID, "uLightColour");

		for (int i = 0; i < maxMaterials; i++) {
			materialsUniformBlock[i] = glGetUniformLocation(program.programID, "materials[" + i + "]");

		}

		/*ambientColorUniform = glGetUniformLocation(program.programID, "uAmbientColor");
		diffuseColorUniform = glGetUniformLocation(program.programID, "uDiffuseColor");
		specularColorUniform = glGetUniformLocation(program.programID, "uSpecularColor");
		*/

	}

	Wrapper<Long> prev = new WrapperImpl<>(System.nanoTime());
	float total = 0;
	float rotation = 0;

	void update() {
		long curr = System.nanoTime();
		long delta = curr - prev.getWrappedOrDefault(System.nanoTime());
		float deltaSeconds = FrameUtils.nanosToSeconds(delta);
		total += deltaSeconds;
		prev.setWrapped(curr);
		rotation = total * 0.25f * (float) Math.PI;

		projectionMatrix
			.setPerspective(
				(float) Math.toRadians(fov),
				(float) width / height,
				0.01f,
				100.0f);
		viewPosition.set(10f * (float) Math.cos(rotation), 10f, 10f * (float) Math.sin(rotation));
		viewMatrix
			.setLookAt(
				viewPosition.x,
				viewPosition.y,
				viewPosition.z,
				0f,
				0f,
				0f,
				0f,
				1f,
				0f);
		projectionMatrix.mul(viewMatrix, viewProjectionMatrix);
		float rotation2 = -total * 0.75f * (float) Math.PI;
		lightPosition.set(5f * (float) Math.sin(rotation2), 0, 5f * (float) Math.cos(rotation2));
	}

	class Model2 {
		WeakReference<ShaderProgram.Mesh> mesh;
		int materialIndex;
		/**
		 * This is the index of the mesh data that this model represents
		 */
		int meshDataIndex;
	}

	class Model {

		Map<ShaderProgram.Mesh, Integer> meshToMaterials = new CollectionSuppliers.MapSupplier<ShaderProgram.Mesh, Integer>().get();
		Matrix4f translation = new Matrix4f();
	}

	private void updateModel(Model model, int index) {

		for (Entry<ShaderProgram.Mesh, Integer> submeshEntry : model.meshToMaterials.entrySet()) {
			Attribute matIndex = submeshEntry.getKey().instanceAttributes.get("materialIndex").get();

			GL46.glBindBuffer(matIndex.bufferType.getGLID(), matIndex.VBOID);
			GL46.glBufferSubData(matIndex.bufferType.getGLID(), index * ShaderProgram.GLDataType.Float.getBytes(), new float[] { submeshEntry.getValue() });
			GL46.glBindBuffer(matIndex.bufferType.getGLID(), 0);

			Attribute modelMatrixAtt = submeshEntry.getKey().instanceAttributes.get("modelTransformMatrix").get();
			GL46.glBindBuffer(modelMatrixAtt.bufferType.getGLID(), modelMatrixAtt.VBOID);
			GL46.glBufferSubData(modelMatrixAtt.bufferType.getGLID(), index * 16 * ShaderProgram.GLDataType.Float.getBytes(), model.translation.get(new float[16]));
			GL46.glBindBuffer(modelMatrixAtt.bufferType.getGLID(), 0);

			Attribute normalMatrixAtt = submeshEntry.getKey().instanceAttributes.get("modelNormalMatrix").get();
			Matrix3f normalMatrix = new Matrix3f();
			normalMatrix.set(model.translation).invert().transpose();

			GL46.glBindBuffer(normalMatrixAtt.bufferType.getGLID(), modelMatrixAtt.VBOID);
			GL46.glBufferSubData(normalMatrixAtt.bufferType.getGLID(), index * 9 * ShaderProgram.GLDataType.Float.getBytes(), normalMatrix.get(new float[9]));
			GL46.glBindBuffer(normalMatrixAtt.bufferType.getGLID(), 0);

		}

	}

	void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		glUseProgram(program.programID);

		glBindBuffer(GL46.GL_UNIFORM_BUFFER, matrixBuffer);
		float[] store = new float[16];
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 0, projectionMatrix.get(store));
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64, viewMatrix.get(store));
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 128, viewProjectionMatrix.get(store));

		glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
		//		updateModel(model);
		//		light.translation.setTranslation(lightPosition);

		//		updateModel(light, 1);

		//		System.out.println(lightPosition + " " + light.translation.getTranslation(new Vector3f()));
		/*	
			glBindBuffer(GL_ARRAY_BUFFER, mesh.vertexArrayBuffer);
			glVertexAttribPointer(vertexAttribute, 3, GL_FLOAT, false, 0, 0);
			glBindBuffer(GL_ARRAY_BUFFER, mesh.normalArrayBuffer);
			glVertexAttribPointer(normalAttribute, 3, GL_FLOAT, false, 0, 0);
			glBindBuffer(GL_ARRAY_BUFFER, mesh.materialIndexBuffer);
			glVertexAttribPointer(materialIndexAttrib, 1, GL_FLOAT, false, 0, 0);
		
			IntBuffer buffer = BufferUtils.createIntBuffer(1);
			buffer.put(mesh.materialIndex);
			buffer.flip();
			glBufferData(GL_ARRAY_BUFFER, buffer, ShaderProgram.BufferUsage.StreamDraw.getGLID());
		
			GL46.glVertexAttribDivisor(materialIndexAttrib, 1);
		*/
		/*
				glUniformMatrix4fv(modelMatrixUniform, false, modelMatrix.get(modelMatrixBuffer));
				normalMatrix.set(modelMatrix).invert().transpose();
				glUniformMatrix3fv(normalMatrixUniform, false, normalMatrix.get(normalMatrixBuffer));*/

		//		glUniformMatrix4fv(viewProjectionMatrixUniform, false, viewProjectionMatrix.get(viewProjectionMatrixBuffer));

		glUniform3fv(lightPositionUniform, lightPosition.get(lightPositionBuffer));
		glUniform3fv(lightColourUniform, new float[] { 1, 0, 0 });

		//		glUniform3fv(viewPositionUniform, viewPosition.get(viewPositionBuffer));
		/*
					glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mesh.elementArrayBuffer);
					glDrawElements(GL_TRIANGLES, mesh.elementCount, GL_UNSIGNED_INT, 0);*/

		//				ShaderProgramSystem.render(program);

		ShaderProgramSystem.tryRender(program);

	}

	private void storeMaterialBuffer() {

		int i = 0;

		{
			Material m = new Material();
			m.ambient.set(1, 0, 0);
			m.diffuse.set(1, 0, 0);
			float[] data = m.toArray();
			GL46.glUniformMatrix3fv(materialsUniformBlock[i++], false, data);

		}
		{
			Material m = new Material();
			m.ambient.set(0, 1, 0);
			m.diffuse.set(0, 1, 0);

			float[] data = m.toArray();
			GL46.glUniformMatrix3fv(materialsUniformBlock[i++], false, data);

		}
		{
			Material m = new Material();
			m.ambient.set(0, 0, 1);
			m.diffuse.set(0, 0, 1);
			float[] data = m.toArray();
			GL46.glUniformMatrix3fv(materialsUniformBlock[i++], false, data);

		}

	}

	static class Material {
		Vector3f ambient = new Vector3f(), diffuse = new Vector3f();
		float ambientStrength = 0.5f, diffuseStrength = 0.5f;
		//		float shininess = 4f;

		public float[] toArray(float[] arr) {
			int i = 0;
			arr[i++] = ambient.x;
			arr[i++] = ambient.y;
			arr[i++] = ambient.z;

			arr[i++] = diffuse.x;
			arr[i++] = diffuse.y;
			arr[i++] = diffuse.z;

			arr[i++] = ambientStrength;
			arr[i++] = diffuseStrength;
			//			arr[i++] = shininess;

			return arr;

		}

		public float[] toArray() {
			return toArray(new float[sizeOf()]);
		}

		public static int sizeOf() {
			return 9;
		}

	}

	/*
		public void createMaterial(AIMaterial AImaterial, float[] data) {
	
			AIColor4D ambient = AIColor4D.create();
			if (aiGetMaterialColor(AImaterial, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE, 0, ambient) != 0) {
				logger.warn("no ambient found\r\t\n" + aiGetErrorString());
				ambient.set(0, 0, 0, 1);
	
			}
	
			AIColor4D diffuse = AIColor4D.create();
	
			if (aiGetMaterialColor(AImaterial, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, diffuse) != 0) {
				logger.warn("no diffuse found\r\t\n" + aiGetErrorString());
				diffuse.set(0, 0, 0, 1);
			}
	
			AIColor4D specular = AIColor4D.create();
			if (aiGetMaterialColor(AImaterial, AI_MATKEY_COLOR_SPECULAR, aiTextureType_NONE, 0, specular) != 0) {
				logger.warn("no specular found\r\t\n" + aiGetErrorString());
				specular.set(0, 0, 0, 1);
			}
	
			int i = 0;
			//ambient
			data[i++] = ambient.r();
			data[i++] = ambient.g();
			data[i++] = ambient.b();
			//		data[i++] = ambient.a();
			//put diffuse
			data[i++] = diffuse.r();
			data[i++] = diffuse.g();
			data[i++] = diffuse.b();
			//		data[i++] = diffuse.a();
			//put specular
			data[i++] = specular.r();
			data[i++] = specular.g();
			data[i++] = specular.b();
			//		data[i++] = specular.a();
	
		}*/

	private ShaderProgram.Mesh setupDiscard(AIMesh aim, int matIndex, Matrix4f modelMatrix) {
		ShaderProgram.Mesh mesh = new ShaderProgram.Mesh();
		{
			Attribute vertexAttrib = new Attribute();
			vertexAttrib.name = "vertex";
			vertexAttrib.bufferType = BufferType.ArrayBuffer;
			vertexAttrib.dataType = GLDataType.Float;
			vertexAttrib.bufferUsage = BufferUsage.StaticDraw;
			vertexAttrib.dataSize = 3;
			vertexAttrib.normalised = false;

			AIVector3D.Buffer vertices = aim.mVertices();
			vertexAttrib.bufferAddress = vertices.address();
			vertexAttrib.bufferLen = (long) (AIVector3D.SIZEOF * vertices.remaining());
			vertexAttrib.bufferResourceType = VBOBufferType.Address;
			mesh.VBOs.add(vertexAttrib);
		}
		{
			Attribute normalAttrib = new Attribute();
			normalAttrib.name = "normal";
			normalAttrib.bufferType = BufferType.ArrayBuffer;
			normalAttrib.dataType = GLDataType.Float;
			normalAttrib.bufferUsage = BufferUsage.StaticDraw;
			normalAttrib.dataSize = 3;
			normalAttrib.normalised = false;
			normalAttrib.instanced = false;
			AIVector3D.Buffer normals = aim.mNormals();
			normalAttrib.bufferAddress = normals.address();
			normalAttrib.bufferLen = (long) (AIVector3D.SIZEOF * normals.remaining());
			normalAttrib.bufferResourceType = VBOBufferType.Address;
			mesh.VBOs.add(normalAttrib);
		}
		{
			VBO elementAttrib = new VBO();
			elementAttrib.bufferType = BufferType.ElementArrayBuffer;
			elementAttrib.bufferUsage = BufferUsage.StaticDraw;
			elementAttrib.dataType = GLDataType.UnsignedInt;
			//		elementAttrib.dataSize = 3;
			mesh.VBOs.add(elementAttrib);
			elementAttrib.bufferResourceType = VBOBufferType.Buffer;
			int elementCount;
			int faceCount = aim.mNumFaces();
			elementCount = faceCount * 3;
			IntBuffer elementArrayBufferData = BufferUtils.createIntBuffer(elementCount);
			AIFace.Buffer facesBuffer = aim.mFaces();
			for (int i = 0; i < faceCount; ++i) {
				AIFace face = facesBuffer.get(i);
				if (face.mNumIndices() != 3) {

				} else {
					elementArrayBufferData.put(face.mIndices());
				}
			}
			elementArrayBufferData.flip();
			elementAttrib.buffer = elementArrayBufferData;
			mesh.index = new WeakReference<ShaderProgram.VBO>(elementAttrib);
			mesh.vertexCount = elementCount;
		}
		{
			Attribute materialIndexAttrib = new Attribute();
			materialIndexAttrib.name = "materialIndex";
			materialIndexAttrib.bufferType = BufferType.ArrayBuffer;
			materialIndexAttrib.dataType = GLDataType.Float;
			materialIndexAttrib.bufferUsage = BufferUsage.DynamicDraw;
			materialIndexAttrib.dataSize = 1;
			materialIndexAttrib.normalised = false;
			materialIndexAttrib.instanced = true;
			materialIndexAttrib.instanceStride = 1;
			materialIndexAttrib.bufferResourceType = VBOBufferType.List;
			mesh.VBOs.add(materialIndexAttrib);
			materialIndexAttrib.data.add(matIndex);
			mesh.instanceAttributes.put("materialIndex", new WeakReference<>(materialIndexAttrib));
		}
		{
			Attribute meshTransformAttrib = new Attribute();
			meshTransformAttrib.name = "modelMatrix";
			meshTransformAttrib.bufferType = BufferType.ArrayBuffer;
			meshTransformAttrib.dataType = GLDataType.Float;
			meshTransformAttrib.bufferUsage = BufferUsage.DynamicDraw;
			meshTransformAttrib.dataSize = 16;
			meshTransformAttrib.normalised = false;
			meshTransformAttrib.instanced = true;
			meshTransformAttrib.instanceStride = 1;
			meshTransformAttrib.bufferResourceType = VBOBufferType.List;
			mesh.VBOs.add(meshTransformAttrib);
			FrameUtils.appendToList(meshTransformAttrib.data, modelMatrix);
			mesh.instanceAttributes.put("modelMatrix", new WeakReference<>(meshTransformAttrib));
		}
		mesh.modelRenderType = GLDrawMode.Triangles;

		return mesh;

	}
}
