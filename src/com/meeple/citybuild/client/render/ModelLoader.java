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
import org.lwjgl.assimp.AIPropertyStore;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL46;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;

import com.meeple.citybuild.client.render.structs.Light;
import com.meeple.citybuild.client.render.structs.Material;
import com.meeple.citybuild.client.render.structs.Struct;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferDataManagementType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2.ShaderClosable;
import com.meeple.shared.frame.nuklear.IOUtil;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;

public class ModelLoader {

	private static Logger logger = Logger.getLogger(ModelLoader.class);

	private static String debugLayout = "[%r][%d{HH:mm:ss:SSS}][%t][%p] (%F:%L) %m%n";
	private static final int maxMaterials = 10;

	private static final int maxLights = 2;
	private static final int vpMatrixBindingpoint = 2;
	private static final int lightBufferBindingPoint = 3;
	private static final int materialBufferBindingPoint = 4;

	/**
	 * Names of the attributes, these are stored in the mesh instanced map with these values as keys
	 */
	private static final String transformMatName = "meshTransformMatrix", materialIndexName = "meshMaterialIndex", normalMatName = "meshNormalMatrix";

	public static void main(String[] args) {

		Logger.getRootLogger().setLevel(org.apache.log4j.Level.ALL);
		Appender a = new ConsoleAppender(new PatternLayout(debugLayout));
		BasicConfigurator.configure(a);

		new ModelLoader().run();
	}

	//window properties
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
	//	private int materialBuffer;
	private int ambientBrightnessLocation;

	/**
	 * class that represents a set of meshes paired with their material index, and a shared transformation
	 * @author Megan
	 *
	 */
	class Model {
		Map<ShaderProgram.Mesh, Integer> meshToMaterials = new CollectionSuppliers.MapSupplier<ShaderProgram.Mesh, Integer>().get();
		Matrix4f translation = new Matrix4f();
	}

	/**
	 * Holds the data about a mesh and which instance it refers to in the buffer 
	 * @author Megan
	 *
	 */
	class MeshInstance {
		WeakReference<ShaderProgram.Mesh> mesh;
		int meshDataIndex;
	}

	Map<String, Mesh> meshes = new CollectionSuppliers.MapSupplier<String, Mesh>().get();
	/**
	 * models used by program
	 */
	Model primaryModel = new Model();
	Model primaryLightModel = new Model();
	Model[][] models = new Model[1][1];
	/**
	 * this holds the models mesh instance data
	 */
	Map<Model, Set<MeshInstance>> instances = new CollectionSuppliers.MapSupplier<Model, Set<MeshInstance>>().get();

	Matrix4f viewMatrix = new Matrix4f();
	Matrix4f projectionMatrix = new Matrix4f();
	Matrix4f viewProjectionMatrix = new Matrix4f();
	Vector3f viewPosition = new Vector3f();
	Vector3f lightPosition = new Vector3f(5f, 0f, 1f);
	Light primaryLight = new Light();
	Light secondaryLight = new Light();

	GLFWKeyCallback keyCallback;
	GLFWFramebufferSizeCallback fbCallback;
	GLFWWindowSizeCallback wsCallback;

	GLFWScrollCallback sCallback;

	void run() {
		try (GLContext glContext = new GLContext()) {
			setupGLFW();
			glContext.init();
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

			glClearColor(0f, 0f, 0f, 1f);
			glEnable(GL_DEPTH_TEST);

			/* Create all needed GL resources */
			createProgram(glContext);
			/*read/setup the scene meshes*/
			loadModel(
				glContext,
				"resources/models/tipi.fbx",
				meshes);

			/* Show window */
			glfwShowWindow(window);

			Callback c = GLUtil.setupDebugMessageCallback();
			while (!glfwWindowShouldClose(window)) {
				glfwPollEvents();
				glViewport(0, 0, fbWidth, fbHeight);
				update();
				render();
				glfwSwapBuffers(window);
			}

			c.free();
			keyCallback.free();
			fbCallback.free();
			wsCallback.free();
			glfwDestroyWindow(window);

			//			ShaderProgramSystem2.close(program);

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			glfwTerminate();
		}
	}

