package com.meeple.display.views;

import static org.lwjgl.nuklear.Nuklear.NK_EDIT_FIELD;
import static org.lwjgl.nuklear.Nuklear.NK_RGBA;
import static org.lwjgl.nuklear.Nuklear.NK_TEXT_ALIGN_LEFT;
import static org.lwjgl.nuklear.Nuklear.nk_begin;
import static org.lwjgl.nuklear.Nuklear.nk_button_label;
import static org.lwjgl.nuklear.Nuklear.nk_check_label;
import static org.lwjgl.nuklear.Nuklear.nk_color_picker;
import static org.lwjgl.nuklear.Nuklear.nk_combo_begin_label;
import static org.lwjgl.nuklear.Nuklear.nk_combo_end;
import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_label;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_propertyf;
import static org.lwjgl.nuklear.Nuklear.nk_selectable_label;
import static org.lwjgl.nuklear.Nuklear.nk_slide_float;
import static org.lwjgl.nuklear.Nuklear.nk_widget_width;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkColorf;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkPluginFilter;
import org.lwjgl.nuklear.NkPluginFilterI;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;

import com.meeple.backend.Client;
import com.meeple.backend.FrameTimings;
import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.ShaderPrograms.Program;
import com.meeple.backend.game.Island;
import com.meeple.backend.game.Island.GroundSubType;
import com.meeple.backend.noise.CircleNoise;
import com.meeple.backend.noise.MultiCircleNoise;
import com.meeple.backend.noise.Noise;
import com.meeple.backend.noise.NoiseStack;
import com.meeple.backend.noise.OctavedNoise;
import com.meeple.backend.view.VPMatrix;
import com.meeple.backend.view.VPMatrix.CameraKey;
import com.meeple.display.Display;
import com.meeple.shared.ColourUtils;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2.ShaderClosable;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.nuklear.NuklearManager;
import com.meeple.shared.frame.window.UserInput.EventOrigin;
import com.meeple.shared.frame.window.UserInput.KeyBinding;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;
import com.meeple.temp.IslandOrig.IslandSize;

public class NoiseView {

	private NkColorf background = NkColorf.create().r(0.10f).g(0.18f).b(0.24f).a(1.0f);

	private Mesh axisMesh;
	private Map<GroundSubType, Mesh> chunkMeshes = new HashMap<>();
	private Mesh seaLevelMesh;

	float gridSize = 1;
	float seaLevel = 0.5f;
	float hillStart = 0.8f;
	float shallowDepth = 0.1f;

	private String seedText = "0";
	private NkPluginFilterI seedFilter = NkPluginFilter.create(Nuklear::nnk_filter_default);

	private IslandSize currentSize = IslandSize.SMALL;
	private float sampleRotation = 0f;

	private boolean autoRotateCamera = true;

	private boolean showGenerateIslandGroup = false;
	private boolean showMeshProperties = true;
	private boolean showViewGroup = false;
	private boolean showBackgroundPicker = false;

	private float viewRotation;
	private Vector3f viewPosition = new Vector3f();
	private float viewHeight = 17f;
	private float range = 17f;
	private float fov = 60;
	private CameraKey primary;

	private static final int noiseCount = 3;
	private boolean[] showSampleGroup = new boolean[noiseCount];
	private boolean showMultGroup = false;
	private Noise<?>[] noises = new Noise[noiseCount];
	private String[] noiseNames = new String[noiseCount];
	private Mesh[] noiseMeshes = new Mesh[noiseCount];
	private Mesh noiseMult;

	NoiseStack stack = new NoiseStack();
	DecimalFormat format = new DecimalFormat("0.000");
	// CircleNoise cNoise;
	boolean showCircleNoiseGroup = true;
	float sampleCRadi = 1;
	float sampleCMeshSize = 1;
	// MultiCircleNoise sNoise;
	boolean showSimplexNoiseGroup = true;
	float sampleSRadi = 10f;
	float sampleSMeshSize = 1000;
	Island i = new Island();
	
	KeyBinding zoomInKeybind;
	KeyBinding zoomOutKeybind;

