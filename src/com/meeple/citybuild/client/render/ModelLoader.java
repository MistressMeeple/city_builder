package com.meeple.citybuild.client.render;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.glfw.GLFWCursorPosCallback;
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

import com.meeple.citybuild.client.render.ModelLoader.Scene.Mesh;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.OGL.IShaderUniformUploadSystem;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.ShaderProgram.VBO;
import com.meeple.shared.frame.OGL.ShaderProgramSystem;
import com.meeple.shared.frame.OGL.ShaderProgramSystem.ShaderClosable;
import com.meeple.shared.frame.OGL.UniformManager;
import com.meeple.shared.frame.nuklear.IOUtil;

/**
 * Shows how to load models in Wavefront obj and mlt format with Assimp binding and render them with
 * OpenGL.
 *
 * @author Zhang Hai
 */
public class ModelLoader {

	private static Logger logger = Logger.getLogger(ModelLoader.class);

	private static String debugLayout = "[%r][%d{HH:mm:ss:SSS}][%t][%p] (%F:%L) %m%n";
	private static final int maxMaterials = 10;

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
	float rotation;

	//shader program
	ShaderProgram program;
	//vertex shader inputs
	int vertexAttribute;
	int normalAttribute;
	int modelMatrixUniform;
	int materialIndexAttrib;
	//vertex shader uniforms
	int viewProjectionMatrixUniform;
	int normalMatrixUniform;
	//fragment shader uniforms
	int materialsUniformBlock[] = new int[maxMaterials];

	int ambientColorUniform;
	int diffuseColorUniform;
	int specularColorUniform;

	int lightPositionUniform;
	int viewPositionUniform;

	Scene scene;

	Matrix4f modelMatrix = new Matrix4f().rotateY(0.5f * (float) Math.PI).scale(1.5f, 1.5f, 1.5f);
	Matrix4f viewMatrix = new Matrix4f();
	Matrix4f projectionMatrix = new Matrix4f();
	Matrix4f viewProjectionMatrix = new Matrix4f();
	Vector3f viewPosition = new Vector3f();
	Vector3f lightPosition = new Vector3f(-5f, 5f, 5f);

	private FloatBuffer modelMatrixBuffer = BufferUtils.createFloatBuffer(4 * 4);
	private FloatBuffer viewProjectionMatrixBuffer = BufferUtils.createFloatBuffer(4 * 4);
	private Matrix3f normalMatrix = new Matrix3f();
	private FloatBuffer normalMatrixBuffer = BufferUtils.createFloatBuffer(3 * 3);
	private FloatBuffer lightPositionBuffer = BufferUtils.createFloatBuffer(3);
	private FloatBuffer viewPositionBuffer = BufferUtils.createFloatBuffer(3);

	GLCapabilities caps;
	GLFWKeyCallback keyCallback;
	GLFWFramebufferSizeCallback fbCallback;
	GLFWWindowSizeCallback wsCallback;
	GLFWCursorPosCallback cpCallback;
	GLFWScrollCallback sCallback;
	Callback debugProc;

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
			loop();
			scene.free();
			if (debugProc != null) {
				debugProc.free();
			}
			cpCallback.free();
			keyCallback.free();
			fbCallback.free();
			wsCallback.free();
			glfwDestroyWindow(window);
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

