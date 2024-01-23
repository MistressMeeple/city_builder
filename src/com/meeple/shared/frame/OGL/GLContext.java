package com.meeple.shared.frame.OGL;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

import java.util.Iterator;
import java.util.Set;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46;
import org.lwjgl.opengl.GLCapabilities;

import com.meeple.shared.utils.CollectionSuppliers.SetSupplier;

public class GLContext implements AutoCloseable {
	public GLCapabilities capabilities;
	public Set<Integer> vertexArrays = new SetSupplier<Integer>().get();
	public Set<Integer> buffers = new SetSupplier<Integer>().get();

	public Set<Integer> programs = new SetSupplier<Integer>().get();
	public Set<Integer> shaders = new SetSupplier<Integer>().get();

	public GLContext() {
	}

	public void init() {
		capabilities = GL.createCapabilities();
	}

	/**
	 * Generates a new UBO. sets its max size and binds it to the binding point <br>
	 * <b>Binding point needs to be the same as when passed to {@link #bindUBONameToIndex(String, int, ShaderProgram...)}</B>
	 * @param bindingPoint index to bind to. 
	 * @param maxSize max size of the buffer in bytes. generally struct size * max instances
	 * @return new UBO ID
	 */
	public int genUBO(int bindingPoint, long maxSize) {
		int buffer = genBuffer();
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
		glBufferData(
			GL_UNIFORM_BUFFER,
			maxSize,
			GL_DYNAMIC_DRAW);

		//binds the buffer to a binding index
		glBindBufferBase(GL_UNIFORM_BUFFER, bindingPoint, buffer);
		return buffer;
	}

	/**
	 * Binds all shader programs to a binding index. <br> 
	 * <b>Binding point needs to be the same as when passed to {@link #genUBO(int, long)}</B>
	 * @param name
	 * @param bindingPoint
	 * @param programs
	 */
	public void bindUBONameToIndex(String name, int bindingPoint, ShaderProgram... programs) {
		for (ShaderProgram program : programs) {
			int actualIndex = GL46.glGetUniformBlockIndex(program.programID, name);
			//binds the binding index to the interface block (by index)
			glUniformBlockBinding(program.programID, actualIndex, bindingPoint);
		}
	}

	@Override
	public void close() {
		deleteAllBuffers();
		deleteAllPrograms();
		deleteAllShaders();
		deleteAllArrays();
	}

	public int genBuffer() {
		int bufferID = GL46.glGenBuffers();
		buffers.add(bufferID);
		return bufferID;
	}

	public void deleteBuffer(int buffer) {
		buffers.remove(buffer);
		GL46.glDeleteBuffers(buffer);
	}

	public void deleteAllBuffers() {
		synchronized (buffers) {
			for (Iterator<Integer> i = buffers.iterator(); i.hasNext();) {
				Integer buffer = i.next();
				GL46.glDeleteBuffers(buffer);
				i.remove();
			}
		}
	}

	public int genVertexArray() {

		int vertexArray = GL46.glGenVertexArrays();
		vertexArrays.add(vertexArray);
		return vertexArray;
	}

	public void deleteVertexArray(int vertexArray) {
		vertexArrays.remove(vertexArray);
		GL46.glDeleteVertexArrays(vertexArray);
	}

	public void deleteAllArrays() {
		synchronized (vertexArrays) {
			for (Iterator<Integer> i = vertexArrays.iterator(); i.hasNext();) {
				Integer vertexArray = i.next();
				GL46.glDeleteVertexArrays(vertexArray);
				i.remove();
			}
		}
	}

	public int genShader(ShaderProgram.GLShaderType type, String source) {
		int id = ShaderProgramSystem.compileShader(source, type.getGLID());
		shaders.add(id);
		return id;
	}

	public void deleteShader(int id) {
		shaders.remove(id);
		GL46.glDeleteShader(id);
	}

	public void deleteAllShaders() {
		synchronized (shaders) {
			for (Iterator<Integer> i = shaders.iterator(); i.hasNext();) {
				Integer shader = i.next();
				GL46.glDeleteShader(shader);
				i.remove();
			}
		}
	}

	public int genProgram() {
		int id = GL46.glCreateProgram();
		programs.add(id);
		return id;
	}

	public void deleteProgram(int program) {
		programs.remove(program);
		GL46.glDeleteProgram(program);
	}

	public void deleteAllPrograms() {
		synchronized (programs) {
			for (Iterator<Integer> i = programs.iterator(); i.hasNext();) {
				Integer program = i.next();
				GL46.glDeleteProgram(program);
				i.remove();
			}
		}
	}
}
