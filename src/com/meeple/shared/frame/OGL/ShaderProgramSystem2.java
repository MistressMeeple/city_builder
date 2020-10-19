package com.meeple.shared.frame.OGL;

import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL46;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import com.meeple.backend.GLHelper;
import com.meeple.backend.GLHelper.GLSLAttribute;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.FileLoader;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLStatus;
import com.meeple.shared.frame.OGL.ShaderProgram.GLTexture;
import com.meeple.shared.frame.OGL.ShaderProgram.GLTextureType;
import com.meeple.shared.frame.OGL.ShaderProgram.IndexBufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgram.VAO;
import com.meeple.shared.frame.OGL.ShaderProgram.VertexAttribute;
import com.meeple.shared.frame.nuklear.IOUtil;

public class ShaderProgramSystem2 {

	private static Logger logger = Logger.getLogger(ShaderProgramSystem2.class);

	/**
	 * 
	 * Sets up the program by<br>
	 * <ol>
	 * <li>Generating and setting program ID ({@link GL46#glCreateProgram()})</li>
	 * <li>Compiling any shader sources ({@link #compileShaders(Map)})</li>
	 * <li>Merging any shaders IDs just compiled with the programs shader IDs
	 * ({@link #merge(Map, Map)}</li>
	 * <li>Attaching shader IDs to the program ({@link #bindShaders(int, Map)})</li>
	 * <li>Getting/setting indexes of all attributes attached to program
	 * ({@link #bindAttributeLocations(int, Map)})</li>
	 * <li>Linking the program ({@link GL46#glLinkProgram(int)})</li>
	 * <li>Validating the program {@link GL46#glValidateProgram(int)}</li>
	 * <li>Retrieving all active uniforms used by the program {@link #getUniformLocations(ShaderProgram)}</li>
	 * </ol>
	 * <br>
	 * Also stores the generated ID's into the GLContext to be easily cleaned up
	 * later.
	 * 
	 * @param glc OpenGL Context to store the generated ID's for it is easy to clean up later
	 * @param program Shader program to create/setup
	 * 
	 * @throws Exception when the program fails error checks. if thrown auto closes
	 *                   and deletes resources
	 */
	public static void create(GLContext glc, ShaderProgram program) {
		if (program.programID != 0) {
			logger.trace("Program has likely already been setup.");
			return;
		}
		boolean failure = false;
		String log = "";
		Exception err = null;
		try {
			program.programID = GL46.glCreateProgram();
			glc.programs.add(program.programID);
			logger.trace("Creating new Shader program with ID: " + program.programID);

			Map<GLShaderType, Integer> shaderIDs = compileShaders(glc, program.shaderSources);
			program.shaderSources.clear();
			merge(shaderIDs, program.shaderIDs);
			program.shaderIDs.putAll(shaderIDs);
			bindShaders(glc, program.programID, program.shaderIDs.values());
			// bindAttributeLocations(program.programID, program.attributes);

			GL46.glLinkProgram(program.programID);
			log = programStatusCheck(program.programID, GLStatus.LinkStatus);

			if (log.length() > 0) {
				logger.trace("program log: \r\n" + log);
			}
			GL46.glValidateProgram(program.programID);
			log = programStatusCheck(program.programID, GLStatus.ValidateStatus);
			if (log.length() > 0) {
				logger.trace("program log: \r\n" + log);
			}
			generateProgramAttributes(program);
			getUniformLocations(program);

		} catch (Exception e) {
			failure = true;
			err = e;
		}
		if (failure) {
			GL46.glUseProgram(0);
			Set<Entry<GLShaderType, Integer>> set = program.shaderIDs.entrySet();
			synchronized (set) {
				Iterator<Entry<GLShaderType, Integer>> i = set.iterator();
				while (i.hasNext()) {
					Entry<GLShaderType, Integer> entry = i.next();
					int shaderID = entry.getValue();
					GL46.glDetachShader(program.programID, shaderID);
					glc.deleteShader(shaderID);
					i.remove();
				}
			}

			glc.deleteProgram(program.programID);
			logger.fatal(err);

			throw new AssertionError(log, err);
		}
	}

	public static String programStatusCheck(int program, GLStatus status) throws Exception {
		int linkStatus = glGetProgrami(program, status.getGLID());
		String programLog = glGetProgramInfoLog(program);
		if (linkStatus == 0) {
			throw new Exception("Program failed the status check: " + status.name() + "\r\n" + programLog);
		}
		return programLog;
	}

