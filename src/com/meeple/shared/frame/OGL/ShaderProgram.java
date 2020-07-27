package com.meeple.shared.frame.OGL;

import java.util.List;

import java.lang.ref.WeakReference;
import java.nio.Buffer;
import java.nio.ByteBuffer;
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

import com.meeple.backend.GLHelper.GLSLAttribute;
import com.meeple.shared.CollectionSuppliers;

public class ShaderProgram {

	protected static final int maxAttribDataSize = 4;
	protected static final int NULL = 0;
	private static int bitsPerByte = 8;

	public static enum GLStatus implements GLEnum {
		LinkStatus(GL46.GL_LINK_STATUS), ValidateStatus(GL46.GL_VALIDATE_STATUS), CompileStatus(GL46.GL_COMPILE_STATUS);
		private int type;

		GLStatus(int type) {
			this.type = type;
		}

		@Override
		public int getGLID() {
			return type;
		}
	}

	public static enum GLCompoundDataType implements GLEnum {

		Vec2f(GL46.GL_FLOAT_VEC2, GLDataType.Float, 2),
		Vec3f(GL46.GL_FLOAT_VEC3, GLDataType.Float, 3),
		Vec4f(GL46.GL_FLOAT_VEC4, GLDataType.Float, 4),

		Vec2i(GL46.GL_INT_VEC2, GLDataType.Int, 2),
		Vec3i(GL46.GL_INT_VEC3, GLDataType.Int, 3),
		Vec4i(GL46.GL_INT_VEC4, GLDataType.Int, 4),
		/*
		 * Vec2b(GL46.GL_BOOL_VEC2, GLDataType.Bool, 2), Vec3b(GL46.GL_BOOL_VEC3,
		 * GLDataType.Bool, 3), Vec4b(GL46.GL_BOOL_VEC4, GLDataType.Bool, 4),
		 */
		Mat2f(GL46.GL_FLOAT_MAT2, GLDataType.Float, 4),
		Mat3f(GL46.GL_FLOAT_MAT3, GLDataType.Float, 9),
		Mat4f(GL46.GL_FLOAT_MAT4, GLDataType.Float, 16);
		private int id;
		GLDataType base;
		int count;

		GLCompoundDataType(int id, GLDataType base, int count) {
			this.id = id;
			this.base = base;
			this.count = count;
		}

		@Override
		public int getGLID() {
			return id;
		}
	}

	public enum GLDataType implements GLEnum {
		/// --primary
		// TODO check book has only 1 bit??
		// Bool(GL46.GL_BOOL, 1),
		Byte(GL46.GL_BYTE, 8),
		UnsignedByte(GL46.GL_UNSIGNED_BYTE, 16),
		Short(GL46.GL_SHORT, 16),
		UnsignedShort(GL46.GL_UNSIGNED_SHORT, 16),
		Int(GL46.GL_INT, 32),
		UnsignedInt(GL46.GL_UNSIGNED_INT, 32),
		Fixed(GL46.GL_FIXED, 32),
		HalfFloat(GL46.GL_HALF_FLOAT, 16),
		Float(GL46.GL_FLOAT, 32),
		Double(GL46.GL_DOUBLE, 64);/*
									 * //samplers Sampler1D(GL46.GL_SAMPLER_1D, -1), Sampler2D(GL46.GL_SAMPLER_2D,
									 * -1), Sampler3D(GL46.GL_SAMPLER_3D, -1), SamplerCube(GL46.GL_SAMPLER_CUBE,
									 * -1), Sampler1DShadow(GL46.GL_SAMPLER_1D_SHADOW, -1),
									 * Sampler2DShadow(GL46.GL_SAMPLER_2D_SHADOW, -1);
									 */

		private int id;
		private int bits;

		GLDataType(int id, int bits) {
			this.id = id;
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
			return id;
		}