		System.out.println("Move the mouse to look around");
		System.out.println("Zoom in/out with mouse wheel");
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
					for (Scene.Mesh mesh : scene.meshes) {
						if (mesh.materialIndex == maxMaterials - 1) {
							mesh.materialIndex = 0;
						} else {
							mesh.materialIndex += 1;

						}
					}

				}
			}
		});
		glfwSetCursorPosCallback(window, cpCallback = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double x, double y) {
				rotation = ((float) x / width - 0.5f) * 2f * (float) Math.PI;
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

		ByteBuffer file = IOUtil.ioResourceToByteBuffer("resources/spaceship.blend", 2048 * 8);

		AIScene scene =
			aiImportFileFromMemory(
				file,
				0 |
					//				aiProcess_JoinIdenticalVertices | 
					aiProcess_Triangulate
					//				aiProcessPreset_TargetRealtime_MaxQuality | 
					| aiProcess_FindDegenerates
				//				aiProcess_ImproveCacheLocality |
				//				aiProcess_SortByPType,
				,
				(ByteBuffer) null);
		if (scene == null) {
			throw new IllegalStateException(aiGetErrorString());
		}
		storeMaterialBuffer(scene);
		this.scene = new Scene(scene);

	}

	void createProgram() throws IOException {

		program = new ShaderProgram();
		String fragSource = ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/assimp.frag"));
		fragSource = fragSource.replaceAll("\\{maxmats\\}", "" + maxMaterials);
		String vertSource = ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/assimp.vert"));
		program.shaderSources.put(GLShaderType.VertexShader, vertSource);
		program.shaderSources.put(GLShaderType.FragmentShader, fragSource);

		ShaderProgramSystem.create(program);

		glUseProgram(program.programID);
		vertexAttribute = glGetAttribLocation(program.programID, "vertex");
		glEnableVertexAttribArray(vertexAttribute);

		normalAttribute = glGetAttribLocation(program.programID, "normal");
		glEnableVertexAttribArray(normalAttribute);

		materialIndexAttrib = glGetAttribLocation(program.programID, "materialIndex");
		glEnableVertexAttribArray(materialIndexAttrib);

		modelMatrixUniform = glGetUniformLocation(program.programID, "modelMatrix");

		viewProjectionMatrixUniform = glGetUniformLocation(program.programID, "vpMatrix");
		normalMatrixUniform = glGetUniformLocation(program.programID, "normalMatrix");
		lightPositionUniform = glGetUniformLocation(program.programID, "uLightPosition");
		viewPositionUniform = glGetUniformLocation(program.programID, "uViewPosition");

		for (int i = 0; i < maxMaterials; i++) {
			materialsUniformBlock[i] = glGetUniformLocation(program.programID, "materials[" + i + "]");
		}

		ambientColorUniform = glGetUniformLocation(program.programID, "uAmbientColor");
		diffuseColorUniform = glGetUniformLocation(program.programID, "uDiffuseColor");
		specularColorUniform = glGetUniformLocation(program.programID, "uSpecularColor");

		System.out.println();
	}

	void update() {
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
	}

	private ShaderProgram.Mesh setupDiscard(Mesh meshData) {
		ShaderProgram.Mesh mesh = new ShaderProgram.Mesh();
		AIMesh aim = AIMesh.create(meshData.mesh);

		Attribute positionAttrib = new Attribute();
		Attribute normalAttrib = new Attribute();
		Attribute matIndexAttrib = new Attribute();
		VBO elementAttrib = new VBO();
		positionAttrib.name = "vertex";
		positionAttrib.bufferType = BufferType.ArrayBuffer;
		positionAttrib.dataType = GLDataType.Float;
		positionAttrib.bufferUsage = BufferUsage.StreamDraw;
		positionAttrib.dataSize = 3;
		positionAttrib.normalised = false;
		mesh.VBOs.add(positionAttrib);
		{

			AIVector3D.Buffer vertices = aim.mVertices();
			vertices.forEach(new Consumer<AIVector3D>() {

				@Override
				public void accept(AIVector3D t) {
					positionAttrib.data.add(t.x());
					positionAttrib.data.add(t.y());
					positionAttrib.data.add(t.z());

				}
			});

		}

		normalAttrib.name = "normal";
		normalAttrib.bufferType = BufferType.ArrayBuffer;
		normalAttrib.dataType = GLDataType.Float;
		normalAttrib.bufferUsage = BufferUsage.StreamDraw;
		normalAttrib.dataSize = 3;
		normalAttrib.normalised = false;
		normalAttrib.instanced = false;
		mesh.VBOs.add(normalAttrib);
		{
			AIVector3D.Buffer normals = aim.mNormals();
			normals.forEach(new Consumer<AIVector3D>() {

				@Override
				public void accept(AIVector3D t) {
					normalAttrib.data.add(t.x());
					normalAttrib.data.add(t.y());
					normalAttrib.data.add(t.z());

				}
			});
		}

		elementAttrib.bufferType = BufferType.ElementArrayBuffer;
		elementAttrib.dataType = GLDataType.UnsignedInt;
		elementAttrib.bufferUsage = BufferUsage.StaticDraw;
		elementAttrib.dataSize = 3;
		mesh.VBOs.add(elementAttrib);
		{

			aim.mFaces().forEach(new Consumer<AIFace>() {

				@Override
				public void accept(AIFace t) {
					if (t.mNumIndices() != 3) {
						//						logger.warn("AIFace.mNumIndices() != 3, actually has " + t.mNumIndices());
						//						logger.warn("wont use this face");
					} else {
						int[] indicies = new int[3];
						t.mIndices().get(indicies);
						for (int i : indicies) {
							elementAttrib.data.add(i);
						}
					}
				}
			});

		}

		matIndexAttrib.name = "materialIndex";
		matIndexAttrib.bufferType = BufferType.ArrayBuffer;
		matIndexAttrib.dataType = GLDataType.Int;
		matIndexAttrib.bufferUsage = BufferUsage.StreamDraw;
		matIndexAttrib.dataSize = 1;
		matIndexAttrib.normalised = false;
		matIndexAttrib.instanced = true;
		matIndexAttrib.instanceStride = 1;
		mesh.VBOs.add(matIndexAttrib);
		matIndexAttrib.data.add(meshData.materialIndex);

		mesh.vertexCount = elementAttrib.data.size() / 3;
		mesh.modelRenderType = GLDrawMode.Triangles;
		mesh.singleFrameDiscard = true;

		return mesh;

	}

	void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		glUseProgram(program.programID);

		for (Scene.Mesh mesh : scene.meshes) {
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

			glUniformMatrix4fv(modelMatrixUniform, false, modelMatrix.get(modelMatrixBuffer));

			glUniformMatrix4fv(viewProjectionMatrixUniform, false, viewProjectionMatrix.get(viewProjectionMatrixBuffer));
			normalMatrix.set(modelMatrix).invert().transpose();
			glUniformMatrix3fv(normalMatrixUniform, false, normalMatrix.get(normalMatrixBuffer));
			glUniform3fv(lightPositionUniform, lightPosition.get(lightPositionBuffer));
			glUniform3fv(viewPositionUniform, viewPosition.get(viewPositionBuffer));

			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mesh.elementArrayBuffer);
			glDrawElements(GL_TRIANGLES, mesh.elementCount, GL_UNSIGNED_INT, 0);
		}

		//				ShaderProgramSystem.render(program);
	}

	private void storeMaterialBuffer(AIScene scene) {

		PointerBuffer mats = scene.mMaterials();
		int i = 0;

		float[] data = new float[Material.sizeOf()];
		Material m = new Material();
		m.diffuse.set(1, 0, 0);
		m.ambient.set(0, 1, 0);
		m.specular.set(0, 0, 1);
		{

			m.diffuseStrength = 0.5f;
			m.toArray(data);
			GL46.glUniformMatrix4fv(materialsUniformBlock[i++], false, data);
			Material.toString(data);
		}
		{
			m.diffuseStrength = 0f;
			m.toArray(data);
			GL46.glUniformMatrix4fv(materialsUniformBlock[i++], false, data);
			Material.toString(data);
		}
		{
			m.diffuseStrength = 1f;
			m.toArray(data);
			GL46.glUniformMatrix4fv(materialsUniformBlock[i++], false, data);
			Material.toString(data);
		}
		//		glUniformMatrix3x4fv(materialsUniformBlock[i++], false, data);
		int matNum = scene.mNumMaterials();
		for (; i < scene.mNumMaterials(); i++) {
			AIMaterial material = AIMaterial.createSafe(mats.get(i));
			if (material != null) {
				float[] materialDataBuffer = new float[4 * 3];
				createMaterial(material, materialDataBuffer);
				GL46.glUniformMatrix4fv(materialsUniformBlock[i++], false, materialDataBuffer);
				Material.toString(materialDataBuffer);
			}
		}
		Material material = new Material();
		material.ambient.set(1, 0, 0);
		material.diffuse.set(1, 0, 0);
		material.specular.set(0, 0, 1);
		float[] d = material.toArray();

		GL46.glUniformMatrix4fv(materialsUniformBlock[i++], false, d);
		Material.toString(d);

	}

	static class Material {
		Vector3f ambient = new Vector3f(), diffuse = new Vector3f(), specular = new Vector3f();
		float ambientStrength = 0.5f, diffuseStrength = 0.5f, specularStrength = 0.5f;
		float shininess = 4f;

		public float[] toArray(float[] arr) {
			int i = 0;
			arr[i++] = ambient.x;
			arr[i++] = ambient.y;
			arr[i++] = ambient.z;
			arr[i++] = ambientStrength;

			arr[i++] = diffuse.x;
			arr[i++] = diffuse.y;
			arr[i++] = diffuse.z;
			arr[i++] = diffuseStrength;

			arr[i++] = specular.x;
			arr[i++] = specular.y;
			arr[i++] = specular.z;
			arr[i++] = specularStrength;

			arr[i++] = shininess;
			arr[i++] = 0;
			arr[i++] = 0;
			arr[i++] = 0;

			return arr;

		}

		public float[] toArray() {
			return toArray(new float[sizeOf()]);
		}

		public static int sizeOf() {
			return 16;
		}

		public static void toString(float[] data) {
			int i = 0;
			System.out.println("ambient : " + data[i++] + " " + data[i++] + " " + data[i++] /*+ " x " + data[i++]*/);
			System.out.println("diffuse : " + data[i++] + " " + data[i++] + " " + data[i++] /*+ " x " + data[i++]*/);
			System.out.println("specular: " + data[i++] + " " + data[i++] + " " + data[i++] /*+ " x " + data[i++]*/);
			//			System.out.println("shininess: " + data[i++] + " " + data[i++] + " " + data[i++] + " " + data[i++]);
		}
	}

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

	}

	class Scene {

		public AIScene scene;
		public List<Mesh> meshes;

		public Scene(AIScene scene) {

			this.scene = scene;

			if (true) {
				int meshCount = scene.mNumMeshes();
				PointerBuffer meshesBuffer = scene.mMeshes();
				meshes = new ArrayList<>();
				for (int i = 0; i < meshCount; ++i) {
					meshes.add(new Mesh(AIMesh.create(meshesBuffer.get(i))));
				}
			}

		}

		public void free() {
			aiReleaseImport(scene);
			scene = null;
			meshes = null;
		}

		public class Model {
			Set<Integer> meshes = new CollectionSuppliers.SetSupplier<Integer>().get();
			Matrix4f modelTranslation = new Matrix4f();
			String name;

		}

		public class Mesh {

			public int materialIndex = 0;
			public long mesh;
			public int materialIndexBuffer;
			public int vertexArrayBuffer;
			public int normalArrayBuffer;
			public int elementArrayBuffer;
			public int elementCount;

			public Mesh(AIMesh mesh) {
				this.mesh = mesh.address();
				this.materialIndex = mesh.mMaterialIndex();
				//material index buffer
				if (true) {
					materialIndexBuffer = glGenBuffers();
					glBindBuffer(GL_ARRAY_BUFFER, materialIndexBuffer);
					IntBuffer buffer = BufferUtils.createIntBuffer(1);
					buffer.put(materialIndex);
					buffer.flip();
					glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
				}
				//vertices
				if (true) {
					List<Vector3f> verts = new ArrayList<>();
					vertexArrayBuffer = glGenBuffers();
					glBindBuffer(GL_ARRAY_BUFFER, vertexArrayBuffer);

					AIVector3D.Buffer vertices = mesh.mVertices();
					int rem = vertices.remaining();
					int size = AIVector3D.SIZEOF * rem;

					nglBufferData(GL_ARRAY_BUFFER, size, vertices.address(), GL_STATIC_DRAW);
					vertices.forEach(new Consumer<AIVector3D>() {

						@Override
						public void accept(AIVector3D vec) {
							verts.add(new Vector3f(vec.x(), vec.y(), vec.z()));
						}
					});
					System.out.println(rem + " " + verts.size());
					System.out.println();
				}
				//normals
				if (true) {
					normalArrayBuffer = glGenBuffers();
					glBindBuffer(GL_ARRAY_BUFFER, normalArrayBuffer);
					AIVector3D.Buffer normals = mesh.mNormals();
					nglBufferData(
						GL_ARRAY_BUFFER,
						AIVector3D.SIZEOF * normals.remaining(),
						normals.address(),
						GL_STATIC_DRAW);
				}
				//element indicies
				if (true) {
					int faceCount = mesh.mNumFaces();
					elementCount = faceCount * 3;
					IntBuffer elementArrayBufferData = BufferUtils.createIntBuffer(elementCount);
					AIFace.Buffer facesBuffer = mesh.mFaces();
					for (int i = 0; i < faceCount; ++i) {
						AIFace face = facesBuffer.get(i);
						if (face.mNumIndices() != 3) {
							logger.warn("AIFace.mNumIndices() != 3, actually has " + face.mNumIndices());
							logger.warn("wont use this face");
						} else {
							elementArrayBufferData.put(face.mIndices());
						}
					}
					elementArrayBufferData.flip();
					elementArrayBuffer = glGenBuffers();
					glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementArrayBuffer);
					glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementArrayBufferData, GL_STATIC_DRAW);
					glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
				}
			}
		}

	}
}
