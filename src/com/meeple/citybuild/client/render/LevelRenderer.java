package com.meeple.citybuild.client.render;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL46;

import com.meeple.citybuild.RayHelper;
import com.meeple.citybuild.client.CityBuilderMain;
import com.meeple.citybuild.client.gui.GameUI;
import com.meeple.citybuild.client.render.WorldRenderer.MeshExt;
import com.meeple.citybuild.client.render.structs.Light;
import com.meeple.citybuild.client.render.structs.Material;
import com.meeple.citybuild.server.Entity;
import com.meeple.citybuild.server.LevelData;
import com.meeple.citybuild.server.LevelData.Chunk;
import com.meeple.citybuild.server.LevelData.Chunk.Tile;
import com.meeple.citybuild.server.WorldGenerator.TileTypes;
import com.meeple.citybuild.server.WorldGenerator.Tiles;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.Tickable;
import com.meeple.shared.frame.CursorHelper;
import com.meeple.shared.frame.CursorHelper.SpaceState;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.KeyInputSystem;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.VertexAttribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferDataManagementType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgramSystem;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2.ShaderClosable;
import com.meeple.shared.frame.OGL.UniformManager;
import com.meeple.shared.frame.camera.VPMatrixSystem;
import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem;
import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem.ProjectionMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.VPMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.ViewMatrixSystem.CameraSpringArm;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;
import com.meeple.temp.Island;
import com.meeple.temp.Island.IslandSize;

public class LevelRenderer {
	public static Logger logger = Logger.getLogger(LevelRenderer.class);

	static class CubeMesh {
		VertexAttribute colourAttrib = new VertexAttribute();
		VertexAttribute translationAttrib = new VertexAttribute();
	}

	public static boolean disableAlphaTest = false;

	public UniformManager<String[], Integer[]>.Uniform<VPMatrix> setupWorldProgram(GLContext glc, ShaderProgram program, VPMatrixSystem VPMatrixSystem, VPMatrix vpMatrix) {
		UniformManager<String[], Integer[]>.Uniform<VPMatrix> u = ShaderProgramSystem.multiUpload.register(new String[] { "vpMatrix", "projectionMatrix", "viewMatrix" }, VPMatrixSystem);

		ShaderProgramSystem.addUniform(program, ShaderProgramSystem.multiUpload, u);
		ShaderProgramSystem.queueUniformUpload(program, ShaderProgramSystem.multiUpload, u, vpMatrix);

		program.shaderSources.put(GLShaderType.VertexShader, ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/line3D.vert")));
		program.shaderSources.put(GLShaderType.FragmentShader, ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/basic-alpha-discard-colour.frag")));

		try {
			ShaderProgramSystem2.create(glc, program);
		} catch (Exception err) {
			err.printStackTrace();
		}
		return u;
	}

	public UniformManager<String, Integer>.Uniform<ProjectionMatrix> setupUIProgram(GLContext glc, ShaderProgram program, ProjectionMatrixSystem pSystem, ProjectionMatrix pMatrix) {

		UniformManager<String, Integer>.Uniform<ProjectionMatrix> u = ShaderProgramSystem.singleUpload.register("projectionMatrix", pSystem);
		ShaderProgramSystem.addUniform(program, ShaderProgramSystem.singleUpload, u);
		ShaderProgramSystem.queueUniformUpload(program, ShaderProgramSystem.singleUpload, u, pMatrix);
		System.out.println(">");
		program.shaderSources.put(GLShaderType.VertexShader, ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/line2D-UI.vert")));
		program.shaderSources.put(GLShaderType.FragmentShader, ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/basic-alpha-discard-colour.frag")));

		try {
			ShaderProgramSystem2.create(glc, program);
		} catch (Exception err) {
			// TODO Auto-generated catch block
			err.printStackTrace();
		}
		return u;

	}

	public UniformManager<String[], Integer[]>.Uniform<VPMatrix> setupMainProgram(GLContext glc, ShaderProgram program, VPMatrixSystem VPMatrixSystem, VPMatrix vpMatrix) {
		UniformManager<String[], Integer[]>.Uniform<VPMatrix> u = ShaderProgramSystem.multiUpload.register(new String[] { "vpMatrix", "projectionMatrix", "viewMatrix" }, VPMatrixSystem);

		ShaderProgramSystem.addUniform(program, ShaderProgramSystem.multiUpload, u);
		ShaderProgramSystem.queueUniformUpload(program, ShaderProgramSystem.multiUpload, u, vpMatrix);

		program.shaderSources.put(GLShaderType.VertexShader, ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/3D-unlit.vert")));
		program.shaderSources.put(GLShaderType.FragmentShader, ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/basic-alpha-discard-colour.frag")));

		try {
			ShaderProgramSystem2.create(glc, program);
		} catch (Exception err) {

			err.printStackTrace();
		}
		return u;
	}

	public void setupLitProgram(GLContext glc, ShaderProgram program, int maxLights, int maxMaterials) {
		String fragSource = ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/lighting.frag"));
		fragSource = fragSource.replaceAll("\\{maxmats\\}", "" + maxMaterials);
		fragSource = fragSource.replaceAll("\\{maxlights\\}", maxLights + "");
		String vertSource = ShaderProgramSystem.loadShaderSourceFromFile(("resources/shaders/lighting.vert"));
		vertSource = vertSource.replaceAll("\\{maxlights\\}", maxLights + "");
		program.shaderSources.put(GLShaderType.VertexShader, vertSource);
		program.shaderSources.put(GLShaderType.FragmentShader, fragSource);
		try {
			ShaderProgramSystem2.create(glc, program);
		} catch (Exception err) {
			// TODO Auto-generated catch block
			err.printStackTrace();
		}

	}

	public void preRender(GLContext glc, LevelData level, VPMatrix vp, ShaderProgram program) {
		FrustumIntersection fi = new FrustumIntersection(vp.cache);

		Set<Entry<Vector2i, Chunk>> set = level.chunks.entrySet();
		synchronized (level.chunks) {
			for (Iterator<Entry<Vector2i, Chunk>> i = set.iterator(); i.hasNext();) {
				Entry<Vector2i, Chunk> entry = i.next();
				Vector2i loc = entry.getKey();
				Chunk chunk = entry.getValue();
				Vector3f chunkPos = new Vector3f(loc.x * LevelData.fullChunkSize, loc.y * LevelData.fullChunkSize, 0);
				MeshExt m = baked.get(chunk);
				if (m == null || chunk.rebake.getAndSet(false)) {
					logger.warn("dont do anymore");
					if (m != null) {
						m.mesh.singleFrameDiscard = true;
					}
					m = bakeChunk(chunkPos, chunk);
					ShaderProgramSystem2.loadVAO(glc, program, m.mesh);
					m.mesh.visible = false;
					baked.put(chunk, m);
				}
				switch (fi.intersectAab(chunkPos, chunkPos.add(LevelData.fullChunkSize, LevelData.fullChunkSize, 0, new Vector3f()))) {

					case FrustumIntersection.INSIDE:
					case FrustumIntersection.INTERSECT:
						m.mesh.visible = true;
						//render chunk
						break;
					case FrustumIntersection.OUTSIDE:
						m.mesh.visible = false;
						break;
					default:
						break;
				}

			}
		}
		//TODO not drawing axis here
		//		drawAxis(glc, program);

	}

	Map<Chunk, MeshExt> baked = new CollectionSuppliers.MapSupplier<Chunk, MeshExt>().get();
	Map<TileTypes, Map<String, MeshExt>> tileMeshes = new CollectionSuppliers.MapSupplier<TileTypes, Map<String, MeshExt>>().get();

	private void bakeTile(Tile tile) {
		switch (tile.type) {

		}
	}

	private MeshExt bakeChunk(Vector3f chunkPos, Chunk chunk) {
		MeshExt m = new MeshExt();

		WorldRenderer.setupDiscardMesh3D(m, 4);
		m.mesh.modelRenderType = GLDrawMode.TriangleFan;
		m.mesh.name = "chunk_" + (int) chunkPos.x + "_" + (int) chunkPos.y;
		m.mesh.renderCount = 0;

		m.positionAttrib.data.add(0f);
		m.positionAttrib.data.add(0f);
		m.positionAttrib.data.add(0f);

		m.positionAttrib.data.add(LevelData.tileSize);
		m.positionAttrib.data.add(0f);
		m.positionAttrib.data.add(0f);

		m.positionAttrib.data.add(LevelData.tileSize);
		m.positionAttrib.data.add(LevelData.tileSize);
		m.positionAttrib.data.add(0f);

		m.positionAttrib.data.add(0f);
		m.positionAttrib.data.add(LevelData.tileSize);
		m.positionAttrib.data.add(0f);

		//TODO bake chunk instead
		for (int x = 0; x < chunk.tiles.length; x++) {
			for (int y = 0; y < chunk.tiles[x].length; y++) {
				Vector3f tilePos = chunkPos.add(x * LevelData.tileSize, y * LevelData.tileSize, 0, new Vector3f());
				Vector4f colour = new Vector4f();
				Tile tile = chunk.tiles[x][y];
				if (tile == null) {
					chunk.tiles[x][y] = chunk.new Tile();
					tile = chunk.tiles[x][y];
				}
				if (tile.type == null) {
					tile.type = Tiles.Hole;
				}

				switch (tile.type) {
					case Hole:

						break;
					case Ground:

						colour = new Vector4f(0.1f, 1f, 0.1f, 1f);
						FrameUtils.appendToList(m.offsetAttrib.data, tilePos);
						m.colourAttrib.data.add(colour.x);
						m.colourAttrib.data.add(colour.y);
						m.colourAttrib.data.add(colour.z);
						m.colourAttrib.data.add(colour.w);
						m.mesh.renderCount += 1;
						break;

				}

			}
		}
		return m;
	}

	private static final String vPosName = "vPos", colourName = "colour", offsetName = "offset", zIndexName = "zIndex";

	private ShaderProgram.Mesh setup_2D_UI(FloatBuffer vertices, FloatBuffer colours, float zIndex, int count) {
		ShaderProgram.Mesh mesh = new ShaderProgram.Mesh();
		{
			VertexAttribute vertexAttrib = new VertexAttribute();
			vertexAttrib.name = "position";
			vertexAttrib.bufferType = BufferType.ArrayBuffer;
			vertexAttrib.dataType = GLDataType.Float;
			vertexAttrib.bufferUsage = BufferUsage.StaticDraw;
			vertexAttrib.dataSize = 2;
			vertexAttrib.normalised = false;

			vertexAttrib.bufferResourceType = BufferDataManagementType.Buffer;
			vertexAttrib.buffer = vertices;
			mesh.VBOs.add(vertexAttrib);
		}

		{
			VertexAttribute colourAttrib = new VertexAttribute();
			colourAttrib.name = colourName;
			colourAttrib.bufferType = BufferType.ArrayBuffer;
			colourAttrib.dataType = GLDataType.Float;
			colourAttrib.bufferUsage = BufferUsage.DynamicDraw;
			colourAttrib.dataSize = 4;
			colourAttrib.normalised = false;
			colourAttrib.instanced = true;
			colourAttrib.instanceStride = 1;
			colourAttrib.bufferResourceType = BufferDataManagementType.Buffer;
			colourAttrib.buffer = colours;

			mesh.VBOs.add(colourAttrib);
			mesh.addAttribute(colourAttrib);
		}

		{
			VertexAttribute zIndexAttrib = new VertexAttribute();
			zIndexAttrib.name = zIndexName;
			zIndexAttrib.bufferType = BufferType.ArrayBuffer;
			zIndexAttrib.dataType = GLDataType.Float;
			zIndexAttrib.bufferUsage = BufferUsage.DynamicDraw;
			zIndexAttrib.dataSize = 1;
			zIndexAttrib.normalised = false;
			zIndexAttrib.instanced = true;
			zIndexAttrib.instanceStride = 1;
			zIndexAttrib.bufferResourceType = BufferDataManagementType.List;
			zIndexAttrib.data.add(zIndex);

			mesh.VBOs.add(zIndexAttrib);
			mesh.addAttribute(zIndexAttrib);
		}

		{
			VertexAttribute offsetAttrib = new VertexAttribute();
			offsetAttrib.name = offsetName;
			offsetAttrib.bufferType = BufferType.ArrayBuffer;
			offsetAttrib.dataType = GLDataType.Float;
			offsetAttrib.bufferUsage = BufferUsage.DynamicDraw;
			offsetAttrib.dataSize = 2;
			offsetAttrib.normalised = false;
			offsetAttrib.instanced = true;
			offsetAttrib.instanceStride = 1;
			offsetAttrib.bufferResourceType = BufferDataManagementType.List;
			offsetAttrib.data.add(0);
			offsetAttrib.data.add(0);

			mesh.VBOs.add(offsetAttrib);
			mesh.addAttribute(offsetAttrib);
		}

		mesh.vertexCount = count;
		mesh.modelRenderType = GLDrawMode.LineLoop;

		return mesh;

	}

	private ShaderProgram.Mesh setup_3D_nolit_flat_mesh(FloatBuffer vertices, FloatBuffer colours, int count) {
		ShaderProgram.Mesh mesh = new ShaderProgram.Mesh();
		{
			VertexAttribute vertexAttrib = new VertexAttribute();
			vertexAttrib.name = "vertex";
			vertexAttrib.bufferType = BufferType.ArrayBuffer;
			vertexAttrib.dataType = GLDataType.Float;
			vertexAttrib.bufferUsage = BufferUsage.StaticDraw;
			vertexAttrib.dataSize = 3;
			vertexAttrib.normalised = false;

			vertexAttrib.bufferResourceType = BufferDataManagementType.Buffer;
			vertexAttrib.buffer = vertices;

			mesh.VBOs.add(vertexAttrib);
		}

		{
			VertexAttribute colourAttrib = new VertexAttribute();
			colourAttrib.name = "colour";
			colourAttrib.bufferType = BufferType.ArrayBuffer;
			colourAttrib.dataType = GLDataType.Float;
			colourAttrib.bufferUsage = BufferUsage.StaticDraw;
			colourAttrib.dataSize = 4;
			colourAttrib.normalised = false;
			colourAttrib.instanced = false;
			colourAttrib.instanceStride = 1;
			colourAttrib.bufferResourceType = BufferDataManagementType.Buffer;
			colourAttrib.buffer = colours;

			mesh.VBOs.add(colourAttrib);
			mesh.addAttribute(colourAttrib);
		}

		mesh.vertexCount = count;
		mesh.modelRenderType = GLDrawMode.Triangles;

		return mesh;

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

		Mesh x = setup_3D_nolit_flat_mesh(verts, colours, count * 2);
		x.name = "axis";
		x.modelRenderType = GLDrawMode.Line;

		return x;
	}

	private Mesh compasLine() {

		FloatBuffer colour = BufferUtils.createFloatBuffer(4);
		colour.put(GameUI.compasLineColour.x);
		colour.put(GameUI.compasLineColour.y);
		colour.put(GameUI.compasLineColour.z);
		colour.put(GameUI.compasLineColour.w);
		colour.flip();
		ShaderProgram.Mesh mcompas = new ShaderProgram.Mesh();
		{
			VertexAttribute vertexAttrib = new VertexAttribute();
			vertexAttrib.name = "position";
			vertexAttrib.bufferType = BufferType.ArrayBuffer;
			vertexAttrib.dataType = GLDataType.Float;
			vertexAttrib.bufferUsage = BufferUsage.StaticDraw;
			vertexAttrib.dataSize = 2;
			vertexAttrib.normalised = false;

			vertexAttrib.bufferResourceType = BufferDataManagementType.List;
			mcompas.VBOs.add(vertexAttrib);
			mcompas.addAttribute(vertexAttrib);
		}

		{
			VertexAttribute colourAttrib = new VertexAttribute();
			colourAttrib.name = colourName;
			colourAttrib.bufferType = BufferType.ArrayBuffer;
			colourAttrib.dataType = GLDataType.Float;
			colourAttrib.bufferUsage = BufferUsage.DynamicDraw;
			colourAttrib.dataSize = 4;
			colourAttrib.normalised = false;
			colourAttrib.instanced = true;
			colourAttrib.instanceStride = 1;
			colourAttrib.bufferResourceType = BufferDataManagementType.Buffer;
			colourAttrib.buffer = colour;

			mcompas.VBOs.add(colourAttrib);
			mcompas.addAttribute(colourAttrib);
		}

		{
			VertexAttribute zIndexAttrib = new VertexAttribute();
			zIndexAttrib.name = zIndexName;
			zIndexAttrib.bufferType = BufferType.ArrayBuffer;
			zIndexAttrib.dataType = GLDataType.Float;
			zIndexAttrib.bufferUsage = BufferUsage.DynamicDraw;
			zIndexAttrib.dataSize = 1;
			zIndexAttrib.normalised = false;
			zIndexAttrib.instanced = true;
			zIndexAttrib.instanceStride = 1;
			zIndexAttrib.bufferResourceType = BufferDataManagementType.List;
			zIndexAttrib.data.add(-2f);

			mcompas.VBOs.add(zIndexAttrib);
			mcompas.addAttribute(zIndexAttrib);
		}

		{
			VertexAttribute offsetAttrib = new VertexAttribute();
			offsetAttrib.name = offsetName;
			offsetAttrib.bufferType = BufferType.ArrayBuffer;
			offsetAttrib.dataType = GLDataType.Float;
			offsetAttrib.bufferUsage = BufferUsage.DynamicDraw;
			offsetAttrib.dataSize = 2;
			offsetAttrib.normalised = false;
			offsetAttrib.instanced = true;
			offsetAttrib.instanceStride = 1;
			offsetAttrib.bufferResourceType = BufferDataManagementType.List;
			offsetAttrib.data.add(0);
			offsetAttrib.data.add(0);

			mcompas.VBOs.add(offsetAttrib);
			mcompas.addAttribute(offsetAttrib);
		}

		mcompas.vertexCount = 2;
		mcompas.modelRenderType = GLDrawMode.LineLoop;
		mcompas.visible = false;

		//NOTE messy way to push pop line width
		Wrapper<Integer> prevWidth = new WrapperImpl<>();
		/*
		 * mcompas.preRender = new Runnable() {
		 * 
		 * @Override public void run() {
		 * prevWidth.setWrapped(GL46.glGetInteger(GL46.GL_LINE_WIDTH));
		 * GL46.glLineWidth(3f);
		 * 
		 * } }; mcompas.postRender = new Runnable() {
		 * 
		 * @Override public void run() {
		 * GL46.glLineWidth(prevWidth.getWrappedOrDefault(1)); }
		 * 
		 * };
		 */
		return mcompas;

	}

	private Mesh compas() {

		Vector2f[] points = WorldRenderer.generateCircle(new Vector2f(0, 0), GameUI.panRadi, WorldRenderer.circleSegments * 2);
		FloatBuffer verts = BufferUtils.createFloatBuffer(points.length * 2);

		for (Vector2f vert : points) {
			verts.put(vert.x);
			verts.put(vert.y);
		}
		verts.flip();
		FloatBuffer colour = BufferUtils.createFloatBuffer(4);
		colour.put(GameUI.compasColour.x);
		colour.put(GameUI.compasColour.y);
		colour.put(GameUI.compasColour.z);
		colour.put(GameUI.compasColour.w);
		colour.flip();
		Mesh mcompas = setup_2D_UI(verts, colour, -1f, points.length);

		//NOTE messy way to push pop line width
		Wrapper<Integer> prevWidth = new WrapperImpl<>();/*
															 * mcompas.preRender = new Runnable() {
															 * 
															 * @Override public void run() {
															 * prevWidth.setWrapped(GL46.glGetInteger(GL46.GL_LINE_WIDTH));
															 * GL46.glLineWidth(3f);
															 * 
															 * } }; mcompas.postRender = new Runnable() {
															 * 
															 * @Override public void run() {
															 * 
															 * GL46.glLineWidth(prevWidth.getWrappedOrDefault(1)); }
															 * 
															 * };
															 */
		return mcompas;

	}

	private int matrixBuffer;
	private int lightBuffer;
	//	private int materialBuffer;
	private int ambientBrightnessLocation;
	private static final int vpMatrixBindingpoint = 2;
	private static final int lightBufferBindingPoint = 3;
	private static final int materialBufferBindingPoint = 4;
	private static final int maxMats = 2;

	private int storeMatrixBuffer(GLContext glc, int bindingPoint) {
		int matrixBuffer = glc.genUBO(bindingPoint, 64 * 3);

		//binds the buffer to a binding index
		glBindBufferBase(GL_UNIFORM_BUFFER, bindingPoint, matrixBuffer);
		return matrixBuffer;
	}

	private void bindUBONameToIndex(String name, int binding, ShaderProgram... programs) {
		for (ShaderProgram program : programs) {
			int actualIndex = GL46.glGetUniformBlockIndex(program.programID, name);
			//binds the binding index to the interface block (by index)
			glUniformBlockBinding(program.programID, actualIndex, binding);
		}
	}

	private void writeVPMatrix(int buffer, Matrix4f projection, Matrix4f view, Matrix4f vp) {

		//no need to be in a program bidning, since this is shared between multiple programs
		glBindBuffer(GL46.GL_UNIFORM_BUFFER, buffer);
		float[] store = new float[16];

		if (projection != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 0, projection.get(store));
		if (view != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 1, view.get(store));
		if (vp != null)
			glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 2, vp.get(store));

		glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
	}

	private void setupUBOs(GLContext glc, ShaderProgram program, Matrix4f projection, Matrix4f view, Matrix4f vp) {

		glUseProgram(program.programID);
		ambientBrightnessLocation = GL46.glGetUniformLocation(program.programID, "ambientBrightness");
		glUniform1f(ambientBrightnessLocation, 0.125f);
		//-----binding to the view/projection uniform buffer/block-----//
		if (true) {
			this.matrixBuffer = storeMatrixBuffer(glc, vpMatrixBindingpoint);
			writeVPMatrix(matrixBuffer, projection, view, vp);
			bindUBONameToIndex("Matrices", vpMatrixBindingpoint, program);
		}

	}

	public Tickable renderGame(CityBuilderMain cityBuilder, VPMatrix vpMatrix, Entity cameraAnchorEntity, ProjectionMatrix ortho, RayHelper rh, KeyInputSystem keyInput, NkContextSingleton nkContext) {

		//		ShaderProgram mainProgram = new ShaderProgram();
		ShaderProgram program = new ShaderProgram();
		//		ShaderProgram uiProgram = new ShaderProgram();

		ShaderProgram uiProgram2 = new ShaderProgram();

		/**
		 * setup the debug draw program with world axis
		 */
		ShaderProgram debugProgram = new ShaderProgram();

		Mesh axisMesh = drawAxis(100);
		Mesh compas = compas();
		Mesh compasLine = compasLine();

		VPMatrixSystem vpSystem = new VPMatrixSystem();

		Wrapper<UniformManager<String[], Integer[]>.Uniform<VPMatrix>> puW = new WrapperImpl<>();
		//		Wrapper<UniformManager<String, Integer>.Uniform<ProjectionMatrix>> uipuW = new WrapperImpl<>();

		vpMatrix.proj.getWrapped().window = cityBuilder.window;
		vpMatrix.proj.getWrapped().FOV = 90;
		vpMatrix.proj.getWrapped().nearPlane = 0.001f;
		vpMatrix.proj.getWrapped().farPlane = 10000f;
		vpMatrix.proj.getWrapped().orthoAspect = 10f;
		vpMatrix.proj.getWrapped().perspectiveOrOrtho = true;
		vpMatrix.proj.getWrapped().scale = 1f;

		ortho.window = cityBuilder.window;
		ortho.FOV = 90;
		ortho.nearPlane = 0.001f;
		ortho.farPlane = 10000f;
		ortho.orthoAspect = 10f;
		ortho.perspectiveOrOrtho = false;
		ortho.scale = 1f;
		CameraSpringArm arm = vpMatrix.view.getWrapped().springArm;
		cityBuilder.window.events.postCreation.add(() -> {

			puW.setWrapped(setupWorldProgram(cityBuilder.window.glContext, program, vpSystem, vpMatrix));
			//			uipuW.setWrapped(setupUIProgram(cityBuilder.window.glContext, uiProgram, vpSystem.projSystem, ortho));
			VPMatrixSystem.ProjectionMatrixSystem.update(ortho);

			//setup the program
			try {

				setupUBOs(cityBuilder.window.glContext, debugProgram, vpMatrix.proj.getWrapped().cache, vpMatrix.view.getWrapped().cache, vpMatrix.cache);

				{
					String vertSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_nolit_flat.vert"));
					String fragSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_nolit_flat.frag"));
					debugProgram.shaderSources.put(GLShaderType.VertexShader, vertSource);
					debugProgram.shaderSources.put(GLShaderType.FragmentShader, fragSource);

					//setup the program
					try {
						ShaderProgramSystem2.create(cityBuilder.window.glContext, debugProgram);
					} catch (Exception err) {
						err.printStackTrace();
					}
					ShaderProgramSystem2.loadVAO(cityBuilder.window.glContext, debugProgram, axisMesh);

					{
						Random r = new Random(1);

						TreeMap<Float, IslandSize> map = new TreeMap<>();
						float count = 0;
						map.put(count += 1, IslandSize.TINY);
						map.put(count += 3, IslandSize.SMALL);
						map.put(count += 5, IslandSize.MEDIUM);
						map.put(count += 2, IslandSize.BIG);

						System.out.println("starting i");
						float value = r.nextFloat() * count;
						IslandSize size = map.higherEntry(value).getValue();

						//						Island i = new Island(r, "test", size);

						//						i.generate();
						//						int msize = i.map.size();
						//						FloatBuffer vertices = BufferUtils.createFloatBuffer(msize * 3);
						//						FloatBuffer colours = BufferUtils.createFloatBuffer(msize * 4);
						//						i.convertToMesh(vertices, colours, 5, 2);
						//						vertices.flip();
						//						colours.flip();
						//						Mesh mesh = setup_3D_nolit_flat_mesh(vertices, colours, msize);
						//						mesh.modelRenderType = GLDrawMode.Points;

						//						ShaderProgramSystem2.loadVAO(cityBuilder.window.glContext, debugProgram, mesh);

						//							i.print();

					}

					bindUBONameToIndex("Matrices", vpMatrixBindingpoint, debugProgram);
					bindUBONameToIndex("Matrices", vpMatrixBindingpoint, program);
				}

				{

					String vertSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/line2D-UI.vert"));
					String fragSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/basic-alpha-discard-colour.frag"));
					uiProgram2.shaderSources.put(GLShaderType.VertexShader, vertSource);
					uiProgram2.shaderSources.put(GLShaderType.FragmentShader, fragSource);

					//setup the program
					try {
						ShaderProgramSystem2.create(cityBuilder.window.glContext, uiProgram2);
					} catch (Exception err) {
						err.printStackTrace();
					}
					ShaderProgramSystem2.loadVAO(cityBuilder.window.glContext, uiProgram2, compas);
					ShaderProgramSystem2.loadVAO(cityBuilder.window.glContext, uiProgram2, compasLine);

					int location = glGetUniformLocation(uiProgram2.programID, "projectionMatrix");
					GL46.glProgramUniformMatrix4fv(uiProgram2.programID, location, false, ortho.cache.get(new float[16]));
				}

			} catch (Exception err) {
				err.printStackTrace();
			}
			/*mpuW.setWrapped(levelRenderer.setupMainProgram(mainProgram, vpSystem, vpMatrix));
			ShaderProgramSystem.loadVAO(mainProgram, cube);*/

		});

		vpSystem.preMult(vpMatrix);

		cityBuilder.gameUI.init(cityBuilder.window, vpMatrix, ortho, rh);
		return (time) -> {
			GL46.glClear(GL46.GL_COLOR_BUFFER_BIT);
			GL46.glPointSize(2f);
			vpSystem.preMult(vpMatrix);
			ShaderProgramSystem.queueUniformUpload(program, ShaderProgramSystem.multiUpload, puW.getWrapped(), vpMatrix);

			writeVPMatrix(matrixBuffer, vpMatrix.proj.getWrapped().cache, vpMatrix.view.getWrapped().cache, vpMatrix.cache);

			keyInput.tick(cityBuilder.window.mousePressTicks, cityBuilder.window.mousePressMap, time.nanos);
			keyInput.tick(cityBuilder.window.keyPressTicks, cityBuilder.window.keyPressMap, time.nanos);
			if (cityBuilder.level != null) {
				//TODO better testing for if mouse controls should be enabled. eg when over a gui

				cityBuilder.gameUI.handlePanningTick(cityBuilder.window, ortho, vpMatrix.view.getWrapped(), cameraAnchorEntity);
				cityBuilder.gameUI.handlePitchingTick(cityBuilder.window, ortho, arm);
				cityBuilder.gameUI.handleScrollingTick(arm);
				long mouseLeftClick = cityBuilder.window.mousePressTicks.getOrDefault(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0l);
				if (mouseLeftClick > 0) {
					Vector4f cursorRay = CursorHelper.getMouse(SpaceState.World_Space, cityBuilder.window, vpMatrix.proj.getWrapped(), vpMatrix.view.getWrapped());
					rh.update(new Vector3f(cursorRay.x, cursorRay.y, cursorRay.z), new Vector3f(vpMatrix.view.getWrapped().position), cityBuilder);

				}

				//NOTE level clear colour
				cityBuilder.window.clearColour.set(0f, 0f, 0f, 0f);
				//TODO do chunk building for faster mesh drawing
				preRender(cityBuilder.window.glContext, cityBuilder.level, vpMatrix, program);
				cityBuilder.gameUI.preRenderMouseUI(cityBuilder.window, ortho, uiProgram2, compas, compas.getAttribute(offsetName), compasLine, compasLine.getAttribute(vPosName));

			}
			//NOTE uploading the ortho UI projection matrix to the UI program
			//also check if we actually need to do this per frame.. could put it into a window-resize event
			int location = glGetUniformLocation(uiProgram2.programID, "projectionMatrix");
			GL46.glProgramUniformMatrix4fv(uiProgram2.programID, location, false, ortho.cache.get(new float[16]));

			//TODO fix "single frame discarding"
//			ShaderProgramSystem2.render(program);
			//			ShaderProgramSystem2.render(uiProgram);
			try(ShaderClosable prog = ShaderProgramSystem2.useProgram(uiProgram2)) {
				ShaderProgramSystem2.renderMesh(compasLine);
				ShaderProgramSystem2.renderMesh(compas);
				
			}
			try(ShaderClosable prog = ShaderProgramSystem2.useProgram(debugProgram)) {

				ShaderProgramSystem2.renderMesh(axisMesh);
			}
			

			//this is the cube test rendering program
			//						ShaderProgramSystem.render(mainProgram);
			return false;
		};
	}
}
