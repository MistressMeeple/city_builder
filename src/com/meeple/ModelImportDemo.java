package com.meeple;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.lang.ref.WeakReference;
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
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL46;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;

import com.meeple.citybuild.client.render.ShaderProgramDefinitions;
import com.meeple.citybuild.client.render.ShaderProgramDefinitions.ShaderProgramDefinition_3D_lit_mat;
import com.meeple.citybuild.client.render.ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat;
import com.meeple.citybuild.client.render.ShaderProgramDefinitions.ViewMatrices;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferDataManagementType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.RenderableVAO;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2.ShaderClosable;
import com.meeple.shared.frame.structs.Light;
import com.meeple.shared.frame.structs.Material;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;
import com.meeple.shared.utils.CollectionSuppliers;
import com.meeple.shared.utils.FrameUtils;

public class ModelImportDemo {

	private static Logger logger = Logger.getLogger(ModelImportDemo.class);

	private static String debugLayout = "[%r][%d{HH:mm:ss:SSS}][%t][%p] (%F:%L) %m%n";
	private static final int maxMaterials = 10;
	public static void main(String[] args) {

		Logger.getRootLogger().setLevel(org.apache.log4j.Level.ALL);
		Appender a = new ConsoleAppender(new PatternLayout(debugLayout));
		BasicConfigurator.configure(a);

		new ModelImportDemo().run();
	}

	// window properties
	long window;
	int width = 1024;
	int height = 768;
	int fbWidth = 1024;
	int fbHeight = 768;
	float fov = 60;

	class Model {
		Map<ShaderProgram.RenderableVAO, Integer> meshToMaterials = new CollectionSuppliers.MapSupplier<ShaderProgram.RenderableVAO, Integer>()
				.get();
		Matrix4f translation = new Matrix4f();
	}

	class MeshInstance {
		WeakReference<ShaderProgram.RenderableVAO> mesh;
		int meshDataIndex;
	}

	/**
	 * models used by program
	 */
	Model primaryModel = new Model();
	Model primaryLightModel = new Model();
	/**
	 * this holds the models mesh instance data
	 */
	Map<Model, Set<MeshInstance>> instances = new CollectionSuppliers.MapSupplier<Model, Set<MeshInstance>>().get();
	ViewMatrices viewMatrices = new ViewMatrices();

	Vector3f viewPosition = new Vector3f();
	Vector3f lightPosition = new Vector3f(5f, 0f, 1f);
	Light primaryLight = new Light();
	Light secondaryLight = new Light();

	GLFWKeyCallback keyCallback;
	GLFWFramebufferSizeCallback fbCallback;
	GLFWWindowSizeCallback wsCallback;

	GLFWScrollCallback sCallback;