	public void setup(Client client, VPMatrix vpMatrix) {
		primary = vpMatrix.newCamera();

		axisMesh = drawAxis(100);
		ShaderProgramSystem2.loadVAO(client.glContext, Program._3D_Unlit_Flat.program, axisMesh);

		setupGridMesh(client.glContext);

		// setupArrayMesh(chunkMeshes, this.stack, gridSize, sampleRotation, seaLevel,
		// 1);

		seaLevelMesh = setupSeaLevel(new float[] { background.r(), background.g(), background.b(), background.a() }, seaLevel - shallowDepth);
		ShaderProgramSystem2.loadVAO(client.glContext, Program._3D_Unlit_Flat.program, seaLevelMesh);
		noiseNames = new String[] { "Circle", "Multi_circle", "Octaved" };
		
		
		zoomInKeybind = client.userInput.new KeyBinding("camera-zoom-in", GLFW.GLFW_KEY_MINUS, EventOrigin.Keyboard);
		zoomOutKeybind = client.userInput.new KeyBinding("camera-zoom-in", GLFW.GLFW_KEY_EQUAL, EventOrigin.Keyboard);
	}

	public void renderShared(VPMatrix vpMatrix, Client client, FrameTimings delta) {

		// NOTE pre-render

		{
			if (autoRotateCamera) {
				viewRotation += (float) (delta.deltaSeconds * 0.125f * (float) Math.PI);
				viewRotation = (float) (viewRotation % (2d * Math.PI));
			}
			vpMatrix.setPerspective(fov, (float) client.windowWidth / client.windowHeight, 0.01f, 100.0f);

			// setting the view position defined by the rotation previously set and a radius
			viewPosition.set(range * (float) Math.cos(-viewRotation), range * (float) Math.sin(-viewRotation), viewHeight);
			// setting the view matrix to look at 000 from view position
			vpMatrix.getCamera(primary, true).setLookAt(viewPosition.x, viewPosition.y, viewPosition.z, 0f, 0f, range * 0.25f, 0f, 0f, 1f);

			// System.out.println(viewHeight + " "+ range);
		}

		vpMatrix.activeCamera(primary);
		vpMatrix.upload();
	}

	// SPLIT
	private Vector2i renderShared(Client client) {
		glClearColor(background.r(), background.g(), background.b(), background.a());

		// NOTE this denotes that GL is using a new frame.
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		
		if (client.userInput.isPressed(zoomInKeybind)) {
			viewHeight -= 0.1;
		} else if (client.userInput.isPressed(zoomOutKeybind)) {
			viewHeight += 0.1f;
		}

		int x = ((int) (client.windowWidth * 0.8f) / 2);

		int y = ((int) (client.windowHeight) / 2);
		return new Vector2i(x, y);
	}

