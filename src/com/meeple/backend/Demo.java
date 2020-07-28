package com.meeple.backend;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL46;

import com.meeple.backend.ShaderPrograms.Program;
import com.meeple.backend.view.VPMatrix;
import com.meeple.backend.view.VPMatrix.CameraKey;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2.ShaderClosable;

public class Demo extends Client {

	public static void main(String[] args) {

		try (Client d = new Demo()) {
			d.setup(1400, 800, "Simple LWJGL Demo");
			d.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private float viewRotation;

	VPMatrix vpMatrix = new VPMatrix();
	CameraKey primary = vpMatrix.newCamera();

	private Vector3f viewPosition = new Vector3f();
	private float viewHeight = 17f;
	private float range = 17f;
	private float fov = 60;

	private Mesh axisMesh;

	@Override
	protected void setupGL() {

		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		ShaderPrograms.initAndCreate(glContext, Program._3D_Unlit_Flat);
		setupUBOs(Program._3D_Unlit_Flat.program);

		axisMesh = drawAxis(100);
		ShaderProgramSystem2.loadVAO(glContext, Program._3D_Unlit_Flat.program, axisMesh);

	}

	@Override
	protected void render(FrameTimings delta) {
		cameraTick(delta);
		vpMatrix.upload();
		glClearColor(0.1f, 0.1f, 0.1f, 0.1f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		//sets the drawing space to be the whole window
		GL46.glViewport(0, 0, windowWidth, windowHeight);
		try (ShaderClosable sc = ShaderProgramSystem2.useProgram(Program._3D_Unlit_Flat.program)) {

			ShaderProgramSystem2.tryFullRenderMesh(axisMesh);
		}

	}

	private void cameraTick(FrameTimings timing) {

		//increment the rotation of the camera
		viewRotation += (float) (timing.deltaSeconds * 0.125f * (float) Math.PI);
		//keep it between 0 and 2pi
		viewRotation = (float) (viewRotation % (2d * Math.PI));

		vpMatrix.setPerspective(fov, (float) windowWidth / windowHeight, 0.01f, 100.0f);

		//setting the view position defined by the rotation previously set and a radius
		viewPosition.set(range * (float) Math.cos(-viewRotation), range * (float) Math.sin(-viewRotation), viewHeight);
		//setting the view matrix to look at 000 from view position
		vpMatrix
			.getCamera(primary)
			.setLookAt(
				viewPosition.x,
				viewPosition.y,
				viewPosition.z,
				0f,
				0f,
				range * 0.25f,
				0f,
				0f,
				1f);

		//		System.out.println(viewHeight + " "+ range);
	}

	private void setupUBOs(ShaderProgram program) {

		//		vpMatrix.setBindingPoint(VPMatrix.Default_VP_Matrix_Binding_Point);
		vpMatrix.setupBuffer(glContext);
		VPMatrix.bindToProgram(program.programID, vpMatrix.getBindingPoint());

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

		Mesh x = new Mesh()
			.addAttribute(
				ShaderPrograms.vertAtt
					.build()
					.data(verts))
			.addAttribute(
				ShaderPrograms.colourAtt
					.build()
					.data(colours))
			.addAttribute(
				ShaderPrograms.transformAtt
					.build()
					.data(
						new Matrix4f()
							.get(new float[16])))
			.vertexCount(count * 2)
			.renderMode(GLDrawMode.Line)
			.name("axis");

		return x;
	}
}