	private static String shaderCompileCheck(int shaderID) throws Exception {
		int linkStatus = glGetShaderi(shaderID, GLStatus.CompileStatus.getGLID());
		String shaderLog = glGetShaderInfoLog(shaderID);
		if (linkStatus == 0) {
			throw new Exception("Shader failed to compile." + "\r\n" + shaderLog);
		}
		return shaderLog;
	}

	/**
	 * Compile a single shader from source and given type, prints any errors
	 * 
	 * @param source shader source to compile
	 * @param type   shader type of shader to compile
	 * @return generated ID of shader
	 * @throws Exception
	 */
	public static int compileShader(GLContext glc, String source, int type) throws Exception {
		int shaderID = glc.genShader(type);

		GL46.glShaderSource(shaderID, source);
		GL46.glCompileShader(shaderID);
		String shaderLog = "";
		shaderCompileCheck(shaderID);
		logger.trace("Shader with ID '" + shaderID + "' successfully compiled");
		if (shaderLog.trim().length() > 0) {
			logger.debug("Shader Log: \r\n" + shaderLog);
		}
		return shaderID;
	}

	public static void merge(Map<GLShaderType, Integer> mergeFrom, Map<GLShaderType, Integer> mergeTo) {
		Set<Entry<GLShaderType, Integer>> set = mergeFrom.entrySet();
		for (Iterator<Entry<GLShaderType, Integer>> iterator = set.iterator(); iterator.hasNext();) {
			Entry<GLShaderType, Integer> entry = iterator.next();
			GLShaderType shaderType = entry.getKey();
			Integer oldID = mergeTo.get(shaderType);
			// only assign if not existing
			if (oldID == null) {
				mergeTo.put(shaderType, entry.getValue());
			}
		}
	}

	/**
	 * Compiles all the shaders from their sources provided in the map values (keyed
	 * by shader type)
	 * 
	 * @param shaderMap map of shaders to compile
	 * @return shader type - shader ID map
	 * @throws Exception
	 */
	public static Map<GLShaderType, Integer> compileShaders(GLContext glc, Map<GLShaderType, String> shaderMap) throws Exception {
		Set<Entry<GLShaderType, String>> sources = shaderMap.entrySet();
		Map<GLShaderType, Integer> shaderIDs = new CollectionSuppliers.MapSupplier<GLShaderType, Integer>().get();
		synchronized (shaderMap) {
			Iterator<Entry<GLShaderType, String>> i = sources.iterator();
			while (i.hasNext()) {
				Entry<GLShaderType, String> entry = i.next();
				GLShaderType shaderType = entry.getKey();
				shaderIDs.put(shaderType, compileShader(glc, entry.getValue(), shaderType.getGLID()));

			}
		}
		return shaderIDs;
	}

	/**
	 * Attaches all the Shaders (ID's stored in values) to the given program ID
	 * 
	 * @param makeQuadProgram ID to bind to
	 * @param shaderMap       map of shader type-ID's to bind
	 */
	public static void bindShaders(GLContext glc, int programID, Collection<Integer> shaderMapSet) {

		synchronized (shaderMapSet) {
			Iterator<Integer> i = shaderMapSet.iterator();
			while (i.hasNext()) {
				Integer entry = i.next();
				GL46.glAttachShader(programID, entry);
				glc.deleteShader(entry);
			}
		}
	}

	private static void generateProgramAttributes(ShaderProgram program) {

		int activeAtts = GL46.glGetProgrami(program.programID, GL46.GL_ACTIVE_ATTRIBUTES);
		for (int i = 0; i < activeAtts; i++) {

			GLSLAttribute att = GLHelper.glGetActiveAttrib(program, i);
			program.atts.put(att.name, att);
		}

	}

	/**
	 * Gets all active uniforms in the program and stores then in the {@link ShaderProgram#shaderUniforms} variable by name to index.
	 * @param program
	 */
	private static void getUniformLocations(ShaderProgram program) {
		program.shaderUniforms.clear();
		int activeUniforms = GL46.glGetProgrami(program.programID, GL46.GL_ACTIVE_UNIFORMS);
		for (int i = 0; i < activeUniforms; i++) {
			String name = GL46.glGetActiveUniformName(program.programID, i);
			program.shaderUniforms.put(name, i);
		}
	}

