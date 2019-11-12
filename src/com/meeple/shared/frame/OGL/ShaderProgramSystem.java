package com.meeple.shared.frame.OGL;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.activation.UnsupportedDataTypeException;

import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.FileLoader;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgram.VAO;
import com.meeple.shared.frame.OGL.ShaderProgram.VBO;

public class ShaderProgramSystem {

	public static Logger logger = Logger.getLogger(ShaderProgramSystem.class);

	public static class SingleUniformSystem extends UniformManager<String, Integer> {

		@Override
		protected Integer generateID(Integer programID, String name) {
			int val = GL46.glGetUniformLocation(programID, name);
			return val;
		}
	}

	public static class MultiUniformSystem extends UniformManager<String[], Integer[]> {
		@Override
		protected Integer[] generateID(Integer programID, String[] name) {
			Integer[] vals = new Integer[name.length];
			for (int i = 0; i < name.length; i++) {
				vals[i] = GL46.glGetUniformLocation(programID, name[i]);
			}
			return vals;
		}
	}

	/**
	 * Sets up the program by<br>
	 * <ol>
	 * <li>Generating and setting program ID ({@link  GL46#glCreateProgram()})</li>
	 * <li>Compiling any shader sources ({@link #compileShaders(Map)})</li>
	 * <li>Merging any shaders IDs just compiled with the programs shader IDs ({@link #merge(Map, Map)}</li>
	 * <li>Attaching shader IDs to the program ({@link #bindShaders(int, Map)})</li>
	 * <li>Getting/setting indexes of all attributes attached to program ({@link #bindAttributeLocations(int, Map)})</li>
	 * <li>Linking the program ({@link GL46#glLinkProgram(int)})</li>
	 * <li>Validating the program {@link GL46#glValidateProgram(int)}</li>
	 * <li>Getting/setting indexes of all uniforms attached to program< ({@link #bindUniformLocations(int, Map)})</li>
	 * </ol>
	 * @param program Shader program to create/setup
	 */
	public void create(ShaderProgram program) {
		program.programID = GL46.glCreateProgram();
		logger.trace("Creating new Shader program with ID: " + program.programID);
		Map<GLShaderType, Integer> shaderIDs = compileShaders(program.shaderSources);
		program.shaderSources.clear();
		merge(shaderIDs, program.shaderIDs);
		program.shaderIDs.putAll(shaderIDs);
		bindShaders(program.programID, program.shaderIDs.values());
		//bindAttributeLocations(program.programID, program.attributes);
		GL46.glLinkProgram(program.programID);
		programErrorCheck(program.programID, GL46.GL_LINK_STATUS);
		GL46.glValidateProgram(program.programID);
		programErrorCheck(program.programID, GL46.GL_VALIDATE_STATUS);
		bindUniformLocations(program.programID, program.uniformSystems);
	}

	private void programErrorCheck(int program, int query) {
		int linkStatus = glGetProgrami(program, query);
		String programLog = glGetProgramInfoLog(program);
		if (programLog.trim().length() > 0) {

			logger.trace("program log: \r\n" + programLog);
		}
		if (linkStatus == 0) {
			throw new AssertionError();
		}
	}

	private boolean shaderCompileCheck(int shader) {
		int linkStatus = glGetShaderi(shader, GL46.GL_COMPILE_STATUS);
		return linkStatus == 0;
	}

	public void merge(Map<GLShaderType, Integer> mergeFrom, Map<GLShaderType, Integer> mergeTo) {
		Set<Entry<GLShaderType, Integer>> set = mergeFrom.entrySet();
		for (Iterator<Entry<GLShaderType, Integer>> iterator = set.iterator(); iterator.hasNext();) {
			Entry<GLShaderType, Integer> entry = iterator.next();
			GLShaderType shaderType = entry.getKey();
			Integer oldID = mergeTo.get(shaderType);
			//only assign if not existing
			if (oldID == null) {
				mergeTo.put(shaderType, entry.getValue());
			}
		}
	}

