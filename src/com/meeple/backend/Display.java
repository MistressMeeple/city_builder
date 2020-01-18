package com.meeple.backend;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.ParseException;
import java.util.Random;
import java.util.TreeMap;

import javax.swing.SwingUtilities;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkColorf;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkPluginFilter;
import org.lwjgl.nuklear.NkPluginFilterI;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferDataManagementType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgramSystem;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;
import com.meeple.shared.frame.component.FrameTimeManager;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.window.MirroredWindowCallbacks;
import com.meeple.temp.Island;
import com.meeple.temp.Island.IslandSize;

public class Display {

	public static void main(String[] args) {
		SwingUtilities
			.invokeLater(
				() -> {

					Display d = new Display();
					d.glInit(1400, 800, "title");
					/*	d.callbacks.keyCallbackSet.add((window, key, scancode, action, mods) -> {
					
							if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
								glfwSetWindowShouldClose(window, true);
					
						});*/

					d.glRun();
					// Free the window callbacks and destroy the window
					glfwFreeCallbacks(d.window);
					glfwDestroyWindow(d.window);

					// Terminate GLFW and free the error callback
					glfwTerminate();
					glfwSetErrorCallback(null).free();
				});
	}

	long window;
	NkContextSingleton ctx = new NkContextSingleton();
	GLCapabilities glCaps;
	MirroredWindowCallbacks callbacks = new MirroredWindowCallbacks();

	public void glInit(int width, int height, String title) {

		GLFWErrorCallback.createPrint(System.err).set();

		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		window = glfwCreateWindow(width, height, title, NULL, NULL);
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");
		callbacks.bindToWindow(window);
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1);
			IntBuffer pHeight = stack.mallocInt(1);