	public void drawNoiseMenu(GLContext glContext, NkContextSingleton nkc, MemoryStack stack, NkRect rect) {

		Wrapper<Boolean> update = new WrapperImpl<>(false);
		if (noises[0] == null) {
			CircleNoise cNoise = new CircleNoise(new Vector2f(), 5000).setCubic(0.5f, -1f, 1f, 0.45f);

			noises[0] = cNoise;
			this.stack.addNoise(cNoise);
			update.setWrapped(update.getWrappedOrDefault(false) | true);
			updateMesh(glContext, 0);
		}
		if (noises[1] == null) {
			MultiCircleNoise sNoise = new MultiCircleNoise().setCubic(0, -0.3f, 1, 0);

			long seed = 0;

			try {
				seed = Long.parseLong(seedText.trim());
			} catch (NumberFormatException e) {
				seed = seedText.hashCode();
				e.printStackTrace();
			}
			Random r = new Random(seed);
			int max = 10 + r.nextInt(20) + r.nextInt(20);
			for (int i = 0; i < max; i++) {
				float x = ((r.nextFloat() * 2) - 1) * (1250 / 2);
				float y = ((r.nextFloat() * 2) - 1) * (1250 / 2);
				int maxSizes = IslandSize.values().length * 10;
				int sizeIndex = r.nextInt(maxSizes - 1) / 10;

				IslandSize size = IslandSize.values()[sizeIndex];

				sNoise.addCircle(i, new Vector4f(x, y, size.pythag * 2f, r.nextFloat() * 2f - 1f));
			}
			update.setWrapped(update.getWrappedOrDefault(false) | true);

			noises[1] = sNoise;
			this.stack.addNoise(sNoise);
			updateMesh(glContext, 1);
		}
		if (noises[2] == null) {

			OctavedNoise oNoise = new OctavedNoise(6f, 500, 0.5f).setCubic(0, 0, 1, 0.5f);
			update.setWrapped(update.getWrappedOrDefault(false) | true);
			this.stack.addNoise(oNoise);
			noises[2] = oNoise;
			updateMesh(glContext, 2);
		}
		if (nk_begin(nkc.context, "f2o", rect, 0)) {
			nk_layout_row_dynamic(nkc.context, 25, 1);
			for (int i = 0; i < noiseCount; i++) {
				int ii = i;
				showSampleGroup[i] = NuklearManager.collapsableGroup(nkc.context, "Noise: " + noiseNames[i], 25, showSampleGroup[i], (int) (25 * 20.5f), new Runnable() {

					@Override
					public void run() {
						update.setWrapped(update.getWrappedOrDefault(false) | drawNoiseInner(glContext, nkc, ii));

					}
				});

			}
			showMultGroup = NuklearManager.collapsableGroup(nkc.context, "Final Noise", 25, showMultGroup, (int) (25 * 20.5f), new Runnable() {

				@Override
				public void run() {

				}

			});
			if (update.getWrappedOrDefault(false)) {
				noiseMult = updateFinal(glContext, noiseMult);
			}

		}
		nk_end(nkc.context);
	}

	private boolean drawNoiseInner(GLContext glContext, NkContextSingleton nkc, int i) {

		boolean update = false;

		update |= noises[i].drawMenu(nkc, format);
		if (update) {
			updateMesh(glContext, i);
		}
		return update;
	}

	public void renderNoise(Client client, FrameTimings delta) {
		Vector2i size = renderShared(client);
		// NOTE actual render
		GL46.glPushAttrib(GL46.GL_VIEWPORT_BIT);

		try (ShaderClosable sc = ShaderProgramSystem2.useProgram(Program._3D_Unlit_Flat.program)) {

			// NOTE TOP LEFT
			{
				GL46.glViewport(0, size.y, size.x, size.y);
				GL46.glPointSize(3f);

				ShaderProgramSystem2.tryFullRenderMesh(noiseMeshes[0]);
				ShaderProgramSystem2.tryFullRenderMesh(axisMesh);

			}
			// NOTE TOP RIGHT
			{
				GL46.glViewport(size.x, size.y, size.x, size.y);
				GL46.glPointSize(3f);
				ShaderProgramSystem2.tryFullRenderMesh(noiseMeshes[1]);
				ShaderProgramSystem2.tryFullRenderMesh(axisMesh);

			}
			// NOTE BOTTOM LEFT
			{

				GL46.glViewport(0, 0, size.x, size.y);
				GL46.glPointSize(3f);
				ShaderProgramSystem2.tryFullRenderMesh(axisMesh);
				ShaderProgramSystem2.tryFullRenderMesh(noiseMeshes[2]);

			}
			// NOTE BOTTOM RIGHT
			{
				GL46.glViewport(size.x, 0, size.x, size.y);

				ShaderProgramSystem2.tryFullRenderMesh(axisMesh);
				ShaderProgramSystem2.tryFullRenderMesh(noiseMult);

			}

		}
		GL46.glPopAttrib();

	}