	void convert(GLContext glContext, Map<String, ShaderProgramDefinition_3D_lit_mat.Mesh> meshes){
		
		{
			int i = 0;
			for (RenderableVAO a : meshes.values()) {
				int mIndex = 0;
				RenderableVAO dmesh = a;
				ShaderProgramSystem2.loadVAO(glContext, ShaderProgramDefinitions.collection._3D_lit_mat, dmesh);

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
				i++;
			}
		}
	}
	void run() {
		try (GLContext glContext = new GLContext()) {

			setupGLFW();
			/**
			 * bind the open gl context to this thread and window
			 */
			glContext.init();
			/**
			 * setup rendering information
			 */
			glClearColor(0f, 0f, 0f, 1f);
			glEnable(GL_DEPTH_TEST);

			/* Create all needed GL resources */
			ShaderProgramDefinitions.collection.create(glContext);
			ShaderProgram program = ShaderProgramDefinitions.collection._3D_lit_mat;
			initLights();
			initMaterials();

			Map<String, ShaderProgramDefinition_3D_lit_mat.Mesh> meshes = com.meeple.shared.ModelLoader.loadModelFromFile("resources/models/yert.fbx", 10, null);
			convert(glContext, meshes);

			/**
			 * setup the debug draw program
			 */
			ShaderProgram debugProgram = ShaderProgramDefinitions.collection._3D_unlit_flat;
			ShaderProgramDefinition_3D_unlit_flat.Mesh axis = drawAxis(100);
			ShaderProgramSystem2.loadVAO(glContext, debugProgram, axis);

			GL46.glLineWidth(3f);

			viewMatrices.projectionMatrix
					.setPerspective(
							(float) Math.toRadians(fov),
							(float) width / height,
							0.01f,
							100.0f);

			/**
			 * now everything is setup we can show the window and start rendering
			 */
			glfwShowWindow(window);

			Callback c = GLUtil.setupDebugMessageCallback();
			while (!glfwWindowShouldClose(window)) {
				glfwPollEvents();
				glViewport(0, 0, fbWidth, fbHeight);
				update();
				render(program);
				ShaderProgramSystem2.tryRender(debugProgram);

				glfwSwapBuffers(window);
			}

			c.free();
			keyCallback.free();
			fbCallback.free();
			wsCallback.free();
			glfwDestroyWindow(window);

			// ShaderProgramSystem2.close(program);

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			glfwTerminate();
		}
	}

	private void initLights() {
		primaryLight.position.set(10, 5, 0);
		primaryLight.attenuation.set(7, 1, 1f);
		primaryLight.colour.set(1, 1, 1);
		primaryLight.enabled = false;

		secondaryLight.position.set(0, 5, 10);
		secondaryLight.attenuation.set(7, 10, 5f);
		secondaryLight.colour.set(0, 1, 1);
		secondaryLight.enabled = false;
		ShaderProgramDefinitions.collection.updateLights(0, primaryLight, secondaryLight);
	}

	private void initMaterials() {
		Material m0 = new Material();
		m0.baseColour.set(1, 1, 1, 1);
		m0.reflectiveTint.set(1, 1, 0);
		m0.baseColourStrength = 0.75f;
		m0.reflectivityStrength = 0.1f;

		Material m1 = new Material();
		m1.baseColour.set(0.76f, 0.60f, 0.42f, 1);
		m1.baseColourStrength = 1f;
		m1.reflectiveTint.set(0.76f, 0.60f, 0.42f);
		m1.reflectivityStrength = 0f;

		Material m2 = new Material();
		m2.baseColour.set(0, 0, 1, 1);
		m2.reflectiveTint.set(0, 0, 1);

		ShaderProgramDefinitions.collection.updateMaterials(0, m0, m1, m2);
	}


	// ------these are for frame delta calculations
	Wrapper<Long> prev = new WrapperImpl<>(System.nanoTime());
	float total = 0;
	// current rotation of view position around 000
	float rotation = 0;

	/**
	 * per frame - pre render.
	 * handles the updating of view-projection matrices and light positioning
	 */
	void update() {
		// calculate delta between frames
		if (true) {

			long curr = System.nanoTime();
			long delta = curr - prev.getOrDefault(System.nanoTime());
			float deltaSeconds = FrameUtils.nanosToSeconds(delta);
			total += deltaSeconds;
			prev.set(curr);
		}

		// set camera rotation
		if (true) {
			rotation = total * 0.0125f * (float) Math.PI;
		}
		// projectionMatrix.rotate(axisAngle)
		// setting the view position defined by the rotation previously set and a radius
		viewPosition.set(15f * (float) Math.cos(rotation), 15f, 15f * (float) Math.sin(rotation));
		// setting the view matrix to look at 000 from view position
		viewMatrices.viewMatrix
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
		viewMatrices.viewMatrixUpdate.set(true);
		// handles the rotation of light source/model
		if (true) {
			float rotation2 = -total * 0.025f * (float) Math.PI;
			lightPosition.set(10f * (float) Math.sin(rotation2), 10f * (float) Math.cos(rotation2), 5f);
			// lightPosition.set(0, 5, 0);
			// lightPosition.set(5, 0, 0);
			primaryLightModel.translation.setTranslation(lightPosition.x, lightPosition.y, 5);
			primaryLight.position.set(lightPosition);
			primaryLight.enabled = true;
			//primaryLight.attenuation.y = primaryLight.attenuation.x * (1 * (float) (Math.sin(rotation2) + 1f) / 2f);

		}
	}

