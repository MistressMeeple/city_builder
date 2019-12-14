package com.meeple.citybuild.client.render;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

import com.meeple.citybuild.client.render.structs.Light;
import com.meeple.citybuild.client.render.structs.Material;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferDataManagementType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.ShaderProgramSystem;
import com.meeple.shared.frame.OGL.ShaderProgramSystem.ShaderClosable;
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
	private int materialBuffer;

	//fragment shader uniforms
	private int materialsUniformBlock[] = new int[maxMaterials];

	private int lightPositionUniform;
	private int lightColourUniform;
	private int lightStrengthUniform;

	class Model {
		Map<ShaderProgram.Mesh, Integer> meshToMaterials = new CollectionSuppliers.MapSupplier<ShaderProgram.Mesh, Integer>().get();
		Matrix4f translation = new Matrix4f();
	}

	class MeshInstance {
		WeakReference<ShaderProgram.Mesh> mesh;
		int meshDataIndex;
	}

	Model model = new Model();
	Model light = new Model();
	Map<Model, Set<MeshInstance>> instances = new CollectionSuppliers.MapSupplier<Model, Set<MeshInstance>>().get();

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

						/*
							Attribute matIndex = entry.getKey().instanceAttributes.get(materialIndexName).get();
							matIndex.data.clear();
							matIndex.data.add(entry.getValue());
							matIndex.update.set(true);
						*/
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
		ByteBuffer file = IOUtil.ioResourceToByteBuffer("resources/cube.blend", 2048 * 8);

		scene =
			aiImportFileFromMemory(
				file,
				0 |
					//				aiProcess_JoinIdenticalVertices | 
					aiProcess_Triangulate
					//				aiProcessPreset_TargetRealtime_MaxQuality | 
					//					| aiProcess_FindDegenerates 
					| aiProcess_GenNormals | aiProcess_FixInfacingNormals | aiProcess_GenSmoothNormals
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
				ShaderProgram.Mesh dmesh = setupDiscard(mesh, 5);
				ShaderProgramSystem.loadVAO(program, dmesh);

				model.meshToMaterials.put(dmesh, mIndex);
				light.meshToMaterials.put(dmesh, mIndex);
				dmesh.renderCount = 2;

				MeshInstance mi = new MeshInstance();
				mi.mesh = new WeakReference<>(dmesh);
				mi.meshDataIndex = 0;
				FrameUtils.addToSetMap(instances, model, mi, new CollectionSuppliers.SetSupplier<>());

				MeshInstance mi2 = new MeshInstance();
				mi2.mesh = new WeakReference<>(dmesh);
				mi2.meshDataIndex = 1;
				FrameUtils.addToSetMap(instances, light, mi2, new CollectionSuppliers.SetSupplier<>());

			}
		}
	}

	private static final String transformMatName = "meshTransformMatrix";
	private static final String materialIndexName = "meshMaterialIndex";
	private static final String normalMatName = "meshNormalMatrix";

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
			int matrixBuffer = GL46.glGenBuffers();
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
			this.matrixBuffer = matrixBuffer;
		}

		if (false) {

			int actualIndex = GL46.glGetUniformBlockIndex(program.programID, "LightBlock");
			int buffer = GL46.glGenBuffers();
			glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
			glBufferData(
				GL_UNIFORM_BUFFER,
				Light.sizeOf * ShaderProgram.GLDataType.Float.getBytes() * maxLights,
				GL_DYNAMIC_DRAW);

			//			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 0, data);

			glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);

			//binds the buffer to a binding index
			glBindBufferBase(GL_UNIFORM_BUFFER, vpMatrixBindingpoint, buffer);
			//binds the binding index to the interface block (by index)
			glUniformBlockBinding(program.programID, actualIndex, vpMatrixBindingpoint);
			this.lightBuffer = buffer;
		}
		if (false) {
			int actualIndex = GL46.glGetUniformBlockIndex(program.programID, "MaterialBlock");
			int buffer = GL46.glGenBuffers();
			glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
			glBufferData(GL_UNIFORM_BUFFER, (9 + 3/*padding*/) * ShaderProgram.GLDataType.Float.getBytes() * maxMaterials, GL_DYNAMIC_DRAW);
			float[] store = new float[16];
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 0, projectionMatrix.get(store));
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64, viewMatrix.get(store));
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 128, viewProjectionMatrix.get(store));
			glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);

			//binds the buffer to a binding index
			glBindBufferBase(GL_UNIFORM_BUFFER, vpMatrixBindingpoint, buffer);
			//binds the binding index to the interface block (by index)
			glUniformBlockBinding(program.programID, actualIndex, vpMatrixBindingpoint);
			this.materialBuffer = buffer;
		}

		lightPositionUniform = glGetUniformLocation(program.programID, "uLightPosition");
		lightColourUniform = glGetUniformLocation(program.programID, "uLightColour");
		lightStrengthUniform = glGetUniformLocation(program.programID, "uLightStrength");

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
		rotation = 0 * 0.25f * (float) Math.PI;

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
		lightPosition.set(2.5f * (float) Math.sin(rotation2), 5f, 5f * (float) Math.cos(rotation2));
		//		lightPosition.set(5, 0, 0);
		light.translation.setTranslation(lightPosition.x, 0, lightPosition.z);
		//		light.translation.setTranslation(10, 0, 0);
		//		model.translation.translate(0, deltaSeconds, 0);
	}

	private void writeMeshTranslation(MeshInstance instance, Matrix4f translation) {
		try {

			//calculate normal matrix
			Matrix3f normal = new Matrix3f();
			normal.set(translation).invert().transpose();
			float[] data = new Matrix4f(normal).get(new float[16]);
			//upload translation and normal matrix
			writeBuffer(instance, transformMatName, translation.get(new float[16]));
			writeBuffer(instance, normalMatName, data);
		} catch (Exception e) {
			logger.warn("failed to update", e);

		}
	}

	private void writeMeshMaterialIndex(MeshInstance instance, int materialIndex) {
		try {
			writeBuffer(instance, materialIndexName, new float[] { materialIndex });
		} catch (Exception e) {
			logger.warn("failed to update", e);
		}
	}

	private void writeBuffer(MeshInstance instance, String name, float[] data) throws Exception {

		Attribute attrib = instance.mesh.get().instanceAttributes.get(name).get();
		long offset = instance.meshDataIndex * (attrib.dataSize * attrib.dataType.getBytes());

		GL46.glBindBuffer(attrib.bufferType.getGLID(), attrib.VBOID);
		GL46.glBufferSubData(attrib.bufferType.getGLID(), offset, data);
		GL46.glBindBuffer(attrib.bufferType.getGLID(), 0);
	}

	private void writeProjMatrix(int buffer, Matrix4f projection) {

		//no need to be in a program bidning, since this is shared between multiple programs
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
		float[] store = new float[16];
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 0, projection.get(store));
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
	}

	private void writeVPMatrix(int buffer, Matrix4f view, Matrix4f viewProjection) {

		//no need to be in a program bidning, since this is shared between multiple programs
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
		float[] store = new float[16];
		//		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 0, projectionMatrix.get(store));
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64, view.get(store));
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 128, viewProjection.get(store));
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
	}

	private void writeLightPosition(int buffer, int lightIndex, Vector3f position) {
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
		//			((10 + 2/*padding*/) * ShaderProgram.GLDataType.Float.getBytes())

		float[] data = new float[] { position.x, position.y, position.z };
		long offset = lightIndex * 12 + 3;

		glBufferSubData(GL46.GL_UNIFORM_BUFFER, offset, data);
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);

		this.lightBuffer = buffer;
	}

	private void writeLight(int buffer, Matrix4f view, Matrix4f viewProjection) {

		//no need to be in a program bidning, since this is shared between multiple programs
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
		float[] store = new float[16];
		//		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 0, projectionMatrix.get(store));
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64, view.get(store));
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 128, viewProjection.get(store));
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
	}

	void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		writeVPMatrix(matrixBuffer, viewMatrix, viewProjectionMatrix);
		try (ShaderClosable sc = ShaderProgramSystem.useProgram(program)) {

			for (MeshInstance meshInstance : instances.get(model)) {
				writeMeshTranslation(meshInstance, model.translation);
				writeMeshMaterialIndex(meshInstance, model.meshToMaterials.get(meshInstance.mesh.get()));
			}

			for (MeshInstance meshInstance : instances.get(light)) {
				writeMeshTranslation(meshInstance, light.translation);
				writeMeshMaterialIndex(meshInstance, light.meshToMaterials.get(meshInstance.mesh.get()));
			}

			glUniform3fv(lightPositionUniform, lightPosition.get(lightPositionBuffer));
			glUniform3fv(lightColourUniform, new float[] { 1, 0, 0 });
			glUniform3fv(lightStrengthUniform, new float[] { 1, 0.01f, 0.005f });
		}

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
			m.ambient.set(1, 0, 0);
			m.diffuse.set(0, 1, 0);

			float[] data = m.toArray();
			GL46.glUniformMatrix3fv(materialsUniformBlock[i++], false, data);
		}
		{
			Material m = new Material();
			m.ambient.set(0, 1, 0);
			m.diffuse.set(0, 0, 1);
			float[] data = m.toArray();
			GL46.glUniformMatrix3fv(materialsUniformBlock[i++], false, data);
		}
		{
			Material m = new Material();
			m.ambient.set(0, 0, 1);
			m.diffuse.set(0, 0, 0);
			float[] data = m.toArray();
			GL46.glUniformMatrix3fv(materialsUniformBlock[i++], false, data);
		}

		{
			Material m = new Material();
			m.ambient.set(0, 1, 0);
			m.diffuse.set(0, 0, 0);
			float[] data = m.toArray();
			GL46.glUniformMatrix3fv(materialsUniformBlock[i++], false, data);
		}

	}

	private ShaderProgram.Mesh setupDiscard(AIMesh aim, long maxMeshes) {
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
			vertexAttrib.bufferResourceType = BufferDataManagementType.Address;
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
			normalAttrib.bufferResourceType = BufferDataManagementType.Address;
			mesh.VBOs.add(normalAttrib);
		}
		{
			BufferObject elementAttrib = new BufferObject();
			elementAttrib.bufferType = BufferType.ElementArrayBuffer;
			elementAttrib.bufferUsage = BufferUsage.StaticDraw;
			elementAttrib.dataType = GLDataType.UnsignedInt;
			//		elementAttrib.dataSize = 3;
			mesh.VBOs.add(elementAttrib);
			elementAttrib.bufferResourceType = BufferDataManagementType.Buffer;
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
			mesh.index = new WeakReference<ShaderProgram.BufferObject>(elementAttrib);
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
			materialIndexAttrib.bufferResourceType = BufferDataManagementType.Empty;
			materialIndexAttrib.bufferLen = maxMeshes;
			mesh.VBOs.add(materialIndexAttrib);
			mesh.instanceAttributes.put(materialIndexName, new WeakReference<>(materialIndexAttrib));
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
			meshTransformAttrib.bufferResourceType = BufferDataManagementType.Empty;
			meshTransformAttrib.bufferLen = maxMeshes;
			mesh.VBOs.add(meshTransformAttrib);
			//			FrameUtils.appendToList(meshTransformAttrib.data, modelMatrix);
			mesh.instanceAttributes.put(transformMatName, new WeakReference<>(meshTransformAttrib));
		}
		/**
		 * It is important to use a data size of 16 rather than 9 because for some reason the buffer adds padding to vec3 to 4 floats
		 * easier to just make it a 4 float array
		 */
		{
			Attribute meshNormalMatrixAttrib = new Attribute();
			meshNormalMatrixAttrib.name = "normalMatrix";
			meshNormalMatrixAttrib.bufferType = BufferType.ArrayBuffer;
			meshNormalMatrixAttrib.dataType = GLDataType.Float;
			meshNormalMatrixAttrib.bufferUsage = BufferUsage.DynamicDraw;
			meshNormalMatrixAttrib.dataSize = 16;
			meshNormalMatrixAttrib.normalised = false;
			meshNormalMatrixAttrib.instanced = true;
			meshNormalMatrixAttrib.instanceStride = 1;
			meshNormalMatrixAttrib.bufferResourceType = BufferDataManagementType.Empty;
			meshNormalMatrixAttrib.bufferLen = maxMeshes;
			mesh.VBOs.add(meshNormalMatrixAttrib);
			//			FrameUtils.appendToList(meshTransformAttrib.data, modelMatrix);
			mesh.instanceAttributes.put(normalMatName, new WeakReference<>(meshNormalMatrixAttrib));
		}
		mesh.modelRenderType = GLDrawMode.Triangles;

		return mesh;

	}
}
