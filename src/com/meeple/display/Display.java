package com.meeple.display;

import static org.lwjgl.nuklear.Nuklear.NK_TEXT_ALIGN_CENTERED;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_NO_SCROLLBAR;
import static org.lwjgl.nuklear.Nuklear.nk_begin;
import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_label;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_progress;
import static org.lwjgl.nuklear.Nuklear.nk_rect;
import static org.lwjgl.nuklear.Nuklear.nk_select_text;
import static org.lwjgl.nuklear.Nuklear.nk_style_pop_color;
import static org.lwjgl.nuklear.Nuklear.nk_style_push_color;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.system.MemoryStack.stackPush;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;

import com.meeple.backend.Client;
import com.meeple.backend.FrameTimings;
import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.ShaderPrograms.Program;
import com.meeple.backend.view.VPMatrix;
import com.meeple.display.views.NoiseView;
import com.meeple.display.views.WorldView;
import com.meeple.shared.frame.nuklear.NkContextSingleton;

public class Display extends Client {
	private static Logger logger = Logger.getLogger(Display.class);

	private static String debugLayout = "[%r][%d{HH:mm:ss:SSS}][%t][%p] (%F:%L) %m%n";

	public static void main(String[] args) {

		Logger.getRootLogger().setLevel(org.apache.log4j.Level.ALL);
		Appender a = new ConsoleAppender(new PatternLayout(debugLayout));
		BasicConfigurator.configure(a);

		try (Display d = new Display()) {
			d.setup(1400, 800, "Noise and island generator");
			d.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static final float sz = 0.5f;
	public final static float[] planeVertices = {
		-sz, sz, sz,
		sz, sz, sz,
		sz, -sz, sz,
		-sz, -sz, sz
	};

	public final static float[] cubeVertices = {
		-1.f, 1.f, 1.f, // Front-top-left
		1.f, 1.f, 1.f, // Front-top-right
		-1.f, -1.f, 1.f, // Front-bottom-left
		1.f, -1.f, 1.f, // Front-bottom-right
		1.f, -1.f, -1.f, // Back-bottom-right
		1.f, 1.f, 1.f, // Front-top-right
		1.f, 1.f, -1.f, // Back-top-right
		-1.f, 1.f, 1.f, // Front-top-left
		-1.f, 1.f, -1.f, // Back-top-left
		-1.f, -1.f, 1.f, // Front-bottom-left
		-1.f, -1.f, -1.f, // Back-bottom-left
		1.f, -1.f, -1.f, // Back-bottom-right
		-1.f, 1.f, -1.f, // Back-top-left
		1.f, 1.f, -1.f // Back-top-right
	};
	NoiseView noisePreview = new NoiseView();
	WorldView worldView = new WorldView();

	@Override
	public void setupGL() {
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		timingManager.desiredFrameRate = 130;

		vpMatrix.setupBuffer(glContext);

		ShaderPrograms.initAndCreate(glContext, Program._3D_Unlit_Flat);
		VPMatrix.bindToProgram(Program._3D_Unlit_Flat.program.programID, vpMatrix.getBindingPoint());

		GL46.glEnable(GL46.GL_DEPTH_TEST);
		/*
		 * this.userInput.scrollCallbackSet.add(new GLFWScrollCallbackI() {
		 * 
		 * @Override public void invoke(long window, double xoffset, double yoffset) {
		 * if (!Nuklear.nk_item_is_any_active(nkContext.context)) { range += yoffset *
		 * -0.5f; range = Math.max(range, 1); } } });
		 */

		// setupArrayMesh(chunkMeshes, this.stack, gridSize, sampleRotation, seaLevel,
		// 1);

		// noises[2] = stack.new CircleNoise(new Vector2f(), 100);

		// genNoiseMesh(noiseSampleMesh, island.map.size(), island::sampleNoise,
		// gridSize, sampleRotation, seaLevel, islandScaleXY);
		// genNoiseMesh(circleSampleMesh, island.map.size(), (x, y) ->
		// island.sampleCircleMap(x, y, new Vector2f(0, 0), island.size.pythag),
		// gridSize, sampleRotation, seaLevel, islandScaleXY);

		noisePreview.setup(this, vpMatrix);
		worldView.setup(this, vpMatrix);
		logger.trace("Finished setting up contexts and meshes");
	}

	@Override
	public void render(FrameTimings delta) {

		switch (currentTab) {
		case Island:
		case Noise:
			noisePreview.renderShared(vpMatrix, this, delta);
			break;
		case Test:
			worldView.preRender();
			break;

		}
		layout(nkContext, (int) (windowWidth * 0.75f), 0, (int) (windowWidth * 0.25f), windowHeight);

		switch (currentTab) {
		case Island:
			noisePreview.renderIsland(this, vpMatrix, delta);
			break;
		case Noise:
			noisePreview.renderNoise(this, delta);
			break;
		case Test:

			if (!worldView.world.generated) {

				worldView.worldClient.StartHold();
				worldView.world.generate();
				worldView.playerController.register(this.windowID, this.userInput);
				worldView.playerController.operateOn(vpMatrix.getCamera(worldView.primaryCamera));
			}
			float prog = worldView.worldClient.progress();

			if (prog < 1f) {
				if (nk_begin(this.nkContext.context, "loading", NkRect.create().set(50, 50, 500, 500), 0)) {

					nk_layout_row_dynamic(this.nkContext.context, 50, 1);
					nk_label(this.nkContext.context, "Loading", NK_TEXT_ALIGN_CENTERED);
					long max = 100;
					PointerBuffer pb = BufferUtils.createPointerBuffer(1);
					pb.put((long) (max * worldView.worldClient.progress()));
					pb.flip();
					nk_progress(this.nkContext.context, pb, max, false);

				}
				nk_end(this.nkContext.context);
			} else {
				worldView.worldClient.free();
			}
			if (worldView.world.generated && prog == 1f) {
				worldView.render(this, vpMatrix, delta);
			}
			break;

		}

	}

	private VPMatrix vpMatrix = new VPMatrix();

	private enum Tab {
		Noise, Island, Test
	}

	private Tab currentTab = Tab.Noise;

	private void layout(NkContextSingleton nkc, int x, int y, int w, int h) {

		try (MemoryStack stack = stackPush()) {
			NkRect rect = NkRect.mallocStack(stack);

			nk_style_push_color(nkc.context, nkc.context.style().window().fixed_background().data().color(), NkColor.create().set((byte) 0, (byte) 0, (byte) 0, (byte) 0));
			nk_style_push_color(nkc.context, nkc.context.style().selectable().normal().data().color(), NkColor.create().set((byte) 100, (byte) 100, (byte) 100, (byte) 255));
			nk_style_push_color(nkc.context, nkc.context.style().selectable().normal_active().data().color(), NkColor.create().set((byte) 50, (byte) 50, (byte) 50, (byte) 255));
			nk_style_push_color(nkc.context, nkc.context.style().selectable().hover().data().color(), NkColor.create().set((byte) 50, (byte) 50, (byte) 50, (byte) 255));
			nk_style_push_color(nkc.context, nkc.context.style().selectable().hover_active().data().color(), nkc.context.style().selectable().hover().data().color());

			if (nk_begin(nkc.context, "selection panel", nk_rect(x, y + 5, w, 25, rect), NK_WINDOW_NO_SCROLLBAR)) {
				Tab[] tabs = Tab.values();
				nk_layout_row_dynamic(nkc.context, 25, tabs.length);

				float rounding = nkc.context.style().selectable().rounding();
				nkc.context.style().selectable().rounding(5f);
				for (Tab tab : tabs) {
					if (nk_select_text(nkc.context, tab.name(), NK_TEXT_ALIGN_CENTERED, currentTab == tab)) {
						currentTab = tab;
					}
				}

				nkc.context.style().selectable().rounding(rounding);

			}
			nk_style_pop_color(nkc.context);
			nk_style_pop_color(nkc.context);
			nk_style_pop_color(nkc.context);
			nk_style_pop_color(nkc.context);
			nk_style_pop_color(nkc.context);

			nk_end(nkc.context);
			if (Nuklear.nk_input_is_mouse_hovering_rect(nkc.context.input(), rect))
				Nuklear.nk_window_set_focus(nkc.context, "selection panel");
			nk_rect(x, y + 30, w, h, rect);
			switch (currentTab) {
			case Island:
				noisePreview.drawIslandMenu(nkc.context, stack, rect);
				break;
			case Noise:
				noisePreview.drawNoiseMenu(glContext, nkc, stack, rect);
				break;
			case Test:
				break;
			}
		}
	}

	class GroundProperties {
		/**
		 * Can grow crops on the land or not
		 */
		boolean arable;
		/**
		 * Can have housing built here
		 */
		boolean habitable;

	}

	@Override
	protected void onClose() {
		service.shutdown();
		service.shutdownNow();
		super.onClose();
	}
}