	void setupGLFW() throws IOException {

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

					for (Entry<ShaderProgram.Mesh, Integer> entry : primaryModel.meshToMaterials.entrySet()) {

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
		//ENABLE V-SYNC, this dramatically reduces GPU CPU intensity 
		glfwSwapInterval(1);
		glfwSetCursorPos(window, width / 2, height / 2);

		try (MemoryStack frame = MemoryStack.stackPush()) {
			IntBuffer framebufferSize = frame.mallocInt(2);
			nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
			width = framebufferSize.get(0);
			height = framebufferSize.get(1);
		}
	}

	/**
	 * loads an AIScene from memory and converts all meshes found into our format
	 * @throws IOException
	 */
	void loadModel(GLContext glc, String fileLocation, Map<String, Mesh> meshes) throws IOException {

		//read the resource into a buffer, and load the scene from the buffer
		ByteBuffer fileContent = IOUtil.ioResourceToByteBuffer(fileLocation, 2048 * 8);
		AIPropertyStore store = aiCreatePropertyStore();
		aiSetImportPropertyInteger(store, AI_CONFIG_PP_SBP_REMOVE, Assimp.aiPrimitiveType_LINE | Assimp.aiPrimitiveType_POINT);
		//		aiSetImportPropertyInteger(store,  AI_CONFIG_PP_FD_CHECKAREA , 0);
		AIScene scene =
			aiImportFileFromMemoryWithProperties(
				fileContent,
				0 |
					//				aiProcess_JoinIdenticalVertices | 
					aiProcess_Triangulate
					//				aiProcessPreset_TargetRealtime_MaxQuality | 
					//					| aiProcess_FindDegenerates 
					| aiProcess_GenNormals | aiProcess_FixInfacingNormals | aiProcess_GenSmoothNormals
					//  aiProcess_MakeLeftHanded |
					//  aiProcess_ImproveCacheLocality |
					//					| aiProcess_findi
					| aiProcess_JoinIdenticalVertices | aiProcess_SortByPType,
				(ByteBuffer) null,
				store);
		if (scene == null) {
			throw new IllegalStateException(aiGetErrorString());
		}

		//reads all the mesh data from the scene into our formatting
		if (true) {
			int meshCount = scene.mNumMeshes();
			PointerBuffer meshesBuffer = scene.mMeshes();
			for (int i = 0; i < meshCount; ++i) {
				AIMesh mesh = AIMesh.create(meshesBuffer.get(i));

				logger.trace("Mesh with name: " + mesh.mName().dataString() + " just been imported");

				int mIndex = 0;
				ShaderProgram.Mesh dmesh = setupMesh(mesh, 2 + (models.length * models[0].length));
				/*
								long vertAddress = mesh.mVertices().address();
								long normAddress = mesh.mNormals().address();
				
								{
				
									int elementCount;
									int faceCount = mesh.mNumFaces();
									elementCount = faceCount * 3;
									IntBuffer elementArrayBufferData = BufferUtils.createIntBuffer(elementCount);
									AIFace.Buffer facesBuffer = mesh.mFaces();
									for (int j = 0; j < faceCount; ++j) {
										AIFace face = facesBuffer.get(j);
										if (face.mNumIndices() != 3) {
				
										} else {
											elementArrayBufferData.put(face.mIndices());
										}
									}
								}*/
				meshes.put(mesh.mName().dataString(), dmesh);
				ShaderProgramSystem2.loadVAO(glc, program, dmesh);

				primaryModel.meshToMaterials.put(dmesh, mIndex + i);
				primaryLightModel.meshToMaterials.put(dmesh, 0);
				dmesh.renderCount = 2;

				int index = 0;
				{
					MeshInstance mi = new MeshInstance();
					mi.mesh = new WeakReference<>(dmesh);
					mi.meshDataIndex = index++;
					FrameUtils.addToSetMap(instances, primaryModel, mi, new CollectionSuppliers.SetSupplier<>());
				}
				{
					MeshInstance mi2 = new MeshInstance();
					mi2.mesh = new WeakReference<>(dmesh);
					mi2.meshDataIndex = index++;
					FrameUtils.addToSetMap(instances, primaryLightModel, mi2, new CollectionSuppliers.SetSupplier<>());
				}
				mesh.clear();
				mesh.free();

				if (false) {

					for (int x = 0; x < models.length; x++) {
						for (int y = 0; y < models[x].length; y++) {
							models[x][y] = new Model();
							models[x][y].meshToMaterials.put(dmesh, 2);
							dmesh.renderCount += 1;

							MeshInstance mi = new MeshInstance();
							mi.mesh = new WeakReference<>(dmesh);
							mi.meshDataIndex = index++;
							FrameUtils.addToSetMap(instances, models[x][y], mi, new CollectionSuppliers.SetSupplier<>());
							models[x][y].translation.setTranslation(x * 2, 0, y * 2);
						}
					}
				}
			}
		}
		aiReleaseImport(scene);
		logger.trace("foo");
	}

	private int storeMatrixBuffer(Matrix4f view, Matrix4f projection, Matrix4f vpMult, int bindingPoint) {
		int matrixBuffer = genUBO(bindingPoint, 64 * 3);
		logger.trace("mat buffer: " + matrixBuffer);
		float[] store = new float[16];
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 0, view.get(store));
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64, projection.get(store));
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 128, vpMult.get(store));
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);

		//binds the buffer to a binding index
		glBindBufferBase(GL_UNIFORM_BUFFER, bindingPoint, matrixBuffer);

		return matrixBuffer;

	}

	private int genUBO(int bindingPoint, long maxSize) {
		int buffer = GL46.glGenBuffers();
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
		glBufferData(
			GL_UNIFORM_BUFFER,
			maxSize,
			GL_DYNAMIC_DRAW);

		//binds the buffer to a binding index
		glBindBufferBase(GL_UNIFORM_BUFFER, bindingPoint, buffer);
		return buffer;
	}

	private void bindUBONameToIndex(String name, int binding, ShaderProgram... programs) {
		for (ShaderProgram program : programs) {
			int actualIndex = GL46.glGetUniformBlockIndex(program.programID, name);
			//binds the binding index to the interface block (by index)
			glUniformBlockBinding(program.programID, actualIndex, binding);
		}
	}

	void createProgram(GLContext glc) throws IOException {
		//create new shader program
		program = new ShaderProgram();
		//generate shader program sources, replacing "max" values
		//max lights/max materials
		String fragSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/lighting.frag"));
		fragSource = fragSource.replaceAll("\\{maxmats\\}", "" + maxMaterials);
		fragSource = fragSource.replaceAll("\\{maxlights\\}", maxLights + "");
		String vertSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/lighting.vert"));
		vertSource = vertSource.replaceAll("\\{maxlights\\}", maxLights + "");
		program.shaderSources.put(GLShaderType.VertexShader, vertSource);
		program.shaderSources.put(GLShaderType.FragmentShader, fragSource);

		//setup the program
		try {
			ShaderProgramSystem2.create(glc, program);
		} catch (Exception err) {
			err.printStackTrace();
		}

		glUseProgram(program.programID);
		ambientBrightnessLocation = GL46.glGetUniformLocation(program.programID, "ambientBrightness");
		//-----binding to the view/projection uniform buffer/block-----//
		if (true) {
			this.matrixBuffer = storeMatrixBuffer(viewMatrix, projectionMatrix, viewProjectionMatrix, vpMatrixBindingpoint);
			bindUBONameToIndex("Matrices", vpMatrixBindingpoint, program);
		}
		//-----binding to the light uniform buffer/block-----//
		if (true) {

			primaryLight.position.set(10, 5, 0);
			primaryLight.attenuation.set(7, 1, 1f);
			primaryLight.colour.set(1, 1, 1);
			primaryLight.enabled = false;

			secondaryLight.position.set(0, 5, 0);
			secondaryLight.attenuation.set(7, 10, 1f);
			secondaryLight.colour.set(1, 1, 1);
			secondaryLight.enabled = true;
		}
		if (true) {
			lightBuffer = genUBO(lightBufferBindingPoint, Light.sizeOf * ShaderProgram.GLDataType.Float.getBytes() * maxLights);

			bindUBONameToIndex("LightBlock", lightBufferBindingPoint, program);

		}
		//-----binding to the material uniform buffer/block-----//
		if (true) {

			//i really should delete this afterwards
			int materialBuffer = genUBO(materialBufferBindingPoint, Material.sizeOf * ShaderProgram.GLDataType.Float.getBytes() * maxMaterials);
			//upload material data
			int i = 0;
			float[] data = new float[Material.sizeOf];

			{
				Material m = new Material();
				m.baseColour.set(1, 1, 1, 1);
				m.reflectiveTint.set(1, 1, 0);
				m.baseColourStrength = 0.75f;
				m.reflectivityStrength = 0.1f;
				data = m.toArray(data, 0);
				glBufferSubData(GL46.GL_UNIFORM_BUFFER, 0, data);
				i++;
			}
			{
				Material m = new Material();
				m.baseColour.set(0.76f, 0.60f, 0.42f, 1);
				m.baseColourStrength = 1f;
				m.reflectiveTint.set(0.76f, 0.60f, 0.42f);
				m.reflectivityStrength = 0f;
				m.toArray(data, 0);
				glBufferSubData(GL46.GL_UNIFORM_BUFFER, Material.dataOffset(Material.sizeOf, i++) * ShaderProgram.GLDataType.Float.getBytes(), data);
			}
			{
				Material m = new Material();
				m.baseColour.set(0, 0, 1, 1);
				m.reflectiveTint.set(0, 0, 1);
				glBufferSubData(GL46.GL_UNIFORM_BUFFER, Material.dataOffset(Material.sizeOf, i++) * ShaderProgram.GLDataType.Float.getBytes(), data);
			}
			bindUBONameToIndex("MaterialBlock", materialBufferBindingPoint, program);
			glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);

		}

	}

	//------these are for frame delta calculations
	Wrapper<Long> prev = new WrapperImpl<>(System.nanoTime());
	float total = 0;
	//current rotation of view position around 000
	float rotation = 0;

	/**
	 * per frame - pre render. 
	 * handles the updating of view-projection matrices and light positioning
	 */
	void update() {
		//calculate delta between frames
		if (true) {

			long curr = System.nanoTime();
			long delta = curr - prev.getWrappedOrDefault(System.nanoTime());
			float deltaSeconds = FrameUtils.nanosToSeconds(delta);
			total += deltaSeconds;
			prev.setWrapped(curr);
		}

		//set camera rotation
		if (true) {
			rotation = total * 0.125f * (float) Math.PI;
		}
		//update projection matrix, not needed per frame but easier to have. 
		projectionMatrix
			.setPerspective(
				(float) Math.toRadians(fov),
				(float) width / height,
				0.01f,
				100.0f);

		//setting the view position defined by the rotation previously set and a radius
		viewPosition.set(15f * (float) Math.cos(rotation), 15f, 15f * (float) Math.sin(rotation));
		//setting the view matrix to look at 000 from view position
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
		//update VP matrix
		projectionMatrix.mul(viewMatrix, viewProjectionMatrix);

		//handles the rotation of light source/model
		if (true) {
			float rotation2 = -total * 0.25f * (float) Math.PI;
			lightPosition.set(10f * (float) Math.sin(rotation2), 5f, 10f * (float) Math.cos(rotation2));
			//			lightPosition.set(0, 5, 0);
			//		lightPosition.set(5, 0, 0);
			primaryLightModel.translation.setTranslation(lightPosition.x, 1, lightPosition.z);
			primaryLight.position.set(lightPosition);
			primaryLight.enabled = true;
			primaryLight.attenuation.y = primaryLight.attenuation.x * (1 * (float) (Math.sin(rotation2) + 1f) / 2f);

		}
		if (true) {
			//TODO
			/*for (int i = 0; i < models.length; i++) {
			
				float a = (-total + (i * 0.8f)) * 0.25f;
				float b = a * (float) Math.PI;
				Vector3f lpos = new Vector3f(
					10f * (float) Math.sin(b),
					i * 0.05f,
					2 * (float) Math.cos(b));
				models[i].translation.setTranslation(lpos);
			}*/
		}
		//		light.translation.setTranslation(10, 0, 0);
		//		model.translation.translate(0, deltaSeconds, 0);
	}

	/**
	 * Main rendering part, called each frame. 
	 * only handles uploading/correcting data to buffers in OGL and rendering through shader program.
	 */
	void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_DEPTH_TEST);

		writeVPMatrix(matrixBuffer, viewMatrix, viewProjectionMatrix);
		try (ShaderClosable sc = ShaderProgramSystem2.useProgram(program)) {

			for (MeshInstance meshInstance : instances.get(primaryModel)) {
				writeMeshTranslation(meshInstance, primaryModel.translation);
				writeMeshMaterialIndex(meshInstance, primaryModel.meshToMaterials.get(meshInstance.mesh.get()));
			}
			for (MeshInstance meshInstance : instances.get(primaryLightModel)) {
				writeMeshTranslation(meshInstance, primaryLightModel.translation);
				writeMeshMaterialIndex(meshInstance, primaryLightModel.meshToMaterials.get(meshInstance.mesh.get()));
			}

			if (false) {

				for (int x = 0; x < models.length; x++) {
					for (int y = 0; y < models[x].length; y++) {

						for (MeshInstance meshInstance : instances.get(models[x][y])) {
							writeMeshTranslation(meshInstance, models[x][y].translation);
							writeMeshMaterialIndex(meshInstance, models[x][y].meshToMaterials.get(meshInstance.mesh.get()));
						}
					}
				}
			}

			/*
				glUniform3fv(lightPositionUniform, lightPosition.get(lightPositionBuffer));
				glUniform3fv(lightColourUniform, new float[] { 1, 0, 0 });
				glUniform3fv(lightStrengthUniform, new float[] { 1, 0.01f, 0.005f });
			*/

			writeLight(lightBuffer, 1, primaryLight);
			writeLight(lightBuffer, 0, secondaryLight);
			glUniform1f(ambientBrightnessLocation, 0.0125f);
		}

		ShaderProgramSystem2.tryRender(program);

	}

	/**
	 * writes the transformation to the sub buffer data of a mesh
	 * @param instance to write the data to
	 * @param translation translation of the mesh instance
	 */
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

	/**
	 * write the meshes material index to the buffer data of a mesh
	 * @param instance to write data to
	 * @param materialIndex new material index to use
	 */
	private void writeMeshMaterialIndex(MeshInstance instance, int materialIndex) {
		try {
			writeBuffer(instance, materialIndexName, new float[] { materialIndex });
		} catch (Exception e) {
			logger.warn("failed to update", e);
		}
	}

	/**
	 * helper function to write sub buffer data
	 * @param instance provides access to attribute
	 * @param name of attribute to use
	 * @param data to upload
	 * @throws Exception if anything fails, eg null pointers
	 */
	private void writeBuffer(MeshInstance instance, String name, float[] data) throws Exception {

		Attribute attrib = instance.mesh.get().instanceAttributes.get(name).get();
		long offset = instance.meshDataIndex * (attrib.dataSize * attrib.dataType.getBytes());

		GL46.glBindBuffer(attrib.bufferType.getGLID(), attrib.VBOID);
		GL46.glBufferSubData(attrib.bufferType.getGLID(), offset, data);
		GL46.glBindBuffer(attrib.bufferType.getGLID(), 0);
	}

	/**
	 * update the VP matrix to the GPU buffers
	 * @param buffer named location to upload to
	 * @param view view matrix
	 * @param viewProjection VP matrix
	 */
	private void writeVPMatrix(int buffer, Matrix4f view, Matrix4f viewProjection) {

		//no need to be in a program bidning, since this is shared between multiple programs
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
		float[] store = new float[16];
		//		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 0, projectionMatrix.get(store));
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 0, view.get(store));
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, 128, viewProjection.get(store));
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
	}

	/**
	 * upload a light to the buffer object
	 * @param buffer named location of buffer to upload to 
	 * @param index index of the light to upload
	 * @param light actual data to upload
	 */
	private void writeLight(int buffer, int index, Light light) {
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
		float[] store = new float[Light.sizeOf];
		light.toArray(store, 0);
		int offset = Struct.dataOffset(Light.sizeOf, index) * ShaderProgram.GLDataType.Float.getBytes();
		glBufferSubData(GL46.GL_UNIFORM_BUFFER, offset, store);
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);

	}

	private IntBuffer convertElementBuffer(AIFace.Buffer facesBuffer, int faceCount, int elementCount) {
		IntBuffer elementArrayBufferData = BufferUtils.createIntBuffer(elementCount);
		for (int i = 0; i < faceCount; ++i) {
			AIFace face = facesBuffer.get(i);
			if (face.mNumIndices() != 3) {
				logger.trace("not 3 verts in a face. actually had " + face.mNumIndices());
			} else {
				elementArrayBufferData.put(face.mIndices());
			}
		}
		elementArrayBufferData.flip();
		return elementArrayBufferData;
	}

	/**
	 * sets up the mesh with attributes/VBOs and uses the AIMesh data provided  
	 * @param aim mesh data to read from
	 * @param maxMeshes maximum instances of the mesh
	 * @return Mesh to be rendered with shader program
	 */
	private ShaderProgram.Mesh setupMesh(AIMesh aim, long maxMeshes) {
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

			aim.mVertices().free();
			vertices.clear();
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
			//			elementAttrib.dataSize = 3;
			mesh.VBOs.add(elementAttrib);
			AIFace.Buffer facesBuffer = aim.mFaces();
			int faceCount = aim.mNumFaces();
			int elementCount = faceCount * 3;
			elementAttrib.bufferResourceType = BufferDataManagementType.Buffer;

			IntBuffer elementArrayBufferData = convertElementBuffer(facesBuffer, faceCount, elementCount);
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
