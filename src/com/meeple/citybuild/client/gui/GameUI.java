package com.meeple.citybuild.client.gui;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;

import org.apache.log4j.Logger;
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

import com.meeple.citybuild.RayHelper;
import com.meeple.citybuild.client.render.RenderingMain;
import com.meeple.citybuild.client.render.Screen;
import com.meeple.citybuild.client.render.WorldRenderer;
import com.meeple.citybuild.client.render.WorldRenderer.MeshExt;
import com.meeple.citybuild.server.Entity;
import com.meeple.citybuild.server.WorldGenerator;
import com.meeple.citybuild.server.WorldGenerator.TileTypes;
import com.meeple.shared.Delta;
import com.meeple.shared.frame.CursorHelper;
import com.meeple.shared.frame.CursorHelper.SpaceState;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem.ProjectionMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.VPMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.CameraMode;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.CameraSpringArm;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.ViewMatrix;
import com.meeple.shared.frame.nuklear.NuklearManager;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;
import com.meeple.shared.frame.window.hints.HasID;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;

public class GameUI extends Screen {
	private static final Logger logger = Logger.getLogger(GameUI.class);

	static enum MouseButton implements HasID<Integer> {
		LClick(GLFW.GLFW_MOUSE_BUTTON_LEFT), RClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT), MClick(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
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
		 * Sim-city style. the difference between the press and current location. constant 
		 */
		Difference,
		/**
		 * hold and drag. not constant. mouse "locks" to world position and movement of mouse moves the world
		 */
		Drag,
		/**
		 * This is the easiest way to control with controller. same as difference but from the middle of the screen
		 */
		FromCenter
	}

	static enum CompasState {
		None,
		Visible,
		Active
	}

	//TODO move to game-options
	//---------------------settings -------------------------
	static boolean invertMouse = true;
	static boolean invertZoom = false;
	static float zoomMult = 2f;
	//aka deadzone
	static final float panRadi = 0.5f;
	static Vector4f compasColour = new Vector4f();
	static Vector4f compasLineColour = new Vector4f();

	static float menuCancelSeconds = 1f;
	static long menuDelayNanos = FrameUtils.secondsToNanos(menuCancelSeconds);
	static final float menuRadi = 0.5f;
	/*//---------------------do not change-------------------------
	Wrapper<Vector2f> mouseRClick = new WrapperImpl<>();
	Wrapper<Vector2f> mouseMClick = new WrapperImpl<>();
	Wrapper<Vector2f> mouseLastPos = new WrapperImpl<>();*/
	ClientWindow window;
	VPMatrix vpMatrix;

	ProjectionMatrix orthoProjection;

	Vector2f panningButtonPos = null;
	Vector2f rotatingButtonPos = null;
	//	Vector2f menuButtonPos = null;

	Vector2f movement = new Vector2f();

	Wrapper<Float> scale = new WrapperImpl<>();
	Wrapper<Float> pitch = new WrapperImpl<>();
	Wrapper<Float> yaw = new WrapperImpl<>();

	public CompasState panningState = CompasState.None;
	public CompasState rotatingState = CompasState.None;

	/**
	 * the mouse left click ray. updated per frame  if the left click has been pressed
	 */
	RayHelper rayHelper;
	TileTypes currentSubMenu = null;
	Object currentAction = null;
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

	private GLFWMouseButtonCallbackI mouseButtonCallback = new GLFWMouseButtonCallbackI() {

		@Override
		public void invoke(long windowID, int button, int action, int mods) {
			/*only allows when no UI element is hovered*/
			if (!Nuklear.nk_window_is_any_hovered(window.nkContext.context)) {
				if (button == panningButton.getID()) {
					if (action == GLFW.GLFW_PRESS) {
						if (panningType == PanningType.Difference) {
							Vector4f mouse = CursorHelper.getMouse(SpaceState.Eye_Space, window, orthoProjection, null);
							panningButtonPos = new Vector2f(mouse.x, mouse.y);
						} else if (panningType == PanningType.FromCenter) {
							panningButtonPos = new Vector2f(0, 0);
						}
					} else if (action == GLFW.GLFW_RELEASE) {
						panningButtonPos = null;
					}
				}
				if (button == rotateButton.getID()) {
					if (action == GLFW.GLFW_PRESS) {
						if (rotationType == PanningType.Difference) {
							Vector4f mouse = CursorHelper.getMouse(SpaceState.Eye_Space, window, orthoProjection, null);
							rotatingButtonPos = new Vector2f(mouse.x, mouse.y);
						} else if (rotationType == PanningType.FromCenter) {
							rotatingButtonPos = new Vector2f(0, 0);
						}
					} else if (action == GLFW.GLFW_RELEASE) {
						rotatingButtonPos = null;
					}
				}
				if (button == menuButton.getID()) {
					//begins tracking elsewhere
					if (action == GLFW.GLFW_PRESS) {

					} else if (action == GLFW.GLFW_RELEASE) {
						if (panningState != CompasState.Active) {
							cancelAction();

						}
					}
				}
			}
		}
	};

	private GLFWCursorPosCallback cursorposCallback = new GLFWCursorPosCallback() {

		@Override
		public void invoke(long window, double xpos, double ypos) {
			if (panningType == PanningType.Drag) {
				//basically check if it is being held down
				if (panningButtonPos != null) {
					movement.add((float) xpos, (float) ypos);
				}
			}

		}
	};
	private GLFWScrollCallbackI scrollCallback = new GLFWScrollCallbackI() {
		@Override
		public void invoke(long windowID, double xoffset, double yoffset) {
			scale.setWrapped((float) yoffset + scale.getWrappedOrDefault(0f));
		}
	};

	private void cancelAction() {
		currentAction = null;
		currentSubMenu = null;
		logger.trace("cancelling menu");
	}

	private Vector2f difference(Vector2f start, Vector2f current) {
		try {
			return new Vector2f(start).sub(current);
		} catch (NullPointerException e) {
			return null;
		}
	}

	public void init(ClientWindow window, VPMatrix vpMatrix, ProjectionMatrix orthoProj, RayHelper rayHelper) {
		this.window = window;
		this.vpMatrix = vpMatrix;
		this.orthoProjection = orthoProj;
		this.rayHelper = rayHelper;
		window.callbacks.scrollCallbackSet.add(scrollCallback);
		window.callbacks.mouseButtonCallbackSet.add(mouseButtonCallback);
		window.callbacks.cursorPosCallbackSet.add(cursorposCallback);
	}

	public void handlePitchingTick(ClientWindow window, ProjectionMatrix proj, CameraSpringArm arm) {

		Vector4f mousePos = CursorHelper.getMouse(SpaceState.Eye_Space, window, proj, null);
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
					//find dead-zone
					float len = mouseDir.length();
					mouseDir.normalize();
					if (mouseDir.isFinite()) {
						mouseDir = mouseDir.mul((len) * 0.1f);
						yaw.setWrapped(yaw.getWrappedOrDefault(0f) + mouseDir.x);
						pitch.setWrapped(pitch.getWrappedOrDefault(0f) + mouseDir.y);
						rotatingState = CompasState.Active;
					}
				}

			}

		}

		//TODO handle the keyboard press
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
				//					arm.yaw += angle;
				yaw.setWrapped(yaw.getWrappedOrDefault(0f) + angle);
			}
		}

		//handles the smooth pitch
		{
			Float s = pitch.getWrapped();
			if (s != null) {

				arm.addPitch(-s);
				process(pitch, s);
			}
		}
		//handles the smooth yaw

		{
			Float s = yaw.getWrapped();
			if (s != null) {
				arm.yaw += s;
				process(yaw, s);
			}
		}
	}

	private void process(Wrapper<Float> wrapper, float decr) {

		wrapper.setWrapped(wrapper.getWrapped() - decr);
		if (Math.abs(wrapper.getWrapped()) < 1f) {
			wrapper.setWrapped(0f);
		}
	}

	public void handleScrollingTick(CameraSpringArm arm) {

		//handle smooth scale

		Float s = scale.getWrapped();
		if (s != null) {

			float nd = (float) (s * zoomMult);
			if (invertZoom) {
				arm.addDistance(nd);
			} else {
				arm.addDistance(-nd);
			}
			//roughly 10% less each tick
			scale.setWrapped(s - (s * 0.15f));
			if (Math.abs(scale.getWrapped()) < 1f) {
				scale.setWrapped(0f);
			}

		}
	}

	public void handlePanningTick(ClientWindow window, ProjectionMatrix orthoProjection, ViewMatrix view, Entity cameraAnchor) {

		Vector4f mousePos = CursorHelper.getMouse(SpaceState.Eye_Space, window, orthoProjection, null);
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

					//find dead-zone
					float len = mouseDir.length();
					mouseDir.normalize();
					if (len < panRadi) {
						panningState = CompasState.Visible;
						len = 0;
					} else if (mouseDir.isFinite()) {
						mouseDir = mouseDir.mul((len) * 0.1f * 0.1f);
						float scale = 1f;
						//almost always
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
				//almost always
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

	public void preRenderMouseUI(ClientWindow window, ProjectionMatrix proj, ShaderProgram program) {

		GL46.glEnable(GL46.GL_DEPTH_TEST);

		if (true) {

			if (panningButtonPos != null) {
				Vector2f mouseClickedPos = new Vector2f(panningButtonPos);

				//					RenderingMain.system.loadVAO(program, mesh.mesh);
				//					mesh.mesh.visible = false;

				{
					MeshExt m = new MeshExt();

					Vector2f mouseDir = new Vector2f();
					if (true) {
						Vector3f camPos = new Vector3f();//vpMatrix.view.getWrapped().getPosition(new Vector3f());

						Vector4f v = CursorHelper.getMouse(SpaceState.Eye_Space, window, proj, null);
						Vector2f dir = new Vector2f(v.x - camPos.x, v.y - camPos.y);
						Vector2f pos = new Vector2f(mouseClickedPos);
						if (pos != null) {
							mouseDir.x = (float) (pos.x - dir.x);
							mouseDir.y = (float) (pos.y - dir.y);
						}

						//find dead-zone and max length

						float len = mouseDir.length();

						mouseDir.normalize();
						//								float len = mouseDir.normalize();
						if (len < panRadi) {
							len = 0;
						} else {
							{
								MeshExt mcompas = new MeshExt();
								Vector2f[] points = WorldRenderer.generateCircle(new Vector2f(mouseClickedPos.x, mouseClickedPos.y), panRadi, WorldRenderer.circleSegments * 2);
								WorldRenderer.setupDiscardMesh(mcompas, points.length);
								for (Vector2f circv : points) {
									mcompas.positionAttrib.data.add(circv.x);
									mcompas.positionAttrib.data.add(circv.y);
								}
								mcompas.mesh.modelRenderType = GLDrawMode.LineLoop;
								FrameUtils.appendToList(mcompas.colourAttrib.data, new Vector4f(1, 0, 1, 1));
								mcompas.mesh.name = "compas";
								mcompas.zIndexAttrib.data.add(-1f);
								RenderingMain.instance.system.loadVAO(program, mcompas.mesh);
							}
							Vector2f line = new Vector2f(mouseDir.x, mouseDir.y);
							Vector2f lineStart = line.mul(panRadi, new Vector2f());
							line = line.mul(len);
							mouseDir = mouseDir.mul((len));
							WorldRenderer.setupDiscardMesh(m, 2);

							m.positionAttrib.data.add(pos.x - lineStart.x);
							m.positionAttrib.data.add(pos.y - lineStart.y);
							m.positionAttrib.data.add(pos.x - line.x);
							m.positionAttrib.data.add(pos.y - line.y);
							m.mesh.modelRenderType = GLDrawMode.Line;
							FrameUtils.appendToList(m.colourAttrib.data, new Vector4f(1, 1, 0, 1));
							m.mesh.name = "compasLine";
							m.zIndexAttrib.data.add(-1f);
							RenderingMain.instance.system.loadVAO(program, m.mesh);
						}

					}

				}

			}
		}
	}

	int btnWidth = 100;
	int btnHeight = 50;
	int btnSpacing = 10;

	@Override
	public void render(ClientWindow window, Delta delta) {

		NkContext ctx = window.nkContext.context;
		int width = (int) (window.bounds.width * 0.75f);
		int height = (int) (window.bounds.height * 0.125f);
		int x = (int) (window.bounds.width - width) / 2;
		int y = (int) (window.bounds.height - height);

		try (MemoryStack stack = stackPush()) {
			NkRect rect = NkRect.mallocStack(stack);

			if (nk_begin(ctx, "MainBuildingMenu", nk_rect(x, y, width, height, rect), NK_WINDOW_BORDER | NK_WINDOW_NO_SCROLLBAR | NK_WINDOW_TITLE)) {

				nk_layout_row_dynamic(ctx, btnWidth + 25, 1);

				if (nk_group_begin(ctx, "", 0)) {
					nk_layout_space_begin(ctx, Nuklear.NK_STATIC, btnHeight, WorldGenerator.TileTypes.values().length);
					int i = 0;
					for (TileTypes type : WorldGenerator.TileTypes.values()) {
						nk_layout_space_push(ctx, nk_rect((btnWidth + btnSpacing) * i++, 0, btnWidth, btnHeight, NkRect.create()));
						if (currentSubMenu != null && currentSubMenu == type) {
							NuklearManager.styledButton(ctx, NuklearMenuSystem.getDisabled(ctx, stack), () -> {
								nk_button_text(ctx, type.toString());
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

		}
	}

}
