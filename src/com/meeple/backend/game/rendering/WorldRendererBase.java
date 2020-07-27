package com.meeple.backend.game.rendering;

import com.meeple.backend.view.VPMatrix;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.AttributeFactory;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;

public abstract class WorldRendererBase {

	private class MeshBuilder {
		
		private Mesh build() {
			return null;
		}
	}

	private ShaderProgram program;
	private AttributeFactory[] attributes;

	public void setup(GLContext glContext) {
		setupProgram(glContext);
	}

	public void bindVPMatrix(VPMatrix vpMatrix) {
		VPMatrix.bindToProgram(program.programID, vpMatrix.getBindingPoint());
	}

	private void setupProgram(GLContext glContext) {
		program = new ShaderProgram();
		initProgram(glContext, program);
		//setup the program
		try {
			ShaderProgramSystem2.create(glContext, program);
		} catch (Exception err) {
			err.printStackTrace();

		}
	}

	protected abstract void initProgram(GLContext glContext, ShaderProgram program);
}