	public void drawIslandMenu(NkContext ctx, MemoryStack stack, NkRect rect) {

		if (nk_begin(ctx, "IslandOrig Gen", rect, 0)) {

			nk_layout_row_dynamic(ctx, 10, 1);
			showGenerateIslandGroup = NuklearManager.collapsableGroup(ctx, "Generate Island", 25, showGenerateIslandGroup, 25 * 6, () ->
			{
				nk_layout_row_dynamic(ctx, 25, 2);

				{
					nk_label(ctx, "Seed: ", NK_TEXT_ALIGN_LEFT);
					seedText = NuklearManager.textArea(ctx, stack, seedText, 10, NK_EDIT_FIELD, seedFilter);
				}

				nk_label(ctx, "Size", NK_TEXT_ALIGN_LEFT);
				if (nk_combo_begin_label(ctx, currentSize.toString(), NkVec2.mallocStack(stack).set(nk_widget_width(ctx), 400))) {

					nk_layout_row_dynamic(ctx, 25, 1);

					for (IslandSize sz : IslandSize.values()) {
						if (nk_selectable_label(ctx, sz.toString(), NK_TEXT_ALIGN_LEFT, new int[] { sz == currentSize ? 1 : 0 })) {
							currentSize = sz;
						}
					}
					nk_combo_end(ctx);
				}

				nk_label(ctx, "Sample rotation", NK_TEXT_ALIGN_LEFT);
				sampleRotation = nk_propertyf(ctx, "r", -1, sampleRotation, 1, 0.1f, 0.01f);

				nk_layout_row_dynamic(ctx, 25, 1);
				if (nk_button_label(ctx, "Generate")) {
					long seed = 0;

					try {
						seed = Long.parseLong(seedText.trim());
					} catch (NumberFormatException e) {
						seed = seedText.hashCode();
						e.printStackTrace();
					}
					this.stack.setSeed(new Random(seed));

					i.init(seaLevel, shallowDepth, hillStart);
					i.setupArrayMesh(chunkMeshes, this.stack, gridSize, sampleRotation, seaLevel, 1);
				}

			});

			nk_layout_row_dynamic(ctx, 10, 1);
			showMeshProperties = NuklearManager.collapsableGroup(ctx, "Chunk properties", 25, showMeshProperties, 25 * 7, () ->
			{

				boolean update = false;

				nk_layout_row_dynamic(ctx, 25, 2);
				nk_label(ctx, "Grid size", NK_TEXT_ALIGN_LEFT);
				float rGridSize = nk_propertyf(ctx, "1", 1f, gridSize, 5f, 0.1f, 0.1f);
				if (rGridSize != gridSize) {
					gridSize = rGridSize;
					update = true;
				}

				nk_label(ctx, "Sea level", NK_TEXT_ALIGN_LEFT);
				float rSeaLevel = nk_propertyf(ctx, "2", -1f, seaLevel, 3, 0.001f, 0.001f);
				if (rSeaLevel != seaLevel) {
					seaLevel = rSeaLevel;
					seaLevelMesh.getAttribute(ShaderPrograms.transformAtt.name).data(new Matrix4f().scaleXY(100, 100).translate(0, 0, seaLevel - shallowDepth).get(new float[16])

					).update.set(true);
					update = true;
				}

				nk_label(ctx, "Hill height", NK_TEXT_ALIGN_LEFT);
				float rHillStart = nk_propertyf(ctx, "3", -1f, hillStart, 3, 0.001f, 0.001f);
				if (rHillStart != hillStart) {
					hillStart = rHillStart;
					update = true;
				}

				nk_label(ctx, "Shallow depth", NK_TEXT_ALIGN_LEFT);
				float rShallowDepth = nk_propertyf(ctx, "4", 0f, shallowDepth, 0.2f, 0.001f, 0.001f);
				if (rShallowDepth != shallowDepth) {
					shallowDepth = rShallowDepth;
					update = true;
				}

				if (update) {

					i.init(seaLevel, shallowDepth, hillStart);
					i.setupArrayMesh(chunkMeshes, this.stack, gridSize, sampleRotation, seaLevel, 1);

				}
			});

			// NOTE padding
			nk_layout_row_dynamic(ctx, 10, 1);
			showViewGroup = NuklearManager.collapsableGroup(ctx, "View", 25, showViewGroup, 25 * 9, () ->
			{
				nk_layout_row_dynamic(ctx, 25, 1);
				autoRotateCamera = nk_check_label(ctx, "Enable auto rotate", autoRotateCamera);

				float newRot = nk_slide_float(ctx, 0, viewRotation, (float) (2f * Math.PI), autoRotateCamera ? 0.0000001f : 0.01f);
				if (!autoRotateCamera) {
					viewRotation = newRot;
				}
				/*
				 * nk_label(ctx, "View scroll", NK_TEXT_ALIGN_LEFT); float newVHR =
				 * nk_slide_float(ctx, 0, (viewHeight + range) / 2, 100f, 0.5f); if (newVHR !=
				 * perFrameCheck) { // perFrameCheck = viewHeight = range = newVHR; }
				 */

				nk_label(ctx, "View height", NK_TEXT_ALIGN_LEFT);
				viewHeight = nk_slide_float(ctx, 0.5f, viewHeight, 10f, 0.5f);

				nk_label(ctx, "View range", NK_TEXT_ALIGN_LEFT);
				range = nk_slide_float(ctx, 1f, range, 10f, 0.5f);

				axisMesh.visible = nk_check_label(ctx, "Show Axis", axisMesh.visible);
			});
			// NOTE padding
			nk_layout_row_dynamic(ctx, 10, 1);
			showBackgroundPicker = NuklearManager.collapsableGroup(ctx, "Background", 25, showBackgroundPicker, (int) (5.5f * 25 + 120), () ->
			{

				nk_layout_row_dynamic(ctx, 120, 1);
				nk_color_picker(ctx, background, NK_RGBA);
				nk_layout_row_dynamic(ctx, 25, 1);
				background.r(nk_propertyf(ctx, "#R:", 0, background.r(), 1.0f, 0.01f, 0.005f)).g(nk_propertyf(ctx, "#G:", 0, background.g(), 1.0f, 0.01f, 0.005f)).b(nk_propertyf(ctx, "#B:", 0, background.b(), 1.0f, 0.01f, 0.005f)).a(nk_propertyf(ctx, "#A:", 0, background.a(), 1.0f, 0.01f, 0.005f));

				seaLevelMesh.getAttribute(ShaderPrograms.colourAtt.name).data(
					new float[] { background.r(), background.g(), background.b(), background.a() }).update.set(true);

			});

			// NOTE padding
			nk_layout_row_dynamic(ctx, 10, 1);
			nk_layout_row_dynamic(ctx, 25, 1);

		}
		nk_end(ctx);

	}