	/**
	 * Main rendering part, called each frame.
	 * only handles uploading/correcting data to buffers in OGL and rendering
	 * through shader program.
	 */
	void render(ShaderProgram program) {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glEnable(GL_DEPTH_TEST);

		ShaderProgramDefinitions.collection.writeVPFMatrix(viewMatrices);
		try (ShaderClosable sc = ShaderProgramSystem2.useProgram(program)) {

			for (MeshInstance meshInstance : instances.get(primaryModel)) {
				writeMeshTranslation(meshInstance, primaryModel.translation);
				writeMeshMaterialIndex(meshInstance, primaryModel.meshToMaterials.get(meshInstance.mesh.get()));
			}
			/*for (MeshInstance meshInstance : instances.get(primaryLightModel)) {
				writeMeshTranslation(meshInstance, primaryLightModel.translation);
				writeMeshMaterialIndex(meshInstance, primaryLightModel.meshToMaterials.get(meshInstance.mesh.get()));
			}*/
			ShaderProgramDefinitions.collection.updateLights(0, secondaryLight, primaryLight);
			// ShaderProgramDefinitions.collection._3D_lit_mat.setAmbientBrightness(0.0125f);
			ShaderProgramDefinitions.collection.updateAmbientBrightness(1.5125f);
		}

		ShaderProgramSystem2.tryRender(program);

	}

	/**
	 * writes the transformation to the sub buffer data of a mesh
	 * 
	 * @param instance    to write the data to
	 * @param translation translation of the mesh instance
	 */
	private void writeMeshTranslation(MeshInstance instance, Matrix4f translation) {
		try {
			// calculate normal matrix
			Matrix3f normal = new Matrix3f();
			normal.set(translation).invert().transpose();
			float[] data = new Matrix4f(normal).get(new float[16]);
			// upload translation and normal matrix
			writeBuffer(instance, ShaderProgramDefinitions.meshTransform_AttributeName, translation.get(new float[16]));
			writeBuffer(instance, ShaderProgramDefinitions.normalMatrix_AttributeName, data);
		} catch (Exception e) {
			logger.warn("failed to update", e);
		}
	}

	/**
	 * write the meshes material index to the buffer data of a mesh
	 * 
	 * @param instance      to write data to
	 * @param materialIndex new material index to use
	 */
	private void writeMeshMaterialIndex(MeshInstance instance, int materialIndex) {
		try {
			writeBuffer(instance, ShaderProgramDefinitions.materialIndex_AttributeName, new float[] { materialIndex });
		} catch (Exception e) {
			logger.warn("failed to update", e);
		}
	}

	/**
	 * helper function to write sub buffer data
	 * 
	 * @param instance provides access to attribute
	 * @param name     of attribute to use
	 * @param data     to upload
	 * @throws Exception if anything fails, eg null pointers
	 */
	private void writeBuffer(MeshInstance instance, String name, float[] data) throws Exception {

		Attribute attrib = instance.mesh.get().instanceAttributes.get(name).get();
		long offset = instance.meshDataIndex * (attrib.dataSize * attrib.dataType.getBytes());

		GL46.glBindBuffer(attrib.bufferType.getGLID(), attrib.VBOID);
		GL46.glBufferSubData(attrib.bufferType.getGLID(), offset, data);
		GL46.glBindBuffer(attrib.bufferType.getGLID(), 0);
	}


