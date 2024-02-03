package com.meeple.citybuild.client.gui;

import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_BORDER;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_NO_SCROLLBAR;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_TITLE;
import static org.lwjgl.nuklear.Nuklear.nk_begin;
import static org.lwjgl.nuklear.Nuklear.nk_button_text;
import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_group_begin;
import static org.lwjgl.nuklear.Nuklear.nk_group_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_layout_space_begin;
import static org.lwjgl.nuklear.Nuklear.nk_layout_space_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_space_push;
import static org.lwjgl.nuklear.Nuklear.nk_rect;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;

import com.meeple.citybuild.client.render.Screen;
import com.meeple.citybuild.client.render.ShaderProgramDefinitions;
import com.meeple.citybuild.client.render.ShaderProgramDefinitions.ShaderProgramDefinition_UI;
import com.meeple.citybuild.server.Entity;
import com.meeple.citybuild.server.LevelData.Chunk.Tile;
import com.meeple.citybuild.server.WorldGenerator;
import com.meeple.citybuild.server.WorldGenerator.TileTypes;
import com.meeple.citybuild.server.WorldGenerator.Tiles;
import com.meeple.shared.Delta;
import com.meeple.shared.RayHelper;
import com.meeple.shared.frame.CursorHelper;
import com.meeple.shared.frame.CursorHelper.SpaceState;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgramSystem;
import com.meeple.shared.frame.camera.Camera;
import com.meeple.shared.frame.camera.Camera.CameraMode;
import com.meeple.shared.frame.camera.CameraSpringArm;
import com.meeple.shared.frame.nuklear.NuklearManager;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;
import com.meeple.shared.frame.window.hints.HasID;
import com.meeple.shared.utils.FrameUtils;

public class GameUI extends Screen {
	private static final Logger logger = Logger.getLogger(GameUI.class);

	static enum MouseButton implements HasID<Integer> {
		LClick(GLFW.GLFW_MOUSE_BUTTON_LEFT), RClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT),
		MClick(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);

		int id;

		private MouseButton(int id) {
			this.id = id;
		}