	public void renderIsland(Client client, VPMatrix vpMatrix, FrameTimings delta) {
		Vector2i size = renderShared(client);
		// NOTE actual render
		GL46.glPushAttrib(GL46.GL_VIEWPORT_BIT);

		try (ShaderClosable sc = ShaderProgramSystem2.useProgram(Program._3D_Unlit_Flat.program)) {

			// NOTE TOP LEFT
			{
				GL46.glViewport(0, size.y, size.x, size.y);

				GL46.glPointSize(3f);
				ShaderProgramSystem2.tryFullRenderMesh(noiseMult);
				ShaderProgramSystem2.tryFullRenderMesh(axisMesh);

			}
			// NOTE TOP RIGHT
			{
				GL46.glViewport(size.x, size.y, size.x, size.y);
				Matrix4f old = new Matrix4f(vpMatrix.getCamera(primary));
				vpMatrix.getCamera(primary).translate(0, 0, -(seaLevel - shallowDepth) / 2);
				vpMatrix.upload();
				GL46.glPointSize(3f);

				ShaderProgramSystem2.tryFullRenderMesh(seaLevelMesh);
				ShaderProgramSystem2.tryFullRenderMesh(noiseMult);
				vpMatrix.getCamera(primary).set(old);
				vpMatrix.upload();

			}
			// NOTE BOTTOM LEFT
			{
				GL46.glViewport(0, 0, size.x, size.y);
				GL46.glPointSize(2f);
				ShaderProgramSystem2.tryFullRenderMesh(seaLevelMesh);
				ShaderProgramSystem2.tryFullRenderMesh(noiseMult);
				ShaderProgramSystem2.tryFullRenderMesh(axisMesh);
				for (Mesh mesh : chunkMeshes.values()) {
					ShaderProgramSystem2.tryFullRenderMesh(mesh);
				}

			}
			// NOTE BOTTOM RIGHT
			{
				GL46.glViewport(size.x, 0, size.x, size.y);
				GL46.glPointSize(2f);
				// ShaderProgramSystem2.tryFullRenderMesh(seaLevelMesh);
				// ShaderProgramSystem2.tryFullRenderMesh(noiseMult);
				ShaderProgramSystem2.tryFullRenderMesh(axisMesh);
				for (Mesh mesh : chunkMeshes.values()) {
					ShaderProgramSystem2.tryFullRenderMesh(mesh);

				}
			}

		}
		GL46.glPopAttrib();

	}

	private void updateMesh(GLContext glContext, int i) {

		// loop diameter
		int rate = 500;
		float vertexScaling = 10f;
		float sampleScaling = 2500f;

		// NOTE setup the mesh buffers abd render count
		int vertexCount = 2 * rate * 2 * rate;
		FloatBuffer vertices = BufferUtils.createFloatBuffer((int) (vertexCount * 3));
		FloatBuffer colours = BufferUtils.createFloatBuffer((int) (vertexCount * 4));
		int render = 0;

		// NOTE normalised coord loop
		/// this is to seperate between sampling and the vertex positions
		for (float x = -1; x < 1; x += (2f / rate)) {
			for (float y = -1; y < 1; y += (2f / rate)) {
				// NOTE sampling at the specified scale
				float sampleX = x * sampleScaling;
				float sampleY = y * sampleScaling;

				// NOTE the coords at the specified scale
				float verX = x * vertexScaling;
				float verY = y * vertexScaling;

				float value = noises[i].sample(sampleX, sampleY);
				Vector4f c = ColourUtils.hslToRgb(-1f / value + 2f, 1, 0.5f);
				vertices.put(verX);
				vertices.put(verY);
				vertices.put(value);

				FrameUtils.appendToBuffer(colours, c);
				render += 1;
			}
		} /*
			 * for (float x = -minmax * rate; x < minmax * rate; x += rate) { for (float y =
			 * -minmax * rate; y < minmax * rate; y += rate) { float value =
			 * noises[i].sample(x * sampleScaling, y * sampleScaling);
			 * 
			 * Vector4f c = ColourUtils.hslToRgb(-1f / value + 2f, 1, 0.5f); //
			 * System.out.println(x + " " + y); // System.out.println((x / minmax) + " " +
			 * (y / minmax)); // System.out.println(); vertices.put((x / minmax));
			 * vertices.put((y / minmax)); vertices.put(value);
			 * 
			 * FrameUtils.appendToBuffer(colours, c); render += 1; } }
			 */
		if (noiseMeshes[i] != null) {
			ShaderProgramSystem2.deleteVAO(glContext, Program._3D_Unlit_Flat.program, noiseMeshes[i]);
		}

		noiseMeshes[i] = new Mesh().drawMode(GLDrawMode.Points).name("noisemesh_"
			+ noiseNames[i])
			.vertexCount(render)
			.addAttribute(ShaderPrograms.vertAtt.build().bufferUsage(BufferUsage.StaticDraw).data(vertices))
			.addAttribute(ShaderPrograms.colourAtt.build().data(colours)).addAttribute(ShaderPrograms.transformAtt.build().data(new Matrix4f().get(new float[16])));
		ShaderProgramSystem2.loadVAO(glContext, Program._3D_Unlit_Flat.program, noiseMeshes[i]);

	}

