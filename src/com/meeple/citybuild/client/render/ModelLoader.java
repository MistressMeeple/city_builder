package com.meeple.citybuild.client.render;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.ARBShaderObjects.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.ARBVertexShader.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
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
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.ShaderProgramSystem;
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

	public static void main(String[] args)  {
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
//	int materialIndexAttrib;
	//vertex shader uniforms
	int viewProjectionMatrixUniform;
	int normalMatrixUniform;
	//fragment shader uniforms
	int lightPositionUniform;
	int viewPositionUniform;
	int ambientColorUniform;
	int diffuseColorUniform;
	int specularColorUniform;

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
		if (!caps.GL_ARB_shader_objects) {
			throw new AssertionError("This demo requires the ARB_shader_objects extension.");
		}
		if (!caps.GL_ARB_vertex_shader) {
			throw new AssertionError("This demo requires the ARB_vertex_shader extension.");
		}
		if (!caps.GL_ARB_fragment_shader) {
			throw new AssertionError("This demo requires the ARB_fragment_shader extension.");
		}
		debugProc = GLUtil.setupDebugMessageCallback();

		glClearColor(0f, 0f, 0f, 1f);
		glEnable(GL_DEPTH_TEST);

		/* Create all needed GL resources */
		loadModel();
		createProgram();

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

		program.shaderSources.put(GLShaderType.VertexShader, ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/assimp.vert")));
		program.shaderSources.put(GLShaderType.FragmentShader, ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/assimp.frag")));
		ShaderProgramSystem.create(program);

		glUseProgramObjectARB(program.programID);
		vertexAttribute = glGetAttribLocationARB(program.programID, "vertex");
		glEnableVertexAttribArrayARB(vertexAttribute);

		normalAttribute = glGetAttribLocationARB(program.programID, "normal");
		glEnableVertexAttribArrayARB(normalAttribute);

//		materialIndexAttrib = glGetAttribLocationARB(program.programID, "materialIndex");
//		glEnableVertexAttribArrayARB(materialIndexAttrib);

		modelMatrixUniform = glGetUniformLocationARB(program.programID, "modelMatrix");

		viewProjectionMatrixUniform = glGetUniformLocationARB(program.programID, "vpMatrix");
		normalMatrixUniform = glGetUniformLocationARB(program.programID, "normalMatrix");
		lightPositionUniform = glGetUniformLocationARB(program.programID, "uLightPosition");
		viewPositionUniform = glGetUniformLocationARB(program.programID, "uViewPosition");
		ambientColorUniform = glGetUniformLocationARB(program.programID, "uAmbientColor");
		diffuseColorUniform = glGetUniformLocationARB(program.programID, "uDiffuseColor");
		specularColorUniform = glGetUniformLocationARB(program.programID, "uSpecularColor");
		/*
				ShaderProgram.Mesh mesh = new ShaderProgram.Mesh();
		
				vertexAttribute.name = "aVertex";
				vertexAttribute.bufferType = BufferType.ArrayBuffer;
				vertexAttribute.dataType = GLDataType.Float;
				vertexAttribute.bufferUsage = BufferUsage.DynamicDraw;
				vertexAttribute.dataSize = 3;
				vertexAttribute.normalised = false;
				mesh.VBOs.add(vertexAttribute);
		
				normalAttribute.name = "aNormal";
				normalAttribute.bufferType = BufferType.ArrayBuffer;
				normalAttribute.dataType = GLDataType.Float;
				normalAttribute.bufferUsage = BufferUsage.DynamicDraw;
				normalAttribute.dataSize = 3;
				normalAttribute.normalised = false;
				mesh.VBOs.add(normalAttribute);
		
				modelMatrixAttribute.name = "modelMatrix";
				modelMatrixAttribute.bufferType = BufferType.ArrayBuffer;
				modelMatrixAttribute.dataType = GLDataType.Float;
				modelMatrixAttribute.bufferUsage = BufferUsage.DynamicDraw;
				modelMatrixAttribute.dataSize = 16;
				modelMatrixAttribute.normalised = false;
				mesh.VBOs.add(modelMatrixAttribute);*/
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

	void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		glUseProgramObjectARB(program.programID);
		for (Scene.Mesh mesh : scene.meshes) {

			glBindBufferARB(GL_ARRAY_BUFFER_ARB, mesh.vertexArrayBuffer);
			glVertexAttribPointerARB(vertexAttribute, 3, GL_FLOAT, false, 0, 0);
			glBindBufferARB(GL_ARRAY_BUFFER_ARB, mesh.normalArrayBuffer);
			glVertexAttribPointerARB(normalAttribute, 3, GL_FLOAT, false, 0, 0);

//			glBindBufferARB(GL_ARRAY_BUFFER_ARB, mesh.materialIndexBuffer);
//			glVertexAttribPointerARB(materialIndexAttrib, 1, GL_INT, false, 0, 0);
//			glBufferDataARB(GL_ARRAY_BUFFER_ARB, new int[] { mesh.materialIndexBuffer }, GL_STATIC_DRAW_ARB);

			glUniformMatrix4fvARB(modelMatrixUniform, false, modelMatrix.get(modelMatrixBuffer));

			glUniformMatrix4fvARB(viewProjectionMatrixUniform, false, viewProjectionMatrix.get(viewProjectionMatrixBuffer));
			normalMatrix.set(modelMatrix).invert().transpose();
			glUniformMatrix3fvARB(normalMatrixUniform, false, normalMatrix.get(normalMatrixBuffer));
			glUniform3fvARB(lightPositionUniform, lightPosition.get(lightPositionBuffer));
			glUniform3fvARB(viewPositionUniform, viewPosition.get(viewPositionBuffer));
			AIMesh m = AIMesh.create(mesh.mesh);
			Scene.Material material = scene.materials.get(m.mMaterialIndex());
			nglUniform3fvARB(ambientColorUniform, 1, material.mAmbientColor.address());
			nglUniform3fvARB(diffuseColorUniform, 1, material.mDiffuseColor.address());
			nglUniform3fvARB(specularColorUniform, 1, material.mSpecularColor.address());

			glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, mesh.elementArrayBuffer);
			glDrawElements(GL_TRIANGLES, mesh.elementCount, GL_UNSIGNED_INT, 0);
		}
	}

	private void storeMaterialBuffer(AIScene scene) {
		float[] materialDataBuffer = new float[scene.mNumMaterials() * 4 * 3];
		PointerBuffer mats = scene.mMaterials();
		for (int i = 0; i < scene.mNumMaterials(); i++) {
			AIMaterial material = AIMaterial.createSafe(mats.get(i));
			if (material != null) {
				createMaterial(material, materialDataBuffer, i);
			}
		}
	}

	public void createMaterial(AIMaterial AImaterial, float[] data, int index) {

		AIColor4D ambient = AIColor4D.create();
		if (aiGetMaterialColor(AImaterial, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE, 0, ambient) != 0) {
			logger.warn("no ambient found\r\t\n" + aiGetErrorString());
			ambient.set(0, 0, 0, 1);
		}
		AIColor4D diffuse = AIColor4D.create();
		if (aiGetMaterialColor(AImaterial, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, diffuse) != 0) {
			logger.warn("no diffuse found\r\t\n" + aiGetErrorString());
			ambient.set(0, 0, 0, 1);
		}
		AIColor4D specular = AIColor4D.create();
		if (aiGetMaterialColor(AImaterial, AI_MATKEY_COLOR_SPECULAR, aiTextureType_NONE, 0, specular) != 0) {
			logger.warn("no specular found\r\t\n" + aiGetErrorString());
			ambient.set(0, 0, 0, 1);
		}
		data[(index * 4) + 0] = ambient.r();
		data[(index * 4) + 1] = ambient.g();
		data[(index * 4) + 2] = ambient.b();
		data[(index * 4) + 3] = ambient.a();
		//put diffuse
		data[(index * 4) + 4] = diffuse.r();
		data[(index * 4) + 5] = diffuse.g();
		data[(index * 4) + 6] = diffuse.b();
		data[(index * 4) + 7] = diffuse.a();
		//put specular
		data[(index * 4) + 8] = specular.r();
		data[(index * 4) + 9] = specular.g();
		data[(index * 4) + 10] = specular.b();
		data[(index * 4) + 11] = specular.a();
	}

	static class Scene {

		public AIScene scene;
		public List<Mesh> meshes;
		public List<Material> materials;

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
			if (true) {
				int materialCount = scene.mNumMaterials();
				PointerBuffer materialsBuffer = scene.mMaterials();
				materials = new ArrayList<>();
				for (int i = 0; i < materialCount; ++i) {
					materials.add(new Material(AIMaterial.create(materialsBuffer.get(i))));
				}
			}
			if (false) {
				AINode root = scene.mRootNode();
				Set<Model> models;
				//				recursivelyReadNode(root, models, meshes, 0);
				//				logger.trace(meshes.size());
			}
		}

		/**
		 * Recursively reads all child nodes of given node
		 * @param node to read
		 * @param models set of models to popuplate
		 * @param meshes set of meshes to read from
		 * @param offset file offset.<br>
		 * each time a new AIScene is read the meshes are added and index goes up. but these dont point to the correct mesh<br>
		 * <ul>
		 * <li>Scene 1
		 * <ol>
		 * 	<li>Mesh 1</li>
		 * 	<li>Mesh 2</li>
		 * </ol></li>
		 * 
		 * <li>Scene 2
		 * <ol start="3">
		 * 	<li>Mesh 1</li>
		 * <li>Mesh 2</li>
		 * </ol>
		 * </li>
		 * </ul>
		 * 
		 */
		private void recursivelyReadNode(AINode node, Set<Model> models, List<Mesh> meshes, int offset) {

			PointerBuffer nodeBuffer = node.mChildren();
			for (int i = 0; i < node.mNumChildren(); i++) {
				AINode newNode = AINode.createSafe(nodeBuffer.get(i));
				int meshCount = newNode.mNumMeshes();
				if (meshCount == 0) {
					logger.trace("Node does not have any meshes. either a light or a camera. not reading. ");
				} else {
					Model model = new Model();

					AIMatrix4x4 mat = newNode.mTransformation();
					model.modelTranslation
						.set(
							mat.a1(),
							mat.b1(),
							mat.c1(),
							mat.d1(),
							mat.a2(),
							mat.b2(),
							mat.c2(),
							mat.d3(),
							mat.a3(),
							mat.b3(),
							mat.c3(),
							mat.d3(),
							mat.a4(),
							mat.b4(),
							mat.c4(),
							mat.d4());
					model.name = newNode.mName().dataString();

					IntBuffer meshBuffer = newNode.mMeshes();
					for (int j = 0; j < newNode.mNumMeshes(); j++) {
						int index = meshBuffer.get(j);
						//local to the scene, start from meshSize 
						Mesh mesh = meshes.get(offset + index);
						AIMesh aimesh = AIMesh.create(mesh.mesh);
						System.out.println("\t" + newNode.mName().dataString() + " uses " + aimesh.mName().dataString() + " mesh (mesh[" + index + "])");

					}

					models.add(model);
				}

				recursivelyReadNode(newNode, models, meshes, offset);

			}
		}

		public void free() {
			aiReleaseImport(scene);
			scene = null;
			meshes = null;
			materials = null;
		}

		public static class Model {
			/**key: Mesh index <br>
			 * value: material index<br>
			 */
			Set<Integer> meshMaterials = new CollectionSuppliers.SetSupplier<Integer>().get();
			Matrix4f modelTranslation = new Matrix4f();
			String name;

		}

		public static class Mesh {

			public long mesh;
			public int materialIndexBuffer;
			public int vertexArrayBuffer;
			public int normalArrayBuffer;
			public int elementArrayBuffer;
			public int elementCount;

			public Mesh(AIMesh mesh) {
				this.mesh = mesh.address();

				//TODO IMPLIMENT MATERIAL INDEX BUFFER

				materialIndexBuffer = glGenBuffersARB();
				glBindBufferARB(GL_ARRAY_BUFFER_ARB, materialIndexBuffer);
				int matIndex = mesh.mMaterialIndex();
				glBufferDataARB(GL_ARRAY_BUFFER_ARB, new int[] { matIndex }, GL_STATIC_DRAW_ARB);

				vertexArrayBuffer = glGenBuffersARB();
				glBindBufferARB(GL_ARRAY_BUFFER_ARB, vertexArrayBuffer);
				AIVector3D.Buffer vertices = mesh.mVertices();
				nglBufferDataARB(
					GL_ARRAY_BUFFER_ARB,
					AIVector3D.SIZEOF * vertices.remaining(),
					vertices.address(),
					GL_STATIC_DRAW_ARB);

				normalArrayBuffer = glGenBuffersARB();
				glBindBufferARB(GL_ARRAY_BUFFER_ARB, normalArrayBuffer);
				AIVector3D.Buffer normals = mesh.mNormals();
				nglBufferDataARB(
					GL_ARRAY_BUFFER_ARB,
					AIVector3D.SIZEOF * normals.remaining(),
					normals.address(),
					GL_STATIC_DRAW_ARB);

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
				elementArrayBuffer = glGenBuffersARB();
				glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, elementArrayBuffer);
				glBufferDataARB(
					GL_ELEMENT_ARRAY_BUFFER_ARB,
					elementArrayBufferData,
					GL_STATIC_DRAW_ARB);
			}
		}

		public static class Material {

			public AIMaterial mMaterial;
			public AIColor4D mAmbientColor;
			public AIColor4D mDiffuseColor;
			public AIColor4D mSpecularColor;

			public Material(AIMaterial material) {

				mMaterial = material;

				mAmbientColor = AIColor4D.create();
				if (aiGetMaterialColor(
					mMaterial,
					AI_MATKEY_COLOR_AMBIENT,
					aiTextureType_NONE,
					0,
					mAmbientColor) != 0) {
					throw new IllegalStateException(aiGetErrorString());
				}
				mDiffuseColor = AIColor4D.create();
				if (aiGetMaterialColor(
					mMaterial,
					AI_MATKEY_COLOR_DIFFUSE,
					aiTextureType_NONE,
					0,
					mDiffuseColor) != 0) {
					throw new IllegalStateException(aiGetErrorString());
				}
				mSpecularColor = AIColor4D.create();
				if (aiGetMaterialColor(
					mMaterial,
					AI_MATKEY_COLOR_SPECULAR,
					aiTextureType_NONE,
					0,
					mSpecularColor) != 0) {
					throw new IllegalStateException(aiGetErrorString());
				}
			}
		}
	}
}