		@Override
		public Integer getID() {
			return id;
		}

	}

	static enum PanningType {
		/**
		 * Sim-city style. the difference between the press and current location.
		 * constant
		 */
		Difference,
		/**
		 * hold and drag. not constant. mouse "locks" to world position and movement of
		 * mouse moves the world
		 */
		Drag,
		/**
		 * This is the easiest way to control with controller. same as difference but
		 * from the middle of the screen
		 */
		FromCenter
	}

	static enum CompasState {
		None,
		Visible,
		Active
	}

	// TODO move to game-options
	// ---------------------settings -------------------------
	private static boolean invertMouse = true;
	private static boolean invertZoom = false;
	private static float zoomMult = 0.5f;
	// aka deadzone
	private static final float panRadi = 0.5f;
	private static Vector4f compasColour = new Vector4f(1, 0, 1, 1);
	private static Vector4f compasLineColour = new Vector4f(1, 1, 0, 1);

	private static float menuCancelSeconds = 1f;
	private static long menuDelayNanos = FrameUtils.secondsToNanos(menuCancelSeconds);
	private static final float menuRadi = 0.5f;
	/*
	 * //---------------------do not change-------------------------
	 * Wrapper<Vector2f> mouseRClick = new WrapperImpl<>();
	 * Wrapper<Vector2f> mouseMClick = new WrapperImpl<>();
	 * Wrapper<Vector2f> mouseLastPos = new WrapperImpl<>();
	 */
	private long windowID;
	private NkContext nkContext;

	// private Matrix4f orthoProjection;

	private Vector2f panningButtonPos = null;
	private Vector2f rotatingButtonPos = null;
	// Vector2f menuButtonPos = null;

	private Vector2f movement = new Vector2f();

	private Float scale = 0f;
	private Float pitch = 0f;
	private Float yaw = 0f;

	public CompasState panningState = CompasState.None;
	public CompasState rotatingState = CompasState.None;

	private ShaderProgramDefinition_UI.Mesh compasMesh;
	private ShaderProgramDefinition_UI.Mesh compasLineMesh;
	private Supplier<Matrix4f> orthoProjectionSupplier;

	/**
	 * the mouse left click ray. updated per frame if the left click has been
	 * pressed
	 */

	TileTypes currentSubMenu = null;
	Tiles currentAction = null;
	/**
	 * This is the mouse button that pans the camera
	 */
	static final MouseButton panningButton = MouseButton.RClick;
	/**
	 * this is the mouse button that rotates the camera
	 */
	static final MouseButton rotateButton = MouseButton.MClick;
	/**
	 * This is the mouse button that opens the menu and cancels current action
	 */
	static final MouseButton menuButton = MouseButton.RClick;
	/**
	 * This is the panning style
	 */
	static final PanningType panningType = PanningType.Difference;
	/**
	 * this is the rotation style.
	 */
	static final PanningType rotationType = PanningType.Difference;

	public void GLFWMouseButtonCallbackI(long windowID, int button, int action, int mods) {
		Matrix4f orthoProjection = orthoProjectionSupplier.get();
		/* only allows when no UI element is hovered */
		if (!Nuklear.nk_window_is_any_hovered(nkContext)) {
			if (button == panningButton.getID()) {
				if (action == GLFW.GLFW_PRESS) {
					if (panningType == PanningType.Difference) {
						Vector4f mouse = CursorHelper.getMouse(SpaceState.Eye_Space, windowID, orthoProjection,
								null);
						panningButtonPos = new Vector2f(mouse.x, mouse.y);
						updateCompas(panningButtonPos, compasMesh, compasLineMesh);
					} else if (panningType == PanningType.FromCenter) {
						panningButtonPos = new Vector2f(0, 0);
						updateCompas(panningButtonPos, compasMesh, compasLineMesh);
					}
				} else if (action == GLFW.GLFW_RELEASE) {
					panningButtonPos = null;
					updateCompas(panningButtonPos, compasMesh, compasLineMesh);
					updateCompasLine(panningButtonPos, compasLineMesh, windowID, orthoProjectionSupplier.get());
				}
			}
			if (button == rotateButton.getID()) {
				if (action == GLFW.GLFW_PRESS) {
					if (rotationType == PanningType.Difference) {
						Vector4f mouse = CursorHelper.getMouse(SpaceState.Eye_Space, windowID, orthoProjection,
								null);
						rotatingButtonPos = new Vector2f(mouse.x, mouse.y);
					} else if (rotationType == PanningType.FromCenter) {
						rotatingButtonPos = new Vector2f(0, 0);
					}
				} else if (action == GLFW.GLFW_RELEASE) {
					rotatingButtonPos = null;
				}
			}
			if (button == menuButton.getID()) {
				// begins tracking elsewhere
				if (action == GLFW.GLFW_PRESS) {
				} else if (action == GLFW.GLFW_RELEASE) {
					if (panningState != CompasState.Active) {
						if (currentAction != null) {
							currentAction = null;
						} else if (currentSubMenu != null) {
							currentSubMenu = null;
						}

					}
				}
			}
		}

	};

	public void GLFWCursorPosCallback(long window, double xpos, double ypos) {
		updateCompasLine(panningButtonPos, compasLineMesh, windowID, orthoProjectionSupplier.get());
		if (panningType == PanningType.Drag) {
			// basically check if it is being held down
			if (panningButtonPos != null) {
				movement.add((float) xpos, (float) ypos);
			}
		}

	
	};
	public void GLFWScrollCallbackI(long windowID, double xoffset, double yoffset) {
			scale = (float) yoffset + scale;
	}

	private Vector2f difference(Vector2f start, Vector2f current) {
		try {
			return new Vector2f(start).sub(current);
		} catch (NullPointerException e) {
			return null;
		}
	}

	public void init(long windowID, NkContext nkContext, Supplier<Matrix4f> orthoProj) {
		this.windowID = windowID;
		this.nkContext = nkContext;

		this.orthoProjectionSupplier = orthoProj;

	}

	public void setupCompas(GLContext glContext, ShaderProgram program) {
		compasMesh = ShaderProgramDefinitions.collection.UI.createMesh();

		Vector2f[] points = FrameUtils.generateCircle(new Vector2f(0, 0), 1f, 32);

		for (Vector2f circv : points) {
			FrameUtils.appendToList(compasMesh.vertexAttribute.data, circv);
		}
		compasMesh.modelRenderType = GLDrawMode.LineLoop;

		FrameUtils.appendToList(compasMesh.colourAttribute.data, compasColour);
		compasMesh.name = "compas";
		compasMesh.zIndexAttribute.data.add(-1f);

		FrameUtils.appendToList(compasMesh.meshTransformAttribute.data, new Matrix4f());
		compasMesh.visible = false;
		compasMesh.vertexCount = points.length;
		ShaderProgramSystem.loadVAO(glContext, program, compasMesh);
	}

	public void setupCompasLine(GLContext glContext, ShaderProgram program) {
		compasLineMesh = ShaderProgramDefinitions.collection.UI.createMesh();
		compasLineMesh.vertexAttribute.data.add(0);
		compasLineMesh.vertexAttribute.data.add(0);
		compasLineMesh.vertexAttribute.data.add(0);
		compasLineMesh.vertexAttribute.data.add(1);
		compasLineMesh.modelRenderType = GLDrawMode.Line;
		FrameUtils.appendToList(compasLineMesh.colourAttribute.data, compasLineColour);
		FrameUtils.appendToList(compasLineMesh.meshTransformAttribute.data, new Matrix4f().identity());
		compasLineMesh.name = "compasLine";
		compasLineMesh.zIndexAttribute.data.add(-1f);
		compasLineMesh.vertexCount = 2;
		compasLineMesh.visible = false;
		ShaderProgramSystem.loadVAO(glContext, program, compasLineMesh);

	}

	public void handlePitchingTick(ClientWindow window, Vector4f mousePositionInEyeSpace, CameraSpringArm arm) {

		Vector4f mousePos = mousePositionInEyeSpace;
		Vector2f dir = new Vector2f(mousePos.x, mousePos.y);
		{
			long mTicks = window.mousePressTicks.getOrDefault(rotateButton.getID(), 0l);

			if (mTicks > 0 && rotatingButtonPos != null) {

				Vector2f mouseDir = difference(rotatingButtonPos, dir);
				if (mouseDir != null) {

					if (invertMouse) {
						mouseDir.x = -mouseDir.x;
						mouseDir.y = -mouseDir.y;
					}
					// find dead-zone
					float len = mouseDir.length();
					mouseDir.normalize();
					if (mouseDir.isFinite()) {
						mouseDir = mouseDir.mul((len) * 0.1f);
						yaw = yaw + mouseDir.x;
						pitch = pitch + mouseDir.y;
						rotatingState = CompasState.Active;
					}
				}

			}

		}

		// TODO handle the keyboard press
		{
			float angle = 0;
			if (window.keyPressTicks.getOrDefault(GLFW.GLFW_KEY_E, 0l) > 0) {
				angle = 1f;
			}
			if (window.keyPressTicks.getOrDefault(GLFW.GLFW_KEY_Q, 0l) > 0) {
				angle = -1f;
			}
			if (angle != 0) {
				angle = angle * 2f;
				// arm.yaw += angle;
				yaw = yaw + angle;
			}
		}

		{
			arm.addPitch(-pitch);
			pitch = 0f;

			arm.yaw += yaw;
			yaw = 0f;

		}
	}

	public void handleScrollingTick(CameraSpringArm arm) {

		// handle smooth scale

		Float s = scale;
		if (s != null) {

			float nd = (float) (s * zoomMult);
			if (invertZoom) {
				arm.addDistance(nd);
			} else {
				arm.addDistance(-nd);
			}
			// roughly 10% less each tick
			scale = s - (s * 0.15f);
			if (Math.abs(scale) < 1f) {
				scale = 0f;
			}

		}
	}

	public void handlePanningTick(ClientWindow window, Vector4f mousePositionInEyeSpace, Camera view,
			Entity cameraAnchor) {

		Vector4f mousePos = mousePositionInEyeSpace;
		float rotZ = 0;
		switch (view.cameraMode) {
			case LookAt:
				rotZ = view.springArm.yaw;
				break;
			case Normal:
				rotZ = view.rotation.y;
				break;
			default:
				break;

		}

		Vector2f dir = new Vector2f(mousePos.x, mousePos.y);
		{
			long rTicks = window.mousePressTicks.getOrDefault(GLFW.GLFW_MOUSE_BUTTON_RIGHT, 0l);

			if (rTicks > 0 && panningButtonPos != null) {
				Vector2f mouseDir = difference(panningButtonPos, dir);
				if (mouseDir != null) {
					if (invertMouse) {
						mouseDir.x = -mouseDir.x;
						mouseDir.y = -mouseDir.y;
					}

					FrameUtils.rotateThis(mouseDir, -(float) Math.toRadians(rotZ));

					// find dead-zone
					float len = mouseDir.length();
					mouseDir.normalize();
					if (len < panRadi) {
						panningState = CompasState.Visible;
						len = 0;
					} else if (mouseDir.isFinite()) {
						mouseDir = mouseDir.mul((len) * 0.1f * 0.1f);
						float scale = 1f;
						// almost always
						if (view.cameraMode == CameraMode.LookAt) {
							scale = view.springArm.getDistance();
						} else {
							scale = view.position.z;
						}
						mouseDir = mouseDir.mul(scale);
						cameraAnchor.position.add(mouseDir.x, mouseDir.y, 0);
						panningState = CompasState.Active;
					}
				}
			} else {
				panningState = CompasState.None;
			}

		}

		if (panningState == CompasState.None) {
			Vector3f mdir = new Vector3f();
			if (window.keyPressTicks.getOrDefault(GLFW.GLFW_KEY_W, 0l) > 0) {
				mdir.y += 1;
			}
			if (window.keyPressTicks.getOrDefault(GLFW.GLFW_KEY_S, 0l) > 0) {
				mdir.y -= 1;
			}
			if (window.keyPressTicks.getOrDefault(GLFW.GLFW_KEY_D, 0l) > 0) {
				mdir.x += 1;
			}
			if (window.keyPressTicks.getOrDefault(GLFW.GLFW_KEY_A, 0l) > 0) {
				mdir.x -= 1;
			}
			mdir.normalize();
			if (mdir.isFinite()) {

				mdir = mdir.rotateZ(-(float) Math.toRadians(rotZ));

				float scale = 1f;
				// almost always
				if (view.cameraMode == CameraMode.LookAt) {
					scale = view.springArm.getDistance();
				} else {
					scale = view.position.z;
				}
				mdir = mdir.mul(scale / 100f);
				cameraAnchor.position.add(mdir);
			}
		}

	}

	private static void updateCompas(Vector2f panningButtonPos, ShaderProgramDefinition_UI.Mesh compasMesh,
			ShaderProgramDefinition_UI.Mesh compasLineMesh) {

		if (panningButtonPos == null) {
			compasLineMesh.visible = false;
			compasMesh.visible = false;
		}

		if (panningButtonPos != null) {
			Vector2f mouseClickedPos = new Vector2f(panningButtonPos);
			Matrix4f matrix = new Matrix4f().translate(mouseClickedPos.x, mouseClickedPos.y, 0).scale(panRadi);
			float[] matrixData = matrix.get(new float[16]);
			for (int i = 0; i < 16; i++) {
				compasMesh.meshTransformAttribute.data.set(i, matrixData[i]);
			}
			compasMesh.meshTransformAttribute.update.set(true);
			compasMesh.visible = true;

		}

	}

	private static void updateCompasLine(Vector2f panningButtonPos, ShaderProgramDefinition_UI.Mesh compasLineMesh,
			long windowID, Matrix4f orthoProjection) {

		if (panningButtonPos == null) {
			// compasLineMesh.visible = false; // TODO uuh wtf this causes nuklear crash??
		}

		if (panningButtonPos != null) {

			Vector4f windowRay = CursorHelper.getMouse(SpaceState.Eye_Space, windowID, orthoProjection, null);
			Vector2f mouseDir = new Vector2f((float) (panningButtonPos.x - windowRay.x),
					(float) (panningButtonPos.y - windowRay.y));

			float len = mouseDir.length();
			if (len < panRadi) {
				len = 0;
			} else {
				float angle = new Vector2f(0, 1).angle(new Vector2f(-mouseDir.x, -mouseDir.y));
				// mCompasLine.mesh.visible = true;
				compasLineMesh.visible = true;

				Matrix4f matrix = new Matrix4f()
						.translate(panningButtonPos.x, panningButtonPos.y, 0)
						.rotate(angle, 0, 0, 1)
						.translate(0, panRadi, 0)
						.scale(len - panRadi);
				float[] matrixData = matrix.get(new float[16]);
				for (int i = 0; i < 16; i++) {

					compasLineMesh.meshTransformAttribute.data.set(i, matrixData[i]);
				}
				compasLineMesh.meshTransformAttribute.update.set(true);
			}

		}
	}

	public void preRenderMouseUI(Map<Integer,Long> mousePressTicks, ShaderProgram program, RayHelper rayHelper) {

		GL46.glEnable(GL46.GL_DEPTH_TEST);

		if (true) {
			if (currentAction != null) {

				long mouseLeftClick = mousePressTicks.getOrDefault(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0l);
				if (mouseLeftClick > 0) {
					Tile t = rayHelper.getCurrentTile();
					// TODO check if mouse over UI
					if (t != null) {
						logger.info("Need to queue to banked chunk set");
						t.type = currentAction;
						rayHelper.getCurrentChunk().rebake.set(true);
					}
				}
			}
		}

	}

	int btnWidth = 100;
	int btnHeight = 50;
	int btnSpacing = 10;

	@Override
	public void render(ClientWindow window, GLContext glContext, Delta delta) {

		NkContext ctx = window.nkContext.context;
		int width = (int) (window.bounds.width * 0.75f);
		int height = (int) (window.bounds.height * 0.125f);
		int x = (int) (window.bounds.width - width) / 2;
		int y = (int) (window.bounds.height - height);

		try (MemoryStack stack = stackPush()) {
			NkRect rect = NkRect.mallocStack(stack);

			if (nk_begin(ctx, "MainBuildingMenu", nk_rect(x, y, width, height, rect),
					NK_WINDOW_BORDER | NK_WINDOW_NO_SCROLLBAR | NK_WINDOW_TITLE)) {

				nk_layout_row_dynamic(ctx, btnWidth + 25, 1);

				if (nk_group_begin(ctx, "", 0)) {
					nk_layout_space_begin(ctx, Nuklear.NK_STATIC, btnHeight, WorldGenerator.TileTypes.values().length);
					int i = 0;
					for (TileTypes type : WorldGenerator.TileTypes.values()) {
						nk_layout_space_push(ctx,
								nk_rect((btnWidth + btnSpacing) * i++, 0, btnWidth, btnHeight, NkRect.create()));
						if (currentSubMenu != null && currentSubMenu == type) {
							NuklearManager.styledButton(ctx, NuklearMenuSystem.getDisabled(ctx, stack), () -> {
								if (nk_button_text(ctx, type.toString())) {
									currentSubMenu = null;
									currentAction = null;
								}
							});
						} else {
							if (nk_button_text(ctx, type.toString())) {
								logger.trace(type.toString() + " has been clicked");
								currentSubMenu = type;
							}
						}

					}

					nk_layout_space_end(ctx);
					nk_group_end(ctx);
				}

			}
			nk_end(ctx);

			if (currentSubMenu != null) {

				if (nk_begin(ctx, "BuildingSubMenu", nk_rect(x, y - height, width, height, rect),
						NK_WINDOW_BORDER | NK_WINDOW_NO_SCROLLBAR | NK_WINDOW_TITLE)) {

					nk_layout_row_dynamic(ctx, btnWidth + 25, 1);

					if (nk_group_begin(ctx, "", 0)) {
						nk_layout_space_begin(ctx, Nuklear.NK_STATIC, btnHeight,
								WorldGenerator.TileTypes.values().length);
						int i = 0;
						Set<Tiles> tileSet = WorldGenerator.typesByTypes.get(currentSubMenu);
						if (tileSet != null) {
							synchronized (tileSet) {
								for (Iterator<Tiles> it = tileSet.iterator(); it.hasNext();) {
									Tiles t = it.next();
									nk_layout_space_push(ctx, nk_rect((btnWidth + btnSpacing) * i++, 0, btnWidth,
											btnHeight, NkRect.create()));
									if (currentAction != null && currentAction == t) {
										NuklearManager.styledButton(ctx, NuklearMenuSystem.getDisabled(ctx, stack),
												() -> {
													nk_button_text(ctx, t.toString());
												});
									} else {
										if (nk_button_text(ctx, t.toString())) {
											logger.trace(t.toString() + " has been clicked");
											currentAction = t;
										}
									}
								}
							}
						}

						nk_layout_space_end(ctx);
						nk_group_end(ctx);
					}

				}
				nk_end(ctx);
			}
		}
	}

}