		public static GLDataType fromValue(int id) {
			for (GLDataType type : values()) {
				if (id == type.id) {
					return type;
				}
			}
			return null;
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
	 * <small>Copied from
	 * {@link GL15#glBufferData(int, ByteBuffer, int)}</small><br>
	 * <p>
	 * Defines the frequency of access (modification and usage), and the nature of
	 * that access. <BR>
	 * The frequency of access may be one of these:
	 * </p>
	 * 
	 * <ul>
	 * <li><em>STREAM</em> - The data store contents will be modified once and used
	 * at most a few times.</li>
	 * <li><em>STATIC</em> - The data store contents will be modified once and used
	 * many times.</li>
	 * <li><em>DYNAMIC</em> - The data store contents will be modified repeatedly
	 * and used many times.</li>
	 * </ul>
	 * 
	 * <p>
	 * The nature of access may be one of these:
	 * </p>
	 * 
	 * <ul>
	 * <li><em>DRAW</em> - The data store contents are modified by the application,
	 * and used as the source for GL drawing and image specification commands.</li>
	 * <li><em>READ</em> - The data store contents are modified by reading data from
	 * the GL, and used to return that data when queried by the application.</li>
	 * <li><em>COPY</em> - The data store contents are modified by reading data from
	 * the GL, and used as the source for GL drawing and image specification
	 * commands.</li>
	 * </ul>
	 *
	 * @param target the target buffer object. One of:<br>
	 *               <table>
	 *               <tr>
	 *               <td>{@link GL15C#GL_ARRAY_BUFFER ARRAY_BUFFER}</td>
	 *               <td>{@link GL15C#GL_ELEMENT_ARRAY_BUFFER
	 *               ELEMENT_ARRAY_BUFFER}</td>
	 *               <td>{@link GL21#GL_PIXEL_PACK_BUFFER PIXEL_PACK_BUFFER}</td>
	 *               <td>{@link GL21#GL_PIXEL_UNPACK_BUFFER
	 *               PIXEL_UNPACK_BUFFER}</td>
	 *               </tr>
	 *               <tr>
	 *               <td>{@link GL30#GL_TRANSFORM_FEEDBACK_BUFFER
	 *               TRANSFORM_FEEDBACK_BUFFER}</td>
	 *               <td>{@link GL31#GL_UNIFORM_BUFFER UNIFORM_BUFFER}</td>
	 *               <td>{@link GL31#GL_TEXTURE_BUFFER TEXTURE_BUFFER}</td>
	 *               <td>{@link GL31#GL_COPY_READ_BUFFER COPY_READ_BUFFER}</td>
	 *               </tr>
	 *               <tr>
	 *               <td>{@link GL31#GL_COPY_WRITE_BUFFER COPY_WRITE_BUFFER}</td>
	 *               <td>{@link GL40#GL_DRAW_INDIRECT_BUFFER
	 *               DRAW_INDIRECT_BUFFER}</td>
	 *               <td>{@link GL42#GL_ATOMIC_COUNTER_BUFFER
	 *               ATOMIC_COUNTER_BUFFER}</td>
	 *               <td>{@link GL43#GL_DISPATCH_INDIRECT_BUFFER
	 *               DISPATCH_INDIRECT_BUFFER}</td>
	 *               </tr>
	 *               <tr>
	 *               <td>{@link GL43#GL_SHADER_STORAGE_BUFFER
	 *               SHADER_STORAGE_BUFFER}</td>
	 *               <td>{@link ARBIndirectParameters#GL_PARAMETER_BUFFER_ARB
	 *               PARAMETER_BUFFER_ARB}</td>
	 *               </tr>
	 *               </table>
	 * @param data   a pointer to data that will be copied into the data store for
	 *               initialization, or {@code NULL} if no data is to be copied
	 * @param usage  the expected usage pattern of the data store. One of:<br>
	 *               <table>
	 *               <tr>
	 *               <td>{@link GL15C#GL_STREAM_DRAW STREAM_DRAW}</td>
	 *               <td>{@link GL15C#GL_STREAM_READ STREAM_READ}</td>
	 *               <td>{@link GL15C#GL_STREAM_COPY STREAM_COPY}</td>
	 *               <td>{@link GL15C#GL_STATIC_DRAW STATIC_DRAW}</td>
	 *               <td>{@link GL15C#GL_STATIC_READ STATIC_READ}</td>
	 *               <td>{@link GL15C#GL_STATIC_COPY STATIC_COPY}</td>
	 *               <td>{@link GL15C#GL_DYNAMIC_DRAW DYNAMIC_DRAW}</td>
	 *               </tr>
	 *               <tr>
	 *               <td>{@link GL15C#GL_DYNAMIC_READ DYNAMIC_READ}</td>
	 *               <td>{@link GL15C#GL_DYNAMIC_COPY DYNAMIC_COPY}</td>
	 *               </tr>
	 *               </table>
	 * 
	 * @see <a target="_blank" href="http://docs.gl/gl4/glBufferData">Reference
	 *      Page</a>
	 */
	public enum BufferUsage implements GLEnum {

		/**
		 * <small>Copied from
		 * {@link GL15#glBufferData(int, ByteBuffer, int)}</small><br>
		 * <p>
		 * Defines the frequency of access (modification and usage), and the nature of
		 * that access. <BR>
		 * The frequency of access may be one of these:
		 * </p>
		 * 
		 * <ul>
		 * <li><em>STREAM</em> - The data store contents will be modified once and used
		 * at most a few times.</li>
		 * <li><em>STATIC</em> - The data store contents will be modified once and used
		 * many times.</li>
		 * <li><em>DYNAMIC</em> - The data store contents will be modified repeatedly
		 * and used many times.</li>
		 * </ul>
		 * 
		 * <p>
		 * The nature of access may be one of these:
		 * </p>
		 * 
		 * <ul>
		 * <li><em>DRAW</em> - The data store contents are modified by the application,
		 * and used as the source for GL drawing and image specification commands.</li>
		 * <li><em>READ</em> - The data store contents are modified by reading data from
		 * the GL, and used to return that data when queried by the application.</li>
		 * <li><em>COPY</em> - The data store contents are modified by reading data from
		 * the GL, and used as the source for GL drawing and image specification
		 * commands.</li>
		 * </ul>
		 *
		 * @param target the target buffer object. One of:<br>
		 *               <table>
		 *               <tr>
		 *               <td>{@link GL15C#GL_ARRAY_BUFFER ARRAY_BUFFER}</td>
		 *               <td>{@link GL15C#GL_ELEMENT_ARRAY_BUFFER
		 *               ELEMENT_ARRAY_BUFFER}</td>
		 *               <td>{@link GL21#GL_PIXEL_PACK_BUFFER PIXEL_PACK_BUFFER}</td>
		 *               <td>{@link GL21#GL_PIXEL_UNPACK_BUFFER
		 *               PIXEL_UNPACK_BUFFER}</td>
		 *               </tr>
		 *               <tr>
		 *               <td>{@link GL30#GL_TRANSFORM_FEEDBACK_BUFFER
		 *               TRANSFORM_FEEDBACK_BUFFER}</td>
		 *               <td>{@link GL31#GL_UNIFORM_BUFFER UNIFORM_BUFFER}</td>
		 *               <td>{@link GL31#GL_TEXTURE_BUFFER TEXTURE_BUFFER}</td>
		 *               <td>{@link GL31#GL_COPY_READ_BUFFER COPY_READ_BUFFER}</td>
		 *               </tr>
		 *               <tr>
		 *               <td>{@link GL31#GL_COPY_WRITE_BUFFER COPY_WRITE_BUFFER}</td>
		 *               <td>{@link GL40#GL_DRAW_INDIRECT_BUFFER
		 *               DRAW_INDIRECT_BUFFER}</td>
		 *               <td>{@link GL42#GL_ATOMIC_COUNTER_BUFFER
		 *               ATOMIC_COUNTER_BUFFER}</td>
		 *               <td>{@link GL43#GL_DISPATCH_INDIRECT_BUFFER
		 *               DISPATCH_INDIRECT_BUFFER}</td>
		 *               </tr>
		 *               <tr>
		 *               <td>{@link GL43#GL_SHADER_STORAGE_BUFFER
		 *               SHADER_STORAGE_BUFFER}</td>
		 *               <td>{@link ARBIndirectParameters#GL_PARAMETER_BUFFER_ARB
		 *               PARAMETER_BUFFER_ARB}</td>
		 *               </tr>
		 *               </table>
		 * @param data   a pointer to data that will be copied into the data store for
		 *               initialization, or {@code NULL} if no data is to be copied
		 * @param usage  the expected usage pattern of the data store. One of:<br>
		 *               <table>
		 *               <tr>
		 *               <td>{@link GL15C#GL_STREAM_DRAW STREAM_DRAW}</td>
		 *               <td>{@link GL15C#GL_STREAM_READ STREAM_READ}</td>
		 *               <td>{@link GL15C#GL_STREAM_COPY STREAM_COPY}</td>
		 *               <td>{@link GL15C#GL_STATIC_DRAW STATIC_DRAW}</td>
		 *               <td>{@link GL15C#GL_STATIC_READ STATIC_READ}</td>
		 *               <td>{@link GL15C#GL_STATIC_COPY STATIC_COPY}</td>
		 *               <td>{@link GL15C#GL_DYNAMIC_DRAW DYNAMIC_DRAW}</td>
		 *               </tr>
		 *               <tr>
		 *               <td>{@link GL15C#GL_DYNAMIC_READ DYNAMIC_READ}</td>
		 *               <td>{@link GL15C#GL_DYNAMIC_COPY DYNAMIC_COPY}</td>
		 *               </tr>
		 *               </table>
		 * 
		 * @see <a target="_blank" href="http://docs.gl/gl4/glBufferData">Reference
		 *      Page</a>
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
		public Set<BufferObject> VBOs = new CollectionSuppliers.SetSupplier<BufferObject>().get();

		@Deprecated
		public boolean singleFrameDiscard = false;

		public VAO addBuffer(BufferObject buffer) {
			VBOs.add(buffer);
			return this;
		}
	}

	public static enum BufferDataManagementType {
		Address, Buffer, List, Manual, Empty
	}

	public static class BufferObject {

		public int VBOID = NULL;
		/**
		 * To be used when calling {@link GL46#glBufferData(int, ByteBuffer, int)} as
		 * first parameter ('target')
		 */
		public BufferType bufferType;
		/**
		 * To be used when calling {@link GL46#glBufferData(int, ByteBuffer, int)} as
		 * third parameter ('usage')
		 */
		public BufferUsage bufferUsage = BufferUsage.StaticDraw;
		public int dataSize = 3;
		public GLDataType dataType;
		public final AtomicBoolean update = new AtomicBoolean(true);
		public BufferDataManagementType bufferResourceType = BufferDataManagementType.List;
		public List<Number> data = new CollectionSuppliers.ListSupplier<Number>().get();
		public Buffer buffer;
		public Long bufferAddress;
		/**
		 * used when using the unsafe direct buffer allocation
		 */
		public Long bufferLen;

		public BufferObject bufferType(BufferType bufferType) {
			this.bufferType = bufferType;
			return this;
		}

		public BufferObject bufferUsage(BufferUsage bufferUsage) {
			this.bufferUsage = bufferUsage;
			return this;
		}

		public BufferObject dataType(GLDataType type) {
			this.dataType = type;
			return this;
		}

		public BufferObject dataSize(int size) {
			this.dataSize = size;
			return this;
		}

		public BufferObject dataType(GLCompoundDataType type) {
			this.dataType = type.base;
			this.dataSize = type.count;
			return this;
		}

		public BufferObject data(Number[] data) {
			for (Number d : data) {
				this.data.add(d);
			}
			this.bufferResourceType = BufferDataManagementType.List;
			return this;
		}

		public BufferObject data(float[] data) {
			for (float d : data) {
				this.data.add(d);
			}
			this.bufferResourceType = BufferDataManagementType.List;
			return this;
		}

		public BufferObject data(List<Number> data) {
			this.data = data;
			this.bufferResourceType = BufferDataManagementType.List;
			return this;
		}

		public BufferObject data(Buffer data) {
			this.buffer = data;
			this.bufferResourceType = BufferDataManagementType.Buffer;
			return this;
		}

		public BufferObject data(Long bufferAddress, Long bufferLen) {
			this.bufferAddress = bufferAddress;
			this.bufferLen = bufferLen;
			this.bufferResourceType = BufferDataManagementType.Address;
			return this;
		}

		public BufferObject data(long emptyLen) {
			this.bufferLen = emptyLen;
			this.bufferResourceType = BufferDataManagementType.Empty;
			return this;
		}

	}

	public static class IndexBufferObject extends BufferObject {
		public IndexBufferObject() {
			bufferType = BufferType.ElementArrayBuffer;
			dataType = GLDataType.UnsignedInt;
			dataSize = 3;
		}

		public IndexBufferObject bufferUsage(BufferUsage bufferUsage) {
			this.bufferUsage = bufferUsage;
			return this;
		}

		public IndexBufferObject data(Number[] data) {
			for (Number d : data) {
				this.data.add(d);
			}
			this.bufferResourceType = BufferDataManagementType.List;
			return this;
		}

		public IndexBufferObject data(float[] data) {
			for (float d : data) {
				this.data.add(d);
			}
			this.bufferResourceType = BufferDataManagementType.List;
			return this;
		}
		public IndexBufferObject data(int[] data) {
			for(int i : data) {
				this.data.add(i);
			}
			this.bufferResourceType = BufferDataManagementType.List;
			return this;
		}

		public IndexBufferObject data(List<Number> data) {
			this.data = data;
			this.bufferResourceType = BufferDataManagementType.List;
			return this;
		}

		public IndexBufferObject data(Buffer data) {
			this.buffer = data;
			this.bufferResourceType = BufferDataManagementType.Buffer;
			return this;
		}

		public IndexBufferObject data(Long bufferAddress, Long bufferLen) {
			this.bufferAddress = bufferAddress;
			this.bufferLen = bufferLen;
			this.bufferResourceType = BufferDataManagementType.Address;
			return this;
		}

		public IndexBufferObject data(long emptyLen) {
			this.bufferLen = emptyLen;
			this.bufferResourceType = BufferDataManagementType.Empty;
			return this;
		}

		public IndexBufferObject dataSize(int size) {
			this.dataSize = size;
			return this;
		}

	}

	public static class Mesh extends VAO {
		public String name;
		public int vertexCount;
		public GLDrawMode modelRenderType = GLDrawMode.Triangles;
		public WeakReference<IndexBufferObject> index;
		public Map<String, WeakReference<VertexAttribute>> attributes = new CollectionSuppliers.MapSupplier<String, WeakReference<VertexAttribute>>().get();

		public int renderCount = 1;
		public boolean visible = true;

		public Mesh drawMode(GLDrawMode drawMode) {
			this.modelRenderType = drawMode;
			return this;
		}

		public Mesh index(IndexBufferObject index) {
			this.index = new WeakReference<>(index);
			VBOs.add(index);
			return this;
		}

		public Mesh vertexCount(int count) {
			this.vertexCount = count;
			return this;
		}

		public Mesh visible(boolean visible) {
			this.visible = visible;
			return this;
		}

		public Mesh renderCount(int count) {
			this.renderCount = count;
			return this;
		}

		public Mesh addAttribute(WeakReference<VertexAttribute> attribute) {
			attributes.put(attribute.get().name, attribute);
			VBOs.add(attribute.get());

			return this;
		}

		public Mesh addAttribute(VertexAttribute attribute) {
			attributes.put(attribute.name, new WeakReference<>(attribute));
			VBOs.add(attribute);

			return this;
		}

		public VertexAttribute getAttribute(String name) {
			WeakReference<VertexAttribute> a = attributes.get(name);
			return a.get();
		}

		public Mesh addBuffer(BufferObject buffer) {
			VBOs.add(buffer);
			return this;
		}

		public Mesh renderMode(GLDrawMode mode) {
			this.modelRenderType = mode;
			return this;
		}

		public Mesh name(String name) {
			this.name = name;
			return this;
		}
	}

	public static class VertexAttribute extends BufferObject {

		protected int index;
		public final boolean enabled = true;
		/**
		 * The name in the shader source to link to
		 */
		public String name;

		/**
		 * To be used when calling {@link org.lwjgl.opengl.GL20#glVertexAttribPointer}
		 * as fourth parameter ('normalised')
		 */
		public boolean normalised = false;

		/**
		 * Sets whether or not this attribute is instanced.
		 */
		public boolean instanced = false;

		public int instanceStride = 1;

		@Override
		public VertexAttribute bufferType(BufferType bufferType) {
			this.bufferType = bufferType;
			return this;
		}

		@Override
		public VertexAttribute bufferUsage(BufferUsage bufferUsage) {
			this.bufferUsage = bufferUsage;
			return this;
		}

		public VertexAttribute name(String name) {
			this.name = name;
			return this;
		}

		public VertexAttribute instanced(boolean instanced, int stride) {
			this.instanced = instanced;
			this.instanceStride = stride;
			return this;
		}

		@Override
		public VertexAttribute dataType(GLDataType type) {
			this.dataType = type;
			return this;
		}

		@Override
		public VertexAttribute dataSize(int size) {
			this.dataSize = size;
			return this;
		}

		@Override
		public BufferObject dataType(GLCompoundDataType type) {
			this.dataType = type.base;
			this.dataSize = type.count;
			return this;
		}

		@Override
		public VertexAttribute data(Number[] data) {
			this.data.clear();
			for (Number d : data) {
				this.data.add(d);
			}
			this.bufferResourceType = BufferDataManagementType.List;
			return this;
		}

		@Override
		public VertexAttribute data(float[] data) {
			this.data.clear();
			for (float d : data) {
				this.data.add(d);
			}
			this.bufferResourceType = BufferDataManagementType.List;
			return this;
		}

		@Override
		public VertexAttribute data(List<Number> data) {
			this.data = data;
			this.bufferResourceType = BufferDataManagementType.List;
			return this;
		}

		@Override
		public VertexAttribute data(Buffer data) {
			this.buffer = data;
			this.bufferResourceType = BufferDataManagementType.Buffer;
			return this;
		}

		@Override
		public VertexAttribute data(Long bufferAddress, Long bufferLen) {
			this.bufferAddress = bufferAddress;
			this.bufferLen = bufferLen;
			this.bufferResourceType = BufferDataManagementType.Address;
			return this;
		}

		@Override
		public VertexAttribute data(long emptyLen) {
			this.bufferLen = emptyLen;
			this.bufferResourceType = BufferDataManagementType.Empty;
			return this;
		}

	}

	public static class AttributeFactory {

		/**
		 * To be used when calling {@link GL46#glBufferData(int, ByteBuffer, int)} as
		 * first parameter ('target')
		 */
		public BufferType bufferType;
		/**
		 * To be used when calling {@link GL46#glBufferData(int, ByteBuffer, int)} as
		 * third parameter ('usage')
		 */
		public BufferUsage bufferUsage = BufferUsage.StaticDraw;
		/**
		 * To be used when calling {@link GL46#glVertexAttribPointer} as second
		 * parameter ('size')
		 */
		public int dataSize = 3;
		public GLDataType dataType;
		public final AtomicBoolean update = new AtomicBoolean(true);
		// public final AtomicBoolean uploadBuffer = new AtomicBoolean(false);
		public BufferDataManagementType bufferResourceType = BufferDataManagementType.List;
		public List<Number> data = new CollectionSuppliers.ListSupplier<Number>().get();
		public Buffer buffer;
		public Long bufferAddress;
		public Long bufferLen;
		public String name;

		public boolean normalised = false;

		public boolean instanced = false;
		public int instanceStride = 1;

		public VertexAttribute build() {
			return new VertexAttribute().name(this.name).dataType(this.dataType).dataSize(this.dataSize).bufferType(this.bufferType).bufferUsage(this.bufferUsage).instanced(this.instanced, this.instanceStride);
		}

		public AttributeFactory bufferType(BufferType bufferType) {
			this.bufferType = bufferType;
			return this;
		}

		public AttributeFactory bufferUsage(BufferUsage bufferUsage) {
			this.bufferUsage = bufferUsage;
			return this;
		}

		public AttributeFactory name(String name) {
			this.name = name;
			return this;
		}

		public AttributeFactory instanced(boolean instanced, int stride) {
			this.instanced = instanced;
			this.instanceStride = stride;
			return this;
		}

		public AttributeFactory dataType(GLDataType type) {
			this.dataType = type;
			return this;
		}

		public AttributeFactory dataSize(int size) {
			this.dataSize = size;
			return this;
		}

		public AttributeFactory dataType(GLCompoundDataType type) {
			this.dataType = type.base;
			this.dataSize = type.count;
			return this;
		}

		public AttributeFactory data(Number[] data) {
			for (Number d : data) {
				this.data.add(d);
			}
			this.bufferResourceType = BufferDataManagementType.List;
			return this;
		}

		public AttributeFactory data(List<Number> data) {
			this.data = data;
			this.bufferResourceType = BufferDataManagementType.List;
			return this;
		}

		public AttributeFactory data(Buffer data) {
			this.buffer = data;
			this.bufferResourceType = BufferDataManagementType.Buffer;
			return this;
		}

		public AttributeFactory data(Long bufferAddress, Long bufferLen) {
			this.bufferAddress = bufferAddress;
			this.bufferLen = bufferLen;
			this.bufferResourceType = BufferDataManagementType.Address;
			return this;
		}

		public AttributeFactory data(long emptyLen) {
			this.bufferLen = emptyLen;
			this.bufferResourceType = BufferDataManagementType.Empty;
			return this;
		}
	}

	public int programID;
	/**
	 * Once the program has been created, shader sources can only be obtained
	 * through the original method of getting it or by using
	 * {@link GL46#glGetShaderSource(int)}
	 */
	public final Map<GLShaderType, String> shaderSources = new CollectionSuppliers.MapSupplier<GLShaderType, String>().get();
	public final Map<GLShaderType, Integer> shaderIDs = new CollectionSuppliers.MapSupplier<GLShaderType, Integer>().get();
	public final Map<String, GLSLAttribute> atts = new CollectionSuppliers.MapSupplier<String, GLSLAttribute>().get();
	public final Set<VAO> VAOs = new CollectionSuppliers.SetSupplier<VAO>().get();

	public final Map<UniformManager<?, ?>, Map<UniformManager<?, ?>.Uniform<?>, List<?>>> uniformSystems = new CollectionSuppliers.MapSupplier<UniformManager<?, ?>, Map<UniformManager<?, ?>.Uniform<?>, List<?>>>().get();

}
