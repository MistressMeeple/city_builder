package com.meeple.shared.frame.OGL;

import java.lang.ref.WeakReference;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.opengl.ARBIndirectParameters;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL46;

import com.meeple.shared.CollectionSuppliers;

public class ShaderProgram {

	protected static final int maxAttribDataSize = 4;
	protected static final int NULL = 0;
	private static int bitsPerByte = 8;

	public enum GLDataType implements GLEnum {
		Byte(GL46.GL_BYTE, 8),
		UnsignedByte(GL46.GL_UNSIGNED_BYTE, 16),
		Short(GL46.GL_SHORT, 16),
		UnsignedShort(GL46.GL_UNSIGNED_SHORT, 16),
		Int(GL46.GL_INT, 32),
		UnsignedInt(GL46.GL_UNSIGNED_INT, 32),
		Fixed(GL46.GL_FIXED, 32),
		HalfFloat(GL46.GL_HALF_FLOAT, 16),
		Float(GL46.GL_FLOAT, 32),
		Double(GL46.GL_DOUBLE, 64);
		private int dataType;
		private int bits;

		GLDataType(int dataType, int bits) {
			this.dataType = dataType;
			this.bits = bits;
		}

		public int getBytes() {
			return bits / bitsPerByte;
		}

		public int getBits() {
			return bits;
		}

		@Override
		public int getGLID() {
			return dataType;
		}
	}

	public enum GLDrawMode implements GLEnum {

		Points(GL46.GL_POINTS),
		LineStrip(GL46.GL_LINE_STRIP),
		LineLoop(GL46.GL_LINE_LOOP),
		Line(GL46.GL_LINES),
		TriangleStrip(GL46.GL_TRIANGLE_STRIP),
		TriangleFan(GL46.GL_TRIANGLE_FAN),
		Triangles(GL46.GL_TRIANGLES),
		LinesAdjacency(GL46.GL_LINES_ADJACENCY),
		LinesAdjacencyStrip(GL46.GL_LINE_STRIP_ADJACENCY),
		TrianglesAdjacency(GL46.GL_TRIANGLES_ADJACENCY),
		TrianglesStripAdjacency(GL46.GL_TRIANGLE_STRIP_ADJACENCY);

		int drawType;

		private GLDrawMode(int drawType) {
			this.drawType = drawType;
		}

		@Override
		public int getGLID() {
			return drawType;
		}
	}

	public enum BufferType implements GLEnum {
		ArrayBuffer(GL46.GL_ARRAY_BUFFER),
		ElementArrayBuffer(GL46.GL_ELEMENT_ARRAY_BUFFER),
		PixelPackBuffer(GL46.GL_PIXEL_PACK_BUFFER),
		PixelUnpackBuffer(GL46.GL_PIXEL_UNPACK_BUFFER),
		TransformFeedbackBuffer(GL46.GL_TRANSFORM_FEEDBACK_BUFFER),
		UniformBuffer(GL46.GL_UNIFORM_BUFFER),
		TextureBuffer(GL46.GL_TEXTURE_BUFFER),
		CopyReadBuffer(GL46.GL_COPY_READ_BUFFER),
		CopyWriteBuffer(GL46.GL_COPY_WRITE_BUFFER),
		DrawIndirectBuffer(GL46.GL_DRAW_INDIRECT_BUFFER),
		AtomicCounterBuffer(GL46.GL_ATOMIC_COUNTER_BUFFER),
		DispatchIndirectBuffer(GL46.GL_DISPATCH_INDIRECT_BUFFER),
		ShaderStorageBuffer(GL46.GL_SHADER_STORAGE_BUFFER),
		ParameterBufferARB(ARBIndirectParameters.GL_PARAMETER_BUFFER_ARB);
		private int glID;

		private BufferType(int GLID) {
			this.glID = GLID;
		}

		public int getGLID() {
			return glID;
		}
	}