	/**
	 * Compiles all the shaders from their sources provided in the map values (keyed by shader type)
	 * @param shaderMap map of shaders to compile
	 * @return shader type - shader ID map
	 */
	public Map<GLShaderType, Integer> compileShaders(Map<GLShaderType, String> shaderMap) {
		Set<Entry<GLShaderType, String>> sources = shaderMap.entrySet();
		Map<GLShaderType, Integer> shaderIDs = Collections.synchronizedMap(new HashMap<>());
		synchronized (shaderMap) {
			Iterator<Entry<GLShaderType, String>> i = sources.iterator();
			while (i.hasNext()) {
				Entry<GLShaderType, String> entry = i.next();
				GLShaderType shaderType = entry.getKey();
				shaderIDs.put(shaderType, compileShader(entry.getValue(), shaderType.getGLID()));

			}
		}
		return shaderIDs;
	}

	/**
	 * Attatches all the Shaders (ID's stored in values) to the given program ID
	 * @param makeQuadProgram ID to bind to 
	 * @param shaderMap map of shader type-ID's to bind
	 */
	private void bindShaders(int programID, Collection<Integer> shaderMapSet) {

		synchronized (shaderMapSet) {
			Iterator<Integer> i = shaderMapSet.iterator();
			while (i.hasNext()) {
				Integer entry = i.next();
				GL46.glAttachShader(programID, entry);
			}
		}
	}

	/**
	 * Sets the correct index for all uniforms provided. <br>
	 * Internally uses {@link GL46#glGetUniformLocation(int, CharSequence)} to set Integer of entry using the entry key as the name. 
	 * @param programID
	 * @param uniformSystems
	 */
	private void bindUniformLocations(int programID, Map<UniformManager<?, ?>, Map<UniformManager<?, ?>.Uniform<?>, List<?>>> uniformSystems) {

		synchronized (uniformSystems) {
			Iterator<Entry<UniformManager<?, ?>, Map<UniformManager<?, ?>.Uniform<?>, List<?>>>> i = uniformSystems.entrySet().iterator();
			while (i.hasNext()) {
				Entry<UniformManager<?, ?>, Map<UniformManager<?, ?>.Uniform<?>, List<?>>> entry = i.next();
				UniformManager<?, ?> system = entry.getKey();
				Map<UniformManager<?, ?>.Uniform<?>, List<?>> uniforms = entry.getValue();
				system.bindUniformLocations(programID, uniforms.keySet());
			}
		}
	}

	/**
	 * Compile a single shader from source and given type, prints any errors 
	 * @param source shader source to compile
	 * @param type shader type of shader to compile
	 * @return generated ID of shader
	 */
	private int compileShader(String source, int type) {
		int shaderID = GL46.glCreateShader(type);
		GL46.glShaderSource(shaderID, source);
		GL46.glCompileShader(shaderID);

		String shaderLog = glGetShaderInfoLog(shaderID);
		if (shaderCompileCheck(shaderID)) {

			logger.trace("Shader with ID '" + shaderID + "' successfully compiled");
			if (shaderLog.trim().length() > 0) {
				logger.debug("Shader Log: \r\n" + shaderLog);
			} else {
				throw new AssertionError(shaderLog);
			}
		}
		return shaderID;
	}

	/**
	 * Load shader source from file provided
	 * @param file to be read from
	 * @return String contents of file
	 */
	@Deprecated
	public String loadShaderSourceFromFile(File file) {
		StringBuilder shaderSource = new StringBuilder();
		if (!file.exists()) {

			logger.warn("Couldnt read the shader file. (file not found)");
			logger.warn("File: " + file.getAbsolutePath());

		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				shaderSource.append(line).append("\n");
			}
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return shaderSource.toString();
	}