	/**
	 * Loads a string that represents a shader from the file system. <br>
	 * calls {@link FileLoader#loadFile(String)} to get the file resource stream
	 * then convers the file into a string by reading line by line
	 * 
	 * @param name file name to load in either the packaged jar file or external
	 *             file
	 * @return string of loaded shader
	 * @see FileLoader#loadFile(String)
	 */
	public static String loadShaderSourceFromFile(String name) {
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
	 * 
	 * @param program to bind to
	 * @param vao     to setup
	 * @see #bindBuffer(ShaderProgram, BufferObject)
	 */
	public static void loadVAO(GLContext glc, ShaderProgram program, VAO vao) {
		if (program.programID == 0) {
			logger.warn("Shader program has not been initialized. Do that first before loading any VAOs");
		}
		int vaoID = glc.genVertexArray();
		/*
		 * Mesh mesh = null; if (vao instanceof Mesh) { mesh = (Mesh) vao; }
		 * 
		 * logger.trace("Generating new VAO with ID " + vaoID + (mesh != null ?
		 * " for mesh " + mesh.name : ""));
		 */
		GL46.glBindVertexArray(vaoID);
		vao.VAOID = vaoID;
		Collection<BufferObject> vboSet = vao.VBOs;
		for (Iterator<BufferObject> iterator = vboSet.iterator(); iterator.hasNext();) {
			BufferObject vbo = iterator.next();
			bindBuffer(glc, program, vbo);
		}

		for (GLSLAttribute glAtt : program.atts.values()) {
			enableAttribute(glAtt);
		}

		// unbind afterwards
		GL46.glBindVertexArray(0);
		program.VAOs.add(vao);
	}

	private static void enableAttribute(GLSLAttribute attribute) {
		// enables all the vertex attrib indexes
		int index = 0;
		int id = attribute.index;
		if (id != -1) {
			for (int i = 0; i < attribute.type.getSize() * attribute.arraySize; i += ShaderProgram.maxAttribDataSize) {
				GL46.glEnableVertexAttribArray(id + index);
				index++;
			}

		}
	}

	/**
	 * Deletes the specified VAO from GL memory and shader program
	 * 
	 * @param glc     context to delete from
	 * @param program shader program that it was bound to
	 * @param vao     VAO to delete
	 */
	public static void deleteVAO(GLContext glc, ShaderProgram program, VAO vao) {
		synchronized (vao.VBOs) {
			for (Iterator<BufferObject> i = vao.VBOs.iterator(); i.hasNext();) {
				BufferObject buffer = i.next();
				glc.deleteBuffer(buffer.VBOID);
				i.remove();

			}
		}
		glc.deleteVertexArray(vao.VAOID);
		program.VAOs.remove(vao);
	}

	/**
	 * generates buffer id and attaches to program. <br>
	 * also writes the buffer data into a OGL stream<br>
	 * If this is an attribute it will also bind the attribute
	 * 
	 * @param program
	 * @param vbo
	 * @see #bindAttribute(int, VertexAttribute)
	 */
	private static void bindBuffer(GLContext glc, ShaderProgram program, BufferObject vbo) {
		if (vbo.VBOID == 0) {
			int vboID = glc.genBuffer();
			vbo.VBOID = vboID;
		}
		GL46.glBindBuffer(vbo.bufferType.getGLID(), vbo.VBOID);
		writeDataToBuffer(vbo);

		if (vbo instanceof VertexAttribute) {
			VertexAttribute attrib = (VertexAttribute) vbo;
			GLSLAttribute att = program.atts.get(attrib.name);
			try {
				GLHelper.setupAttrib(program.programID, att, attrib.instanced ? attrib.instanceStride : 0);
			} catch (Exception e) {
				logger.warn("Mesh attribute was null: " + attrib.name);
			}

		}

	}

	/**
	 * Writes the VBO data into the VBO buffer and then sends the buffer off to OGL
	 * 
	 * @param vbo to write data to OGL
	 */
	public static void writeDataToBuffer(BufferObject vbo) {

		switch (vbo.bufferResourceType) {
		case Address:
			GL46.nglBufferData(vbo.bufferType.getGLID(), vbo.bufferLen, vbo.bufferAddress, vbo.bufferUsage.getGLID());
			break;
		case List:

			int arraySize = vbo.data.size();

			// TODO check actual buffer size vs expected
			// logger.trace("todo: check acutal size vs expected size");
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
				break;
			}
			case Fixed: {
				System.out.println("Sorry I have no idea how to represent fixed in java... ");
				throw new RuntimeException(new UnsupportedOperationException());
			}
			}

		case Buffer:

			if (vbo.buffer.position() != 0) {
				vbo.buffer.flip();
			}

			switch (vbo.dataType) {
			case Byte:
			case UnsignedByte: {
				ByteBuffer byteBuffer = ((ByteBuffer) vbo.buffer);
				GL46.glBufferData(vbo.bufferType.getGLID(), byteBuffer, vbo.bufferUsage.getGLID());

				break;
			}
			case Short:
			case UnsignedShort: {

				ShortBuffer shortBuffer = ((ShortBuffer) vbo.buffer);
				GL46.glBufferData(vbo.bufferType.getGLID(), shortBuffer, vbo.bufferUsage.getGLID());
				break;
			}
			case Int:
			case UnsignedInt: {

				IntBuffer intBuffer = ((IntBuffer) vbo.buffer);
				GL46.glBufferData(vbo.bufferType.getGLID(), intBuffer, vbo.bufferUsage.getGLID());
				break;
			}
			case HalfFloat:
			case Float: {
				FloatBuffer floatBuffer = ((FloatBuffer) vbo.buffer);
				GL46.glBufferData(vbo.bufferType.getGLID(), floatBuffer, vbo.bufferUsage.getGLID());
				break;
			}
			case Double: {
				DoubleBuffer doubleBuffer = ((DoubleBuffer) vbo.buffer);
				GL46.glBufferData(vbo.bufferType.getGLID(), doubleBuffer, vbo.bufferUsage.getGLID());
				break;
			}
			case Fixed: {
				System.out.println("Sorry I have no idea how to represent fixed in java... ");
				throw new RuntimeException(new UnsupportedOperationException());
			}
			default: {
				break;

			}
			}
			break;
		/*
		 * case Empty: GL46.glBufferData(vbo.bufferType.getGLID(), vbo.bufferLen *
		 * vbo.dataType.getBytes() * vbo.dataSize, vbo.bufferUsage.getGLID()); break;
		 */
		case Manual:
			logger.warn("manual management of vbo data. ");
			break;
		default:
			break;
		}
	}

	public static GLTexture loadTexture(GLContext glContext, String imagePath) {
		GLTexture texture = new GLTexture();
		ByteBuffer imageData = null;
		int textureID = 0;
		try {
			imageData = IOUtil.ioResourceToByteBuffer(imagePath, 1024);
			try (MemoryStack stack = stackPush()) {
				IntBuffer w = stack.mallocInt(1);
				IntBuffer h = stack.mallocInt(1);
				IntBuffer components = stack.mallocInt(1);

				// Decode texture image into a byte buffer
				ByteBuffer decodedImage = STBImage.stbi_load_from_memory(imageData, w, h, components, 4);

				int width = w.get();
				int height = h.get();

				// Create a new OpenGL texture 
				textureID = glContext.genTextures();

				// Bind the texture
				GL46.glBindTexture(GL46.GL_TEXTURE_2D, textureID);

				// Tell OpenGL how to unpack the RGBA bytes. Each component is 1 byte size
				GL46.glPixelStorei(GL46.GL_UNPACK_ALIGNMENT, 1);
				GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_LINEAR);
				GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_LINEAR);
				GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_REPEAT);
				GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_REPEAT);

				// Upload the texture data
				GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_RGBA, width, height, 0, GL46.GL_RGBA, GL46.GL_UNSIGNED_BYTE, decodedImage);

				// Generate Mip Map
				GL46.glGenerateMipmap(GL46.GL_TEXTURE_2D);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		texture.textureID = textureID;
		texture.textureIndex = 0;
		texture.glTextureType = GLTextureType.Texture2D;
		return texture;

	}

	public static interface ShaderClosable extends Closeable {
		@Override
		public abstract void close();

	}

	public static ShaderClosable useProgram(ShaderProgram program) {

		GL46.glUseProgram(
			program.programID);/*
								 * for (GLSLAttribute glAtt : program.atts.values()) { enableAttribute(glAtt); }
								 */
		return new ShaderClosable() {

			@Override
			public void close() {
				/*
				 * for (GLSLAttribute glAtt : program.atts.values()) { disableAttribute(glAtt);
				 * }
				 */
				GL46.glUseProgram(0);
			}
		};
	}

	/**
	 * This retrieves the stored uniform location from a generated program. This <b>cannot</b> be called before {@link ShaderProgramSystem2#create(GLContext, ShaderProgram)}<br>
	 * This will return null if it has been called before setup or if there is no such uniform found in the shader program. 
	 * @param program ShaderProgram to get the uniform location from
	 * @param uniformName name of the uniform to get the location of
	 * @return location of the uniform, or null if the uniform was not found. 
	 */
	public static int getUniformLocation(ShaderProgram program, String uniformName) {
		return program.shaderUniforms.get(uniformName);
	}

	public static ShaderClosable useModel(Mesh model) throws Exception {
		if (model.VAOID == 0) {
			throw new Exception("Mesh '" + model.name + "' has not been loaded to the GL context. cannot be rendered");
		} else
			GL46.glBindVertexArray(model.VAOID);
		return new ShaderClosable() {
			@Override
			public void close() {
				// unbind vertex array
				GL46.glBindVertexArray(0);
			}
		};
	}

	public static ShaderClosable useVBO(BufferObject vbo) throws Exception {
		if (vbo.VBOID == 0) {
			throw new Exception("Buffer has not been loaded to the GL context");
		}
		GL46.glBindBuffer(vbo.bufferType.getGLID(), vbo.VBOID);
		return new ShaderClosable() {

			@Override
			public void close() {
				GL46.glBindBuffer(vbo.bufferType.getGLID(), 0);
			}
		};
	}

	public static ShaderClosable useTexture(GLTexture texture) {

		GL46.glActiveTexture(ShaderProgram.TextureUnits[texture.textureIndex]);
		GL46.glBindTexture(texture.glTextureType.getGLID(), texture.textureID);
		return new ShaderClosable() {

			@Override
			public void close() {
				GL46.glActiveTexture(ShaderProgram.TextureUnits[texture.textureIndex]);
				GL46.glBindTexture(texture.glTextureType.getGLID(), 0);
			}
		};
	}

	public static void renderMesh(Mesh mesh) {

		// check for index buffer
		WeakReference<IndexBufferObject> indexVboRef = mesh.index;
		IndexBufferObject indexVBO = null;

		if (indexVboRef != null) {
			indexVBO = indexVboRef.get();
		}
		renderMesh(mesh.VAOID, mesh.modelRenderType, indexVBO, mesh.vertexCount, mesh.renderCount);
	}

	public static void renderMesh(int meshID, GLDrawMode modelRenderType, IndexBufferObject buffer, int vertexCount, int renderCount) {
		renderMesh(meshID, modelRenderType.getGLID(), buffer == null ? 0 : buffer.VBOID, vertexCount, renderCount);
	}

	public static void renderMesh(int meshID, int renderType, int buffer, int vertexCount, int renderCount) {

		if (buffer > 0) {
			// if any index buffer then bind
			GL46.glBindBuffer(BufferType.ElementArrayBuffer.getGLID(), buffer);
			// render count more than one = instanced
			if (renderCount > 1) {
				GL46.glDrawElementsInstanced(renderType, vertexCount, GLDataType.UnsignedInt.getGLID(), 0, renderCount);
			} else if (renderCount == 1) {
				GL46.glDrawElements(renderType, vertexCount, GLDataType.UnsignedInt.getGLID(), 0);
			}
		} else {
			// render count more than one = instanced
			if (renderCount > 1) {
				GL46.glDrawArraysInstanced(renderType, 0, vertexCount, renderCount);
			} else if (renderCount == 1) {
				GL46.glDrawArrays(renderType, 0, vertexCount);
			}

		}
	}

	public static void tryFullRenderMesh(Mesh mesh) {

		// only render if visible
		if (mesh != null && mesh.visible) {
			try (ShaderClosable vaoc = useModel(mesh)) {
				Collection<BufferObject> vboSet = mesh.VBOs;
				for (Iterator<BufferObject> iterator = vboSet.iterator(); iterator.hasNext();) {
					BufferObject vbo = iterator.next();
					// check if need to write to the buffer data to OGL
					boolean write = vbo.update.compareAndSet(true, false);
					if (write) {
						try (ShaderClosable vboc = useVBO(vbo)) {
							writeDataToBuffer(vbo);
						} catch (Exception err) {
							new Exception("Buffer attatched to mesh '" + mesh.name + "' throw an error", err).printStackTrace();
						}
					}
				}

				renderMesh(mesh);

			} catch (Exception err) {
				err.printStackTrace();
			}

		}
	}

}