	private Mesh updateFinal(GLContext glContext, Mesh mesh) {

		int vSize = 500;
		float minmax = vSize / 2f;
		int vertexCount = vSize * vSize;
		FloatBuffer vertices = BufferUtils.createFloatBuffer((int) (vertexCount * 3));
		FloatBuffer colours = BufferUtils.createFloatBuffer((int) (vertexCount * 4));
		int render = 0;

		float scale = 20f;
		for (float x = -minmax * scale; x < minmax * scale; x += scale) {
			for (float y = -minmax * scale; y < minmax * scale; y += scale) {
				float sample = this.stack.sample(x, y);
				Vector2f pos = new Vector2f(x, y);
				Vector4f c = ColourUtils.hslToRgb(-1f / sample + 2f, 1, 0.5f);

				vertices.put((pos.x / minmax));
				vertices.put((pos.y / minmax));
				vertices.put(sample);

				FrameUtils.appendToBuffer(colours, c);
				render += 1;
			}
		}

		if (mesh != null) {
			ShaderProgramSystem2.deleteVAO(glContext, Program._3D_Unlit_Flat.program, mesh);
		}

		Mesh meshOut = new Mesh().drawMode(GLDrawMode.Points).name("noisemesh_"
			+ noiseMult).vertexCount(render).addAttribute(ShaderPrograms.vertAtt.build().bufferUsage(BufferUsage.StaticDraw).data(vertices)).addAttribute(ShaderPrograms.colourAtt.build().data(colours)).addAttribute(ShaderPrograms.transformAtt.build().data(new Matrix4f().get(new float[16])));
		ShaderProgramSystem2.loadVAO(glContext, Program._3D_Unlit_Flat.program, meshOut);
		return meshOut;
	}

	private static Mesh setupSeaLevel(float[] colour, float initHeight) {
		Mesh mesh = new Mesh().drawMode(GLDrawMode.TriangleFan)
			.vertexCount(4)
			.name("sea_level")
			.renderCount(1)
			.addAttribute(ShaderPrograms.vertAtt.build().data(Display.planeVertices))
			.addAttribute(ShaderPrograms.colourAtt.build().instanced(true, 1).data(colour))
			.addAttribute(ShaderPrograms.transformAtt.build().data(new Matrix4f().scaleXY(100, 100).translate(0, 0, initHeight).get(new float[16])));
		return mesh;
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

		Mesh x = new Mesh();
		x.name("axis");
		x.vertexCount(count * 2);
		x.renderMode(GLDrawMode.Line);
		x.addAttribute(ShaderPrograms.vertAtt.build().data(verts));
		x.addAttribute(ShaderPrograms.colourAtt.build().data(colours));
		x.addAttribute(ShaderPrograms.transformAtt.build().data(new Matrix4f().get(new float[16])));
		return x;
	}

	private void setupGridMesh(GLContext glContext) {
		float[] colour = new float[4];
		for (GroundSubType gsb : GroundSubType.values()) {
			switch (gsb) {
			case Water_Deep:
				colour = new float[] { 0, 0, 1, 1, };
				break;

			case Water_Shallow:
				colour = new float[] { 200f / 255f, 200f / 255f, 10f / 255f, 1f

				};
				break;
			case Ground_High:
				colour = new float[] { 1, 0, 0, 1, };
				break;
			case Ground_Low:
				colour = new float[] { 0, 1, 0, 1, };
				break;
			default:
				break;
			}

			Mesh gridMesh = new Mesh().drawMode(GLDrawMode.TriangleFan).vertexCount(4).name("arraygrid_" + gsb.toString()).renderCount(0).addAttribute(
				ShaderPrograms.vertAtt.build().bufferUsage(BufferUsage.StaticDraw).data(Display.planeVertices)).addAttribute(ShaderPrograms.colourAtt.build().data(new float[] { colour[0], colour[1], colour[2], colour[3], colour[0],
					colour[1], colour[2], colour[3], colour[0], colour[1], colour[2], colour[3], colour[0], colour[1], colour[2], colour[3] })).addAttribute(ShaderPrograms.transformAtt.build().data(new Matrix4f().get(new float[16])));
			ShaderProgramSystem2.loadVAO(glContext, Program._3D_Unlit_Flat.program, gridMesh);
			Mesh old = chunkMeshes.put(gsb, gridMesh);
			if (old != null) {
				ShaderProgramSystem2.deleteVAO(glContext, Program._3D_Unlit_Flat.program, old);
			}
		}
	}
}