	/**
	* <small>Copied from {@link GL15#glBufferData(int, ByteBuffer, int)}</small><br>
	* <p>
	* Defines the frequency of access (modification and usage), and the nature of that access. 
	* <BR>The frequency of access may be one of these:</p>
	* 
	* <ul>
	* <li><em>STREAM</em> - The data store contents will be modified once and used at most a few times.</li>
	* <li><em>STATIC</em> - The data store contents will be modified once and used many times.</li>
	* <li><em>DYNAMIC</em> - The data store contents will be modified repeatedly and used many times.</li>
	* </ul>
	* 
	* <p>The nature of access may be one of these:</p>
	* 
	* <ul>
	* <li><em>DRAW</em> - The data store contents are modified by the application, and used as the source for GL drawing and image specification commands.</li>
	* <li><em>READ</em> - The data store contents are modified by reading data from the GL, and used to return that data when queried by the application.</li>
	* <li><em>COPY</em> - The data store contents are modified by reading data from the GL, and used as the source for GL drawing and image specification commands.</li>
	* </ul>
	*
	* @param target the target buffer object. One of:<br><table><tr><td>{@link GL15C#GL_ARRAY_BUFFER ARRAY_BUFFER}</td><td>{@link GL15C#GL_ELEMENT_ARRAY_BUFFER ELEMENT_ARRAY_BUFFER}</td><td>{@link GL21#GL_PIXEL_PACK_BUFFER PIXEL_PACK_BUFFER}</td><td>{@link GL21#GL_PIXEL_UNPACK_BUFFER PIXEL_UNPACK_BUFFER}</td></tr><tr><td>{@link GL30#GL_TRANSFORM_FEEDBACK_BUFFER TRANSFORM_FEEDBACK_BUFFER}</td><td>{@link GL31#GL_UNIFORM_BUFFER UNIFORM_BUFFER}</td><td>{@link GL31#GL_TEXTURE_BUFFER TEXTURE_BUFFER}</td><td>{@link GL31#GL_COPY_READ_BUFFER COPY_READ_BUFFER}</td></tr><tr><td>{@link GL31#GL_COPY_WRITE_BUFFER COPY_WRITE_BUFFER}</td><td>{@link GL40#GL_DRAW_INDIRECT_BUFFER DRAW_INDIRECT_BUFFER}</td><td>{@link GL42#GL_ATOMIC_COUNTER_BUFFER ATOMIC_COUNTER_BUFFER}</td><td>{@link GL43#GL_DISPATCH_INDIRECT_BUFFER DISPATCH_INDIRECT_BUFFER}</td></tr><tr><td>{@link GL43#GL_SHADER_STORAGE_BUFFER SHADER_STORAGE_BUFFER}</td><td>{@link ARBIndirectParameters#GL_PARAMETER_BUFFER_ARB PARAMETER_BUFFER_ARB}</td></tr></table>
	* @param data   a pointer to data that will be copied into the data store for initialization, or {@code NULL} if no data is to be copied
	* @param usage  the expected usage pattern of the data store. One of:<br><table><tr><td>{@link GL15C#GL_STREAM_DRAW STREAM_DRAW}</td><td>{@link GL15C#GL_STREAM_READ STREAM_READ}</td><td>{@link GL15C#GL_STREAM_COPY STREAM_COPY}</td><td>{@link GL15C#GL_STATIC_DRAW STATIC_DRAW}</td><td>{@link GL15C#GL_STATIC_READ STATIC_READ}</td><td>{@link GL15C#GL_STATIC_COPY STATIC_COPY}</td><td>{@link GL15C#GL_DYNAMIC_DRAW DYNAMIC_DRAW}</td></tr><tr><td>{@link GL15C#GL_DYNAMIC_READ DYNAMIC_READ}</td><td>{@link GL15C#GL_DYNAMIC_COPY DYNAMIC_COPY}</td></tr></table>
	* 
	* @see <a target="_blank" href="http://docs.gl/gl4/glBufferData">Reference Page</a>
	*/
	public enum BufferUsage implements GLEnum {

		/**
		* <small>Copied from {@link GL15#glBufferData(int, ByteBuffer, int)}</small><br>
		* <p>
		* Defines the frequency of access (modification and usage), and the nature of that access. 
		* <BR>The frequency of access may be one of these:</p>
		* 
		* <ul>
		* <li><em>STREAM</em> - The data store contents will be modified once and used at most a few times.</li>
		* <li><em>STATIC</em> - The data store contents will be modified once and used many times.</li>
		* <li><em>DYNAMIC</em> - The data store contents will be modified repeatedly and used many times.</li>
		* </ul>
		* 
		* <p>The nature of access may be one of these:</p>
		* 
		* <ul>
		* <li><em>DRAW</em> - The data store contents are modified by the application, and used as the source for GL drawing and image specification commands.</li>
		* <li><em>READ</em> - The data store contents are modified by reading data from the GL, and used to return that data when queried by the application.</li>
		* <li><em>COPY</em> - The data store contents are modified by reading data from the GL, and used as the source for GL drawing and image specification commands.</li>
		* </ul>
		*
		* @param target the target buffer object. One of:<br><table><tr><td>{@link GL15C#GL_ARRAY_BUFFER ARRAY_BUFFER}</td><td>{@link GL15C#GL_ELEMENT_ARRAY_BUFFER ELEMENT_ARRAY_BUFFER}</td><td>{@link GL21#GL_PIXEL_PACK_BUFFER PIXEL_PACK_BUFFER}</td><td>{@link GL21#GL_PIXEL_UNPACK_BUFFER PIXEL_UNPACK_BUFFER}</td></tr><tr><td>{@link GL30#GL_TRANSFORM_FEEDBACK_BUFFER TRANSFORM_FEEDBACK_BUFFER}</td><td>{@link GL31#GL_UNIFORM_BUFFER UNIFORM_BUFFER}</td><td>{@link GL31#GL_TEXTURE_BUFFER TEXTURE_BUFFER}</td><td>{@link GL31#GL_COPY_READ_BUFFER COPY_READ_BUFFER}</td></tr><tr><td>{@link GL31#GL_COPY_WRITE_BUFFER COPY_WRITE_BUFFER}</td><td>{@link GL40#GL_DRAW_INDIRECT_BUFFER DRAW_INDIRECT_BUFFER}</td><td>{@link GL42#GL_ATOMIC_COUNTER_BUFFER ATOMIC_COUNTER_BUFFER}</td><td>{@link GL43#GL_DISPATCH_INDIRECT_BUFFER DISPATCH_INDIRECT_BUFFER}</td></tr><tr><td>{@link GL43#GL_SHADER_STORAGE_BUFFER SHADER_STORAGE_BUFFER}</td><td>{@link ARBIndirectParameters#GL_PARAMETER_BUFFER_ARB PARAMETER_BUFFER_ARB}</td></tr></table>
		* @param data   a pointer to data that will be copied into the data store for initialization, or {@code NULL} if no data is to be copied
		* @param usage  the expected usage pattern of the data store. One of:<br><table><tr><td>{@link GL15C#GL_STREAM_DRAW STREAM_DRAW}</td><td>{@link GL15C#GL_STREAM_READ STREAM_READ}</td><td>{@link GL15C#GL_STREAM_COPY STREAM_COPY}</td><td>{@link GL15C#GL_STATIC_DRAW STATIC_DRAW}</td><td>{@link GL15C#GL_STATIC_READ STATIC_READ}</td><td>{@link GL15C#GL_STATIC_COPY STATIC_COPY}</td><td>{@link GL15C#GL_DYNAMIC_DRAW DYNAMIC_DRAW}</td></tr><tr><td>{@link GL15C#GL_DYNAMIC_READ DYNAMIC_READ}</td><td>{@link GL15C#GL_DYNAMIC_COPY DYNAMIC_COPY}</td></tr></table>
		* 
		* @see <a target="_blank" href="http://docs.gl/gl4/glBufferData">Reference Page</a>
		*/
		StreamDraw(GL46.GL_STREAM_DRAW),
		StreamRead(GL46.GL_STREAM_READ),
		StreamCopy(GL46.GL_STREAM_COPY),
		StaticDraw(GL46.GL_STATIC_DRAW),
		StaticRead(GL46.GL_STATIC_READ),
		StaticCopy(GL46.GL_STATIC_COPY),
		DynamicDraw(GL46.GL_DYNAMIC_DRAW),
		DynamicRead(GL46.GL_DYNAMIC_READ),
		DynamicCopy(GL46.GL_DYNAMIC_COPY);
		private int GLID;

		private BufferUsage(int glID) {
			this.GLID = glID;
		}

		@Override
		public int getGLID() {
			return GLID;
		}

	}

	public enum GLShaderType implements GLEnum {
		VertexShader(GL46.GL_VERTEX_SHADER),
		TessellationControlShader(GL46.GL_TESS_CONTROL_SHADER),
		TessellationEvaluationShader(GL46.GL_TESS_EVALUATION_SHADER),
		GeometryShader(GL46.GL_GEOMETRY_SHADER),
		FragmentShader(GL46.GL_FRAGMENT_SHADER),
		ComputeShader(GL46.GL_COMPUTE_SHADER);
		private int GLID;

		private GLShaderType(int glID) {
			this.GLID = glID;

		}

		@Override
		public int getGLID() {
			return GLID;
		}
	}

	public static class VAO {
		public int VAOID = NULL;
		public Set<VBO> VBOs = new CollectionSuppliers.SetSupplier<VBO>().get();
		public boolean singleFrameDiscard = false;
	}

	public static class VBO {

		public int VBOID = NULL;
		/**
		* To be used when calling {@link org.lwjgl.opengl.GL20#glBufferData(int, ByteBuffer, int)} as first parameter ('target')
		*/
		public BufferType bufferType;
		/**
		* To be used when calling {@link org.lwjgl.opengl.GL20#glBufferData(int, ByteBuffer, int)} as third parameter ('usage')
		*/
		public BufferUsage bufferUsage = BufferUsage.StaticDraw;
		/**
		 * To be used when calling {@link org.lwjgl.opengl.GL20#glVertexAttribPointer} as second parameter ('size')
		 */
		public int dataSize = 3;
		public GLDataType dataType;
		public final AtomicBoolean update = new AtomicBoolean(true);
		//		public final AtomicBoolean uploadBuffer = new AtomicBoolean(false);
		public List<Number> data = new CollectionSuppliers.ListSupplier<Number>().get();
		public Buffer buffer;
	}

	public static class Mesh extends VAO {
		public String name;
		public int vertexCount;
		public GLDrawMode modelRenderType = GLDrawMode.Triangles;
		public WeakReference<VBO> index;
		/**
		 * Stored as: instance stride - buffer ID
		 */
		public Map<Integer, Set<WeakReference<VBO>>> instanceAttributes = new CollectionSuppliers.MapSupplier<Integer, Set<WeakReference<VBO>>>().get();
		public int renderCount = 1;
		public boolean visible = true;
	}

	public static class Attribute extends VBO {

		protected int index;
		public final boolean enabled = true;
		/**
		 * The name in the shader source to link to
		 */
		public String name;

		/**
		 * To be used when calling {@link org.lwjgl.opengl.GL20#glVertexAttribPointer} as fourth parameter ('normalised')
		 */
		public boolean normalised = false;

		/**
		 * Sets whether or not this attribute is instanced. 
		 */
		public boolean instanced = false;

		public int instanceStride = 1;

	}

	public int programID;
	/**
	 * Once the program has been created, shader sources can only be obtained through the origianl method of getting it or by using {@link GL46#glGetShaderSource(int)}
	 */
	public final Map<GLShaderType, String> shaderSources = new CollectionSuppliers.MapSupplier<GLShaderType, String>().get();
	public final Map<GLShaderType, Integer> shaderIDs = new CollectionSuppliers.MapSupplier<GLShaderType, Integer>().get();

	public final Set<VAO> VAOs = new CollectionSuppliers.SetSupplier<VAO>().get();

	public final Map<UniformManager<?, ?>, Map<UniformManager<?, ?>.Uniform<?>, List<?>>> uniformSystems =
		new CollectionSuppliers.MapSupplier<UniformManager<?, ?>, Map<UniformManager<?, ?>.Uniform<?>, List<?>>>().get();

}