	private ShaderProgramDefinition_3D_unlit_flat.Mesh drawAxis(int size) {
		ShaderProgramDefinitions.ShaderProgramDefinition_3D_unlit_flat.Mesh mesh = ShaderProgramDefinitions.collection._3D_unlit_flat
				.createMesh(1);

		int count = 3;
		FloatBuffer verts = BufferUtils.createFloatBuffer(2 * 3 * count);
		FloatBuffer colours = BufferUtils.createFloatBuffer(2 * 4 * count);
		verts.put(new float[] { 0, 0, 0 });
		verts.put(new float[] { size, 0, 0 });
		colours.put(new float[] { 1, 0, 0, 1 });
		colours.put(new float[] { 1, 0, 0, 1 });

		verts.put(new float[] { 0, 0, 0 });
		verts.put(new float[] { 0, size, 0 });
		colours.put(new float[] { 0, 1, 0, 1 });
		colours.put(new float[] { 0, 1, 0, 1 });

		verts.put(new float[] { 0, 0, 0 });
		verts.put(new float[] { 0, 0, size });
		colours.put(new float[] { 0, 0, 1, 1 });
		colours.put(new float[] { 0, 0, 1, 1 });
		verts.flip();
		colours.flip();

		mesh.vertexAttribute.bufferResourceType = BufferDataManagementType.Buffer;
		mesh.vertexAttribute.buffer = verts;

		mesh.colourAttribute.bufferResourceType = BufferDataManagementType.Buffer;
		mesh.colourAttribute.buffer = colours;

		
		FrameUtils.appendToList(mesh.meshTransformAttribute.data, new Matrix4f().identity());
		mesh.meshTransformAttribute.bufferResourceType = BufferDataManagementType.List;
		

		mesh.vertexCount = count * 2;
		mesh.name = "axis";
		mesh.modelRenderType = GLDrawMode.Line;

		return mesh;
	}

	/**
	 * this is a generic setup for GLFW window creation
	 * 
	 * @throws IOException
	 */
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
				if (width > 0 && height > 0
						&& (ModelImportDemo.this.fbWidth != width || ModelImportDemo.this.fbHeight != height)) {
					ModelImportDemo.this.fbWidth = width;
					ModelImportDemo.this.fbHeight = height;
				}
			}
		});
		glfwSetWindowSizeCallback(window, wsCallback = new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (width > 0 && height > 0 && (ModelImportDemo.this.width != width || ModelImportDemo.this.height != height)) {
					ModelImportDemo.this.width = width;
					ModelImportDemo.this.height = height;
					viewMatrices.projectionMatrix
							.setPerspective(
									(float) Math.toRadians(fov),
									(float) width / height,
									0.01f,
									100.0f);
					viewMatrices.projectionMatrixUpdate.set(true);
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

					for (Entry<ShaderProgram.RenderableVAO, Integer> entry : primaryModel.meshToMaterials.entrySet()) {

						if (entry.getValue() == maxMaterials - 1) {
							entry.setValue(0);
						} else {
							entry.setValue(entry.getValue() + 1);
						}

						/*
						 * Attribute matIndex =
						 * entry.getKey().instanceAttributes.get(materialIndexName).get();
						 * matIndex.data.clear();
						 * matIndex.data.add(entry.getValue());
						 * matIndex.update.set(true);
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
				viewMatrices.projectionMatrix
						.setPerspective(
								(float) Math.toRadians(fov),
								(float) width / height,
								0.01f,
								100.0f);
				viewMatrices.projectionMatrixUpdate.set(true);
			}
		});
		
		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
		glfwMakeContextCurrent(window);
		// ENABLE V-SYNC, this dramatically reduces GPU CPU intensity
		glfwSwapInterval(1);
		glfwSetCursorPos(window, width / 2, height / 2);

		try (MemoryStack frame = MemoryStack.stackPush()) {
			IntBuffer framebufferSize = frame.mallocInt(2);
			nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
			width = framebufferSize.get(0);
			height = framebufferSize.get(1);
		}
	}

}