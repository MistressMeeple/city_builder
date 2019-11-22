package com.meeple.citybuild.client.input;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL46;

import com.meeple.citybuild.client.render.RenderingMain;
import com.meeple.citybuild.client.render.WorldRenderer;
import com.meeple.citybuild.client.render.WorldRenderer.MeshExt;
import com.meeple.citybuild.server.Entity;
import com.meeple.shared.Tickable;
import com.meeple.shared.frame.CursorHelper;
import com.meeple.shared.frame.CursorHelper.SpaceState;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem.ProjectionMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.CameraMode;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.CameraSpringArm;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.ViewMatrix;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;

public class CameraControlHandler {

	static boolean invertMouse = true;
	static boolean invertZoom = false;
	static float zoomMult = 2f;
	//aka deadzone
	static final float panRadi = 0.5f;
	static Vector4f compasColour = new Vector4f();
	static Vector4f compasLineColour = new Vector4f();

	//---------------------do not change-------------------------
	Wrapper<Vector2f> mouseRClick = new WrapperImpl<>();
	Wrapper<Vector2f> mouseMClick = new WrapperImpl<>();
	Wrapper<Vector2f> mouseLastPos = new WrapperImpl<>();

	Wrapper<Float> scale = new WrapperImpl<>();
	Wrapper<Float> pitch = new WrapperImpl<>();
	Wrapper<Float> yaw = new WrapperImpl<>();

	public CompasState panningState = CompasState.None;
	public CompasState pitchingState = CompasState.None;

	static enum CompasState {
		None,
		Visible,
		Active
	}

	private boolean inDeadzone() {
		return true;
	}

	public Tickable handlePitchingTick(ClientWindow window, ProjectionMatrix proj, CameraSpringArm arm) {
		Wrapper<Float> scale = new WrapperImpl<>();
		Wrapper<Float> pitch = new WrapperImpl<>();
		Wrapper<Float> yaw = new WrapperImpl<>();
		window.callbacks.scrollCallbackSet
			.add(
				(windowID, xpos, ypos) -> {
					scale.setWrapped((float) ypos + scale.getWrappedOrDefault(0f));
				});

		return (delta) -> {
			Vector4f mousePos = CursorHelper.getMouse(SpaceState.Eye_Space, window, proj, null);
			Vector2f dir = new Vector2f(mousePos.x, mousePos.y);
			{
				long mTicks = window.mousePressTicks.getOrDefault(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, 0l);
				if (mTicks < 5) {
					mouseMClick.setWrapped(dir);
					pitchingState = CompasState.None;
				} else if (mTicks > 5) {

					Vector2f mouseDir = new Vector2f();
					Vector2f pos = mouseMClick.getWrapped();
					if (pos != null) {
						mouseDir.x = (float) (pos.x - dir.x);
						mouseDir.y = (float) (pos.y - dir.y);
						if (invertMouse) {
							mouseDir.x = -mouseDir.x;
							mouseDir.y = -mouseDir.y;
						}
					}
					//find dead-zone
					float len = mouseDir.length();
					mouseDir.normalize();
					if (len < panRadi) {
						pitchingState = CompasState.Visible;
						len = 0;
					} else if (mouseDir.isFinite()) {
						mouseDir = mouseDir.mul((len) * 0.1f);
						//						arm.addPitch(-mouseDir.y);
						//						arm.yaw += mouseDir.x;
						//					((Entity) view.anchor.anchorObject).rotation.add(-mouseDir.y * 0.1f, mouseDir.x * 0.1f, 0);
						yaw.setWrapped(yaw.getWrappedOrDefault(0f) + mouseDir.x);
						pitch.setWrapped(pitch.getWrappedOrDefault(0f) + mouseDir.y);
						pitchingState = CompasState.Active;
					}

				}

			}

			if (pitchingState == CompasState.None) {
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
			{
				Float s = scale.getWrapped();
				if (s != null) {

					float nd = (float) (s * zoomMult);
					if (invertZoom) {
						arm.addDistance(nd);
					} else {
						arm.addDistance(-nd);
					}
					scale.setWrapped(s / 1.1f);
					if (Math.abs(scale.getWrapped()) < 1f) {
						scale.setWrapped(0f);
					}
				}
			}

			{
				Float s = pitch.getWrapped();
				if (s != null) {

					arm.addPitch(-s);
					process(pitch, 1.5f);
				}
			}

			{
				Float s = yaw.getWrapped();
				if (s != null) {
					arm.yaw += s;
					process(yaw, 1.5f);
				}
			}
			return false;
		};
	}

	private void process(Wrapper<Float> wrapper, float decr) {

		wrapper.setWrapped(wrapper.getWrapped() / decr);
		if (Math.abs(wrapper.getWrapped()) < 1f) {
			wrapper.setWrapped(0f);
		}
	}

	public void handlePanningTick(ClientWindow window, ProjectionMatrix proj, ViewMatrix view, Entity cameraAnchor) {

		Vector4f mousePos = CursorHelper.getMouse(SpaceState.Eye_Space, window, proj, null);
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

			if (rTicks < 5) {
				mouseRClick.setWrapped(dir);
				panningState = CompasState.None;
			} else if (rTicks > 5) {
				Vector3f mouseDir = new Vector3f();
				Vector2f pos = mouseRClick.getWrapped();
				if (pos != null) {
					mouseDir.x = (float) (pos.x - dir.x);
					mouseDir.y = (float) (pos.y - dir.y);
					if (invertMouse) {
						mouseDir.x = -mouseDir.x;
						mouseDir.y = -mouseDir.y;
					}
				}
				mouseDir = mouseDir.rotateZ(-(float) Math.toRadians(rotZ));
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

			if (window.mousePressTicks.getOrDefault(GLFW.GLFW_MOUSE_BUTTON_RIGHT, 0l) > 0) {
				Vector2f mouseClickedPos = mouseRClick.getWrapped();

				//					RenderingMain.system.loadVAO(program, mesh.mesh);
				//					mesh.mesh.visible = false;

				if (mouseClickedPos != null) {
					{
						MeshExt m = new MeshExt();
						Vector2f[] points = WorldRenderer.generateCircle(new Vector2f(mouseClickedPos.x, mouseClickedPos.y), panRadi, WorldRenderer.circleSegments * 2);
						WorldRenderer.setupDiscardMesh(m, points.length);
						for (Vector2f v : points) {
							m.positionAttrib.data.add(v.x);
							m.positionAttrib.data.add(v.y);
						}
						m.mesh.modelRenderType = GLDrawMode.LineLoop;
						FrameUtils.appendToList(m.colourAttrib.data, new Vector4f(1, 0, 1, 1));
						m.mesh.name = "compas";
						m.zIndexAttrib.data.add(-1f);
						RenderingMain.instance.system.loadVAO(program, m.mesh);
						/*	mesh.offsetAttrib.data.clear();
							mesh.offsetAttrib.data.add(mouseClickedPos.x);
							mesh.offsetAttrib.data.add(mouseClickedPos.y);
							mesh.offsetAttrib.update.set(true);
							mesh.mesh.visible = true;*/
					}

					{
						MeshExt m = new MeshExt();

						Vector2f mouseDir = new Vector2f();
						if (true) {
							Vector3f camPos = new Vector3f();//vpMatrix.view.getWrapped().getPosition(new Vector3f());

							Vector4f v = CursorHelper.getMouse(SpaceState.Eye_Space, window, proj, null);
							Vector2f dir = new Vector2f(v.x - camPos.x, v.y - camPos.y);
							Vector2f pos = new Vector2f(mouseRClick.getWrapped().x, mouseRClick.getWrapped().y);
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
	}
}
