package com.meeple.shared.frame;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL30C.*;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Set;

import org.lwjgl.opengl.GL46;

public class OpenGLSettingPop implements Closeable {

	private final Set<Integer> enabled;
	private final Set<Integer> disabled;

	public OpenGLSettingPop() {
		enabled = new HashSet<>();
		disabled = new HashSet<>();
	}

	public void enable(int GLID) {
		GL46.glEnable(GLID);
		enabled.add(GLID);
	}

	public void disable(int GLID) {
		GL46.glDisable(GLID);
		disabled.add(GLID);
	}

	@Override
	public void close() {
		for (Integer e : enabled) {
			GL46.glDisable(e);
		}
		for (Integer d : disabled) {
			GL46.glEnable(d);
		}
		// default OpenGL state
		glUseProgram(0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
		glDisable(GL_BLEND);
		glDisable(GL_SCISSOR_TEST);
	}

}