			glfwGetWindowSize(window, pWidth, pHeight);

			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			glfwSetWindowPos(
				window,
				(vidmode.width() - pWidth.get(0)) / 2,
				(vidmode.height() - pHeight.get(0)) / 2);
		}

		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);

		glfwShowWindow(window);
		glCaps = GL.createCapabilities();
		ctx.setup(window, callbacks);

	}

	private int windowWidth,
		windowHeight;

	private int fbWidth,
		fbHeight;

	Random random = new Random();
	TreeMap<Float, IslandSize> sizeMap = new TreeMap<>();
	float sizeMapMax = 0;

	ShaderProgram program;
	Float[] vertices = {
		-0.5f, -0.5f, 0f,
		0.5f, -0.5f, 0f,
		0.5f, 0.5f, 0f,
		-0.5f, 0.5f, 0f
	};

	public void glRun() {

		GL.setCapabilities(glCaps);
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		FrameTimeManager time = new FrameTimeManager();
		time.desiredFrameRate = 130;
		System.out.println("setup for programs happens here");

		program = createProgram();
		setupUBOs(program);

		mesh = new Mesh()
			.drawMode(GLDrawMode.LineLoop)
			.vertexCount(vertices.length / 3)
			.addAttribute(
				new Attribute()
					.name("vertex")
					.dataType(GLDataType.Float)
					.dataSize(3)
					.bufferType(BufferType.ArrayBuffer)
					.bufferUsage(BufferUsage.StaticDraw)
					.data(vertices))
			.addAttribute(
				new Attribute()
					.name("colour")
					.dataType(GLDataType.Float)
					.dataSize(4)
					.bufferType(BufferType.ArrayBuffer)
					.bufferUsage(BufferUsage.StaticDraw)
					.data(new Float[] { 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f }));
		ShaderProgramSystem.loadVAO(program, mesh);

		Mesh axisMesh = drawAxis(100);
		ShaderProgramSystem.loadVAO(program, axisMesh);

		float total = 0f;
		long prev = System.nanoTime();
		long delta;

		float deltaSeconds;
		float fps;

		sizeMap.put(sizeMapMax += 1, IslandSize.TINY);
		sizeMap.put(sizeMapMax += 3, IslandSize.SMALL);
		sizeMap.put(sizeMapMax += 5, IslandSize.MEDIUM);
		sizeMap.put(sizeMapMax += 2, IslandSize.BIG);
		/*
				float value = random.nextFloat() * sizeMapMax;
				IslandSize size = sizeMap.higherEntry(value).getValue();
				generateIsland(new Random(random.nextLong()), size);*/

		GL46.glEnable(GL46.GL_DEPTH_TEST);
		while (!glfwWindowShouldClose(window)) {
			//NOTE delta calculation per frame
			{
				long curr = System.nanoTime();
				delta = curr - prev;
				deltaSeconds = FrameUtils.nanosToSeconds(delta);
				fps = 1f / deltaSeconds;
				prev = curr;
				total += deltaSeconds;
			}
			//NOTE get the latest accurate window/frame buffer sizes 
			try (MemoryStack stack = stackPush()) {
				IntBuffer w = stack.mallocInt(1);
				IntBuffer h = stack.mallocInt(1);

				glfwGetWindowSize(window, w, h);
				windowWidth = w.get(0);
				windowHeight = h.get(0);

				glfwGetFramebufferSize(window, w, h);
				fbWidth = w.get(0);
				fbHeight = h.get(0);
			}

			//NOTE handle the input updates
			ctx.handleInput(window);
			callbacks.tick(delta);

			//NOTE pre-render
			modelTick(total);
			layout(ctx.context, 300, 300);
			glClearColor(background.r(), background.g(), background.b(), background.a());

			//NOTE this denotes that GL is using a new frame. 
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			//NOTE actual render
			GL46.glPointSize(4f);
			ShaderProgramSystem2.tryRender(program);

			//NOTE UI render always last
			ctx.render(fbWidth, fbHeight, windowWidth, windowHeight);
			glfwSwapBuffers(window);
			time.run();

		}
	}

	float rotation;
	Matrix4f viewMatrix = new Matrix4f();
	Matrix4f projectionMatrix = new Matrix4f();
	Matrix4f viewProjectionMatrix = new Matrix4f();
	Vector3f viewPosition = new Vector3f();
	float fov = 60;

	private void modelTick(float total) {
		rotation = total * 0.125f * (float) Math.PI;
		projectionMatrix
			.setPerspective(
				(float) Math.toRadians(fov),
				(float) windowWidth / windowHeight,
				0.01f,
				100.0f);

		//setting the view position defined by the rotation previously set and a radius
		viewPosition.set(5f * (float) Math.cos(rotation), 5f * (float) Math.sin(rotation), 5f);
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
				0f,
				1f);
		//update VP matrix
		projectionMatrix.mul(viewMatrix, viewProjectionMatrix);
		writeVPFMatrix(matrixBuffer, projectionMatrix, viewMatrix, viewProjectionMatrix);
	}

	private static final int EASY = 0;
	private static final int HARD = 1;

	NkColorf background = NkColorf
		.create()
		.r(0.10f)
		.g(0.18f)
		.b(0.24f)
		.a(1.0f);

	private int op = EASY;

	private IntBuffer compression = BufferUtils.createIntBuffer(1).put(0, 20);
	Mesh mesh;

	long currentSeed = 0;
	IslandSize currentSize = IslandSize.MEDIUM;
	int[] currentLow = { 6 }, currentHigh = { 10 };
	TreeMap<Float, Vector4f> map = new TreeMap<>();
	NkPluginFilterI numberFilter = NkPluginFilter.create(Nuklear::nnk_filter_float);

	void layout(NkContext ctx, int x, int y) {
		try (MemoryStack stack = stackPush()) {
			NkRect rect = NkRect.mallocStack(stack);

			if (nk_begin(
				ctx,
				"Island Gen",
				nk_rect(x, y, 300, 350, rect),
				NK_WINDOW_BORDER | NK_WINDOW_MOVABLE | NK_WINDOW_SCALABLE | NK_WINDOW_MINIMIZABLE | NK_WINDOW_TITLE)) {
				nk_layout_row_static(ctx, 30, 80, 1);

				nk_layout_row(ctx, Nuklear.NK_DYNAMIC, 30, new float[] { 0.2f, 0.8f });
				nk_label(ctx, "Seed:", NK_TEXT_LEFT);
				ByteBuffer buffer = stack.calloc(256);
				int length = memASCII(currentSeed + "", false, buffer);
				IntBuffer len = stack.ints(length);
				nk_edit_string(ctx, NK_EDIT_SIMPLE, buffer, len, 255, numberFilter);
				try {
					currentSeed = Long.parseLong(memASCII(buffer, len.get(0)));
				} catch (Exception e) {

				}

				nk_layout_row_dynamic(ctx, 25, IslandSize.values().length);
				for (IslandSize size : IslandSize.values()) {

					if (nk_option_label(ctx, size.name(), currentSize == size)) {
						currentSize = size;
					}
				}
				//				nk_property_int(ctx, "Compression:", 0, compression, 100, 10, 1);

				nk_layout_row_dynamic(ctx, 25, 2);
				nk_property_int(ctx, "min", 0, currentLow, 10, 1, 0.5f);
				nk_property_int(ctx, "max", currentLow[0] + 1, currentHigh, currentLow[0] + 10, 1, 0.5f);
				nk_layout_row_dynamic(ctx, 20, 1);
				nk_label(ctx, "background:", NK_TEXT_LEFT);
				nk_layout_row_dynamic(ctx, 25, 1);
				if (nk_combo_begin_color(ctx, nk_rgb_cf(background, NkColor.mallocStack(stack)), NkVec2.mallocStack(stack).set(nk_widget_width(ctx), 400))) {
					nk_layout_row_dynamic(ctx, 120, 1);
					nk_color_picker(ctx, background, NK_RGBA);
					nk_layout_row_dynamic(ctx, 25, 1);
					background
						.r(nk_propertyf(ctx, "#R:", 0, background.r(), 1.0f, 0.01f, 0.005f))
						.g(nk_propertyf(ctx, "#G:", 0, background.g(), 1.0f, 0.01f, 0.005f))
						.b(nk_propertyf(ctx, "#B:", 0, background.b(), 1.0f, 0.01f, 0.005f))
						.a(nk_propertyf(ctx, "#A:", 0, background.a(), 1.0f, 0.01f, 0.005f));
					nk_combo_end(ctx);
				}

				nk_layout_row_dynamic(ctx, 30, 2);
				if (nk_button_label(ctx, "generate")) {
					/*
										float value = new Random(currentSeed).nextFloat() * sizeMapMax;
										IslandSize size = sizeMap.higherEntry(value).getValue();*/
					generateIsland(currentSeed, currentSize, currentLow[0], currentHigh[0]);
				}

				if (nk_button_label(ctx, "random")) {

					currentSeed = random.nextLong();
					float value = new Random(currentSeed).nextFloat() * sizeMapMax;
					IslandSize size = sizeMap.higherEntry(value).getValue();
					currentSize = size;
					currentLow[0] = 6;
					currentHigh[0] = 10;
					generateIsland(currentSeed, size, 6, 10);
				}

			}
			nk_end(ctx);
		}
	}

	private void generateIsland(long seed, IslandSize size, int lower, int higher) {

		System.out.println("starting with seed " + seed);
		Island i = new Island(seed, "test", size);

		i.generate(lower, higher);

		int msize = i.map.size();
		FloatBuffer vertices = BufferUtils.createFloatBuffer(msize * 3);
		FloatBuffer colours = BufferUtils.createFloatBuffer(msize * 4);
		msize = i.convertToMesh(vertices, colours, size.radius / 2, 1);
		vertices.flip();
		colours.flip();
		program.VAOs.remove(mesh);
		ShaderProgramSystem.deleteMesh(mesh);

		/*	mesh = new Mesh()
				.drawMode(GLDrawMode.Points)
				.vertexCount(msize)
		
				.addAttribute(
					new Attribute()
						.name("vertex")
						.dataType(GLDataType.Float)
						.dataSize(3)
						.bufferType(BufferType.ArrayBuffer)
						.bufferUsage(BufferUsage.StaticDraw)
						.data(vertices))
				.addAttribute(
					new Attribute()
						.name("colour")
						.dataType(GLDataType.Float)
						.dataSize(4)
						.bufferType(BufferType.ArrayBuffer)
						.bufferUsage(BufferUsage.StaticDraw)
						.data(colours));*/
		mesh = new Mesh()
			.drawMode(GLDrawMode.Points)
			.vertexCount(msize)
			.addAttribute(
				new Attribute()
					.name("vertex")
					.dataType(GLDataType.Float)
					.dataSize(3)
					.bufferType(BufferType.ArrayBuffer)
					.bufferUsage(BufferUsage.StaticDraw)
					.data(vertices))
			.addAttribute(
				new Attribute()
					.name("colour")
					.dataType(GLDataType.Float)
					.dataSize(4)
					.bufferType(BufferType.ArrayBuffer)
					.bufferUsage(BufferUsage.StaticDraw)
					.data(
						colours));
		mesh.name = "mesh";

		ShaderProgramSystem.loadVAO(program, mesh);

		//		updateBuffers(vertices, colours, msize);

	}

	private ShaderProgram createProgram() {
		//create new shader program
		ShaderProgram program = new ShaderProgram();
		//generate shader program sources, replacing "max" values
		//max lights/max materials

		String vertSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_nolit_flat.vert"));
		String fragSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_nolit_flat.frag"));

		program.shaderSources.put(GLShaderType.VertexShader, vertSource);
		program.shaderSources.put(GLShaderType.FragmentShader, fragSource);

		//setup the program
		try {
			ShaderProgramSystem.create(program);
		} catch (Exception err) {
			err.printStackTrace();
			return null;
		}
		return program;
	}

	private int matrixBuffer;
	private static final int vpMatrixBindingpoint = 2;

	private void setupUBOs(ShaderProgram program) {

		glUseProgram(program.programID);
		//-----binding to the view/projection uniform buffer/block-----//
		if (true) {

			this.matrixBuffer = GL46.glGenBuffers();

			glBindBuffer(GL46.GL_UNIFORM_BUFFER, matrixBuffer);
			glBufferData(
				GL_UNIFORM_BUFFER,
				16 * 4 * 3,
				GL_DYNAMIC_DRAW);

			//binds the buffer to a binding index
			glBindBufferBase(GL_UNIFORM_BUFFER, vpMatrixBindingpoint, matrixBuffer);

			writeVPFMatrix(matrixBuffer, projectionMatrix, viewMatrix, viewProjectionMatrix);

			//binds the buffer to a binding index
			glBindBufferBase(GL_UNIFORM_BUFFER, vpMatrixBindingpoint, matrixBuffer);

			int actualIndex = GL46.glGetUniformBlockIndex(program.programID, "Matrices");
			//binds the binding index to the interface block (by index)
			glUniformBlockBinding(program.programID, actualIndex, vpMatrixBindingpoint);
		}

	}

	private void writeVPFMatrix(int buffer, Matrix4f projection, Matrix4f view, Matrix4f vp) {

		//no need to be in a program bidning, since this is shared between multiple programs
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
		float[] store = new float[16];

		if (projection != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 0, projection.get(store));
		if (view != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 1, view.get(store));
		if (vp != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 2, vp.get(store));

		glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);

	}

	private Mesh drawAxis(int size) {
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

		Mesh x = setup_3D_nolit_flat_mesh(verts, colours, count * 2);
		x.name = "axis";
		x.modelRenderType = GLDrawMode.Line;

		return x;
	}

	private ShaderProgram.Mesh setup_3D_nolit_flat_mesh(FloatBuffer vertices, FloatBuffer colours, int count) {
		ShaderProgram.Mesh mesh = new ShaderProgram.Mesh();
		{
			Attribute vertexAttrib = new Attribute();
			vertexAttrib.name = "vertex";
			vertexAttrib.bufferType = BufferType.ArrayBuffer;
			vertexAttrib.dataType = GLDataType.Float;
			vertexAttrib.bufferUsage = BufferUsage.StaticDraw;
			vertexAttrib.dataSize = 3;
			vertexAttrib.normalised = false;

			vertexAttrib.bufferResourceType = BufferDataManagementType.Buffer;
			vertexAttrib.buffer = vertices;

			mesh.VBOs.add(vertexAttrib);
		}

		{
			Attribute colourAttrib = new Attribute();
			colourAttrib.name = "colour";
			colourAttrib.bufferType = BufferType.ArrayBuffer;
			colourAttrib.dataType = GLDataType.Float;
			colourAttrib.bufferUsage = BufferUsage.StaticDraw;
			colourAttrib.dataSize = 4;
			colourAttrib.normalised = false;
			colourAttrib.instanced = false;
			colourAttrib.instanceStride = 1;
			colourAttrib.bufferResourceType = BufferDataManagementType.Buffer;
			colourAttrib.buffer = colours;

			mesh.VBOs.add(colourAttrib);
			mesh.attributes.put("colour", colourAttrib);
		}

		mesh.vertexCount = count;
		mesh.modelRenderType = GLDrawMode.Triangles;

		return mesh;

	}
}