	/**
	 * Loads a string that represents a shader from the file system. <br>
	 * calls {@link FileLoader#loadFile(String)} to get the file resource stream then convers the file into a string by reading line by line
	 * @param name file name to load in either the packaged jar file or external file
	 * @return string of loaded shader
	 * @see FileLoader#loadFile(String) 
	 */
	public String loadShaderSourceFromFile(String name) {
		Reader stream = FileLoader.loadFile(name);
		StringBuilder shaderSource = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(stream)) {
			String line;
			while ((line = reader.readLine()) != null) {
				shaderSource.append(line).append("\n");
			}

		} catch (IOException err) {
			err.printStackTrace();
		}
		logger.trace("Loading " + name);
		return shaderSource.toString();
	}

	/**
	 * Sets up the VAO to be used by the shader program<br>
	 * generates the ID, iterates and binds all VBO data and unbinds at the end
	 * @param program to bind to
	 * @param vao to setup
	 * @see #bindBuffer(ShaderProgram, VBO)
	 */
	public void loadVAO(ShaderProgram program, VAO vao) {
		if (program.programID == 0) {
			logger.warn("Shader program has not been initialized. Do that first before loading any VAOs");
		}
		int vaoID = GL46.glGenVertexArrays();
		/*		Mesh mesh = null;
				if (vao instanceof Mesh) {
					mesh = (Mesh) vao;
				}
		
				logger.trace("Generating new VAO with ID " + vaoID + (mesh != null ? " for mesh " + mesh.name : ""));*/
		GL46.glBindVertexArray(vaoID);
		vao.VAOID = vaoID;
		Collection<VBO> vboSet = vao.VBOs;
		for (Iterator<VBO> iterator = vboSet.iterator(); iterator.hasNext();) {
			VBO vbo = iterator.next();
			bindBuffer(program, vbo);
		}
		//unbind afterwards
		GL46.glBindVertexArray(0);
		program.VAOs.add(vao);
	}

	/**
	 * generates buffer id and attaches to program. <br>
	 * also writes the buffer data into a OGL stream<br>
	 * If this is an attribute it will also bind the attribute
	 * @param program
	 * @param vbo
	 * @see #bindAttribute(int, Attribute)
	 */
	private void bindBuffer(ShaderProgram program, VBO vbo) {
		int vboID = GL46.glGenBuffers();
		GL46.glBindBuffer(vbo.bufferType.getGLID(), vboID);
		vbo.VBOID = vboID;
		writeDataToBuffer(vbo);

		if (vbo instanceof Attribute) {
			Attribute attrib = (Attribute) vbo;
			bindAttribute(program.programID, attrib);

		}
	}

	/**
	 * Writes the VBO data into the VBO buffer and then sends the buffer off to OGL
	 * @param vbo to write data to OGL 
	 */
	public void writeDataToBuffer(VBO vbo) {

		int arraySize = vbo.data.size();

		//TODO check actual buffer size vs expected
		//		logger.trace("todo: check acutal size vs expected size");
		switch (vbo.dataType) {
			case Byte:
			case UnsignedByte: {
				ByteBuffer byteBuffer = ((ByteBuffer) vbo.buffer);
				if (byteBuffer == null || byteBuffer.capacity() != arraySize) {
					vbo.buffer = byteBuffer = BufferUtils.createByteBuffer(arraySize);
				}
				for (Number b : vbo.data) {
					byteBuffer.put(b.byteValue());
				}
				byteBuffer.flip();
				GL46.glBufferData(vbo.bufferType.getGLID(), byteBuffer, vbo.bufferUsage.getGLID());

				break;
			}
			case Short:
			case UnsignedShort: {

				ShortBuffer shortBuffer = ((ShortBuffer) vbo.buffer);
				if (shortBuffer == null || shortBuffer.capacity() != arraySize) {
					vbo.buffer = shortBuffer = BufferUtils.createShortBuffer(arraySize);
				}
				for (Number b : vbo.data) {
					shortBuffer.put(b.shortValue());
				}
				shortBuffer.flip();
				GL46.glBufferData(vbo.bufferType.getGLID(), shortBuffer, vbo.bufferUsage.getGLID());
				break;
			}
			case Int:
			case UnsignedInt: {

				IntBuffer intBuffer = ((IntBuffer) vbo.buffer);
				if (intBuffer == null || intBuffer.capacity() != arraySize) {
					vbo.buffer = intBuffer = BufferUtils.createIntBuffer(arraySize);
				}
				for (Number b : vbo.data) {
					intBuffer.put(b.intValue());
				}
				intBuffer.flip();
				GL46.glBufferData(vbo.bufferType.getGLID(), intBuffer, vbo.bufferUsage.getGLID());
				break;
			}
			case HalfFloat:
			case Float: {
				FloatBuffer floatBuffer = ((FloatBuffer) vbo.buffer);
				if (floatBuffer == null || floatBuffer.capacity() != arraySize) {
					vbo.buffer = floatBuffer = BufferUtils.createFloatBuffer(arraySize);
				}
				for (Number b : vbo.data) {
					floatBuffer.put(b.floatValue());
				}
				floatBuffer.flip();
				GL46.glBufferData(vbo.bufferType.getGLID(), floatBuffer, vbo.bufferUsage.getGLID());
				break;
			}
			case Double: {
				DoubleBuffer doubleBuffer = ((DoubleBuffer) vbo.buffer);
				if (doubleBuffer == null || doubleBuffer.capacity() != arraySize) {
					vbo.buffer = doubleBuffer = BufferUtils.createDoubleBuffer(arraySize);
				}
				for (Number b : vbo.data) {
					doubleBuffer.put(b.doubleValue());
				}
				doubleBuffer.flip();
				GL46.glBufferData(vbo.bufferType.getGLID(), doubleBuffer, vbo.bufferUsage.getGLID());
				break;
			}
			case Fixed:
				System.out.println("Sorry I have no idea how to represent fixed in java... ");
				throw new RuntimeException(new UnsupportedDataTypeException());
		}
	}

	private int currentDiff(int index, int dataLength) {
		int mod = dataLength % ShaderProgram.maxAttribDataSize;
		return (index * ShaderProgram.maxAttribDataSize <= dataLength ? ShaderProgram.maxAttribDataSize : mod);
	}

	/**
	 * gets the attribute ID from the shader program<br>
	 * this WILL handle any instanced attributes and any attributes with data size over 4  
	 * calls {@link GL46#glVertexAttribPointer(int, int, int, boolean, int, long)} to setup the pointer<br>
	 * if attribute is instanced the diviser is setup with {@link GL46#glVertexAttribDivisor(int, int)}  
	 * 
	 * @param shaderProgramID
	 * @param attrib
	 */
	private void bindAttribute(int shaderProgramID, Attribute attrib) {
		attrib.index = GL46.glGetAttribLocation(shaderProgramID, attrib.name);
		if (attrib.index == -1) {
			logger.warn("[Aborting Attribute Binding] Attribute '" + attrib.name + "' could not be found in the source for program with ID " + shaderProgramID);
			return;
		}
		int id = attrib.index;
		if (attrib.dataSize > 4) {
			int index = 0;
			int runningTotal = 0;
			for (int i = 0; i < attrib.dataSize; i += ShaderProgram.maxAttribDataSize) {
				int currDiff = currentDiff(index, attrib.dataSize);
				GL46
					.glVertexAttribPointer(
						id + index,
						currDiff,
						attrib.dataType.getGLID(),
						attrib.normalised,
						attrib.dataSize * attrib.dataType.getBytes(),
						runningTotal * attrib.dataType.getBytes());
				if (attrib.instanced) {
					GL46.glVertexAttribDivisor(id + index, attrib.instanceStride);

				}
				index++;
				runningTotal += currDiff;
			}

			//			logger.trace("Attrib '" + attrib.name + "' has indexes " + id + " through " + (id + index - 1) + " with data size of " + attrib.dataSize);
		} else {

			//			logger.trace("Attrib '" + attrib.name + "' has index " + id + " with data size of " + attrib.dataSize);
			GL46.glVertexAttribPointer(id, attrib.dataSize, attrib.dataType.getGLID(), attrib.normalised, 0, 0);
			if (attrib.instanced) {
				GL46.glVertexAttribDivisor(id, attrib.instanceStride);
			}

		}
		//GL46.glBindBuffer(attrib.target, 0);
	}

	public <Name, ID> void addUniform(ShaderProgram program, UniformManager<Name, ID> system, UniformManager<Name, ID>.Uniform<?> uniform) {

		Map<UniformManager<?, ?>.Uniform<?>, List<?>> uniformList = program.uniformSystems.get(system);

		if (uniformList == null) {
			uniformList = new HashMap<>();
		}
		List<?> queue = uniformList.getOrDefault(uniform, new ArrayList<>());
		uniformList.put(uniform, queue);
		program.uniformSystems.put(system, uniformList);
	}

	@Deprecated
	public <Name, ID> void addUniformSystem(ShaderProgram program, UniformManager<Name, ID> system) {
		Map<UniformManager<?, ?>.Uniform<?>, List<?>> set = program.uniformSystems.get(system);
		if (set == null) {
			program.uniformSystems.put(system, new HashMap<>());
		}
	}

	public <Name, ID, T> void queueUniformUpload(ShaderProgram program, UniformManager<Name, ID> manager, UniformManager<Name, ID>.Uniform<T> uniform, T object) {
		Map<UniformManager<?, ?>.Uniform<?>, List<?>> uniforms = program.uniformSystems.get(manager);
		if (uniforms != null && !uniforms.isEmpty()) {
			List<?> queueBase = uniforms.getOrDefault(uniforms, new ArrayList<T>());
			try {
				List<T> queue = (List<T>) queueBase;
				if (queue != null) {
					queue = new ArrayList<>();
					uniforms.put(uniform, queue);
				}
				queue.add(object);

			} catch (Exception e) {
				System.out.println("Failed to upload the object: " + e);
				return;
			}
		}
	}

	/**
	 * Iterates all the queued uniforms for upload and uploads them. 
	 * @param program shader program to iterate all uniforms and upload
	 */
	private void uploadUniforms(ShaderProgram program) {
		synchronized (program.uniformSystems) {
			try (MemoryStack stack = stackPush()) {
				for (Iterator<Entry<UniformManager<?, ?>, Map<UniformManager<?, ?>.Uniform<?>, List<?>>>> i = program.uniformSystems.entrySet().iterator(); i.hasNext();) {
					Entry<UniformManager<?, ?>, Map<UniformManager<?, ?>.Uniform<?>, List<?>>> entry = i.next();
					UniformManager<?, ?> system = entry.getKey();
					Map<UniformManager<?, ?>.Uniform<?>, List<?>> uniforms = entry.getValue();
					system.uploadUniforms(uniforms);
				}
			}

		}
	}

	//----------------------------------------------- RENDER METHODS -----------------------------------//TODO 
	public void render(ShaderProgram program) {

		//bind shader program
		GL46.glUseProgram(program.programID);
		//upload all the queued uniform uploads
		uploadUniforms(program);
		//sync - important
		synchronized (program.VAOs) {

			for (Iterator<VAO> vaoI = program.VAOs.iterator(); vaoI.hasNext();) {
				VAO vao = vaoI.next();
				if (vao instanceof Mesh) {
					Mesh model = (Mesh) vao;
					//only render if visible
					if (model.visible) {
						GL46.glBindVertexArray(model.VAOID);
						Collection<VBO> vboSet = model.VBOs;
						for (Iterator<VBO> iterator = vboSet.iterator(); iterator.hasNext();) {
							VBO vbo = (VBO) iterator.next();
							//check if need to write to the buffer data to OGL
							boolean write = vbo.update.compareAndSet(true, false);
							if (write) {
								GL46.glBindBuffer(vbo.bufferType.getGLID(), vbo.VBOID);
								writeDataToBuffer(vbo);
								GL46.glBindBuffer(vbo.bufferType.getGLID(), 0);
							}
							//check if is attribute and if should be enabled
							if (vbo instanceof Attribute) {
								Attribute att = (Attribute) vbo;

								if (att.enabled) {
									//enables all the vertex attrib indexes
									int index = 0;
									int id = att.index;
									if (id != -1) {
										for (int i = 0; i < att.dataSize; i += ShaderProgram.maxAttribDataSize) {
											GL46.glEnableVertexAttribArray(id + index);
											index++;
										}
									}
								}
							}

						}
						//check for index buffer
						WeakReference<VBO> indexVboRef = model.index;
						VBO indexVBO = null;

						if (indexVboRef != null) {
							indexVBO = indexVboRef.get();
						}

						if (indexVBO != null) {
							//if any index buffer then bind 
							GL46.glBindBuffer(indexVBO.bufferType.getGLID(), indexVBO.VBOID);
							//render count more than one = instanced
							if (model.renderCount > 1) {
								GL46.glDrawElementsInstanced(model.modelRenderType.drawType, model.vertexCount, indexVBO.dataType.getGLID(), 0, model.renderCount);
							} else if (model.renderCount == 1) {
								GL46.glDrawElements(model.modelRenderType.drawType, model.vertexCount, indexVBO.dataType.getGLID(), 0);
							}
						} else {
							//render count more than one = instanced
							if (model.renderCount > 1) {
								GL46.glDrawArraysInstanced(model.modelRenderType.drawType, 0, model.vertexCount, model.renderCount);
							} else if (model.renderCount == 1) {
								GL46.glDrawArrays(model.modelRenderType.drawType, 0, model.vertexCount);
							}

						}
						for (Iterator<VBO> iterator = vboSet.iterator(); iterator.hasNext();) {
							VBO vbo = (VBO) iterator.next();
							if (vbo instanceof Attribute) {
								Attribute att = (Attribute) vbo;
								//disable all vertex attributes
								int index = 0;
								int id = att.index;
								if (id != -1) {
									for (int i = 0; i < att.dataSize; i += ShaderProgram.maxAttribDataSize) {
										GL46.glDisableVertexAttribArray(id + index);
										index++;
									}
								}
							}

						}
						//unbind vertex array
						GL46.glBindVertexArray(0);
					}
					//only discard if actually rendered
					if (vao.singleFrameDiscard) {
						deleteModel(vao);
						vaoI.remove();
					}
				}
			}

		}
		//unbind shader program
		GL46.glUseProgram(0);

	}

	//----------------------------------------------- CLOSE METHODS -----------------------------------//TODO 

	public void deleteModel(VAO model) {
		if (model != null) {
			GL46.glDeleteVertexArrays(model.VAOID);
			Collection<VBO> vbos = model.VBOs;
			for (Iterator<VBO> iterator2 = vbos.iterator(); iterator2.hasNext();) {
				VBO vbo = (VBO) iterator2.next();
				GL46.glDeleteBuffers(vbo.VBOID);
				iterator2.remove();
			}
			model.VAOID = ShaderProgram.NULL;
		} else {
			logger.warn("Could not delete model, parameter was null", new NullPointerException());
		}
	}

	/**
	 * Detaches and deletes all shader's for this program, then deletes the program from memory 
	 * @param program program to detach and delete
	 */
	public void detatchAndDeleteShaders(ShaderProgram program) {

		GL46.glUseProgram(0);
		Set<Entry<GLShaderType, Integer>> set = program.shaderIDs.entrySet();
		synchronized (set) {
			Iterator<Entry<GLShaderType, Integer>> i = set.iterator();
			while (i.hasNext()) {
				Entry<GLShaderType, Integer> entry = i.next();
				int shaderID = entry.getValue();
				GL46.glDetachShader(program.programID, shaderID);
				GL46.glDeleteShader(shaderID);
				i.remove();
			}
		}

		GL46.glDeleteProgram(program.programID);
	}

	public void close(ShaderProgram program) {
		detatchAndDeleteShaders(program);
	}

}
