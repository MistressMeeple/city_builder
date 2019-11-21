package com.meeple.citybuild.client.render;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joml.FrustumIntersection;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.meeple.citybuild.client.render.WorldRenderer.MeshExt;
import com.meeple.citybuild.server.LevelData;
import com.meeple.citybuild.server.LevelData.Chunk;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.GLShaderType;
import com.meeple.shared.frame.OGL.UniformManager;
import com.meeple.shared.frame.camera.VPMatrixSystem;
import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem;
import com.meeple.shared.frame.camera.VPMatrixSystem.ProjectionMatrixSystem.ProjectionMatrix;
import com.meeple.shared.frame.camera.VPMatrixSystem.VPMatrix;

public class LevelRenderer {
	public static Logger logger = Logger.getLogger(LevelRenderer.class);

	static class CubeMesh {
		Attribute colourAttrib = new Attribute();
		Attribute translationAttrib = new Attribute();
	}

	public static boolean disableAlphaTest = false;

	public UniformManager<String[], Integer[]>.Uniform<VPMatrix> setupWorldProgram(ShaderProgram program, VPMatrixSystem VPMatrixSystem, VPMatrix vpMatrix) {
		UniformManager<String[], Integer[]>.Uniform<VPMatrix> u = RenderingMain.multiUpload.register(new String[] { "vpMatrix", "projectionMatrix", "viewMatrix" }, VPMatrixSystem);

		RenderingMain.system.addUniform(program, RenderingMain.multiUpload, u);
		RenderingMain.system.queueUniformUpload(program, RenderingMain.multiUpload, u, vpMatrix);

		program.shaderSources.put(GLShaderType.VertexShader, RenderingMain.system.loadShaderSourceFromFile(("resources/shaders/line3D.vert")));
		program.shaderSources.put(GLShaderType.FragmentShader, RenderingMain.system.loadShaderSourceFromFile(("resources/shaders/basic-alpha-discard-colour.frag")));

		RenderingMain.system.create(program);
		return u;
	}

	public UniformManager<String, Integer>.Uniform<ProjectionMatrix> setupUIProgram(ShaderProgram program, ProjectionMatrixSystem pSystem, ProjectionMatrix pMatrix) {

		UniformManager<String, Integer>.Uniform<ProjectionMatrix> u = RenderingMain.singleUpload.register("projectionMatrix", pSystem);
		RenderingMain.system.addUniform(program, RenderingMain.singleUpload, u);
		RenderingMain.system.queueUniformUpload(program, RenderingMain.singleUpload, u, pMatrix);

		program.shaderSources.put(GLShaderType.VertexShader, RenderingMain.system.loadShaderSourceFromFile(("resources/shaders/line2D-UI.vert")));
		program.shaderSources.put(GLShaderType.FragmentShader, RenderingMain.system.loadShaderSourceFromFile(("resources/shaders/basic-alpha-discard-colour.frag")));

		RenderingMain.system.create(program);
		return u;

	}

	public UniformManager<String[], Integer[]>.Uniform<VPMatrix> setupMainProgram(ShaderProgram program, VPMatrixSystem VPMatrixSystem, VPMatrix vpMatrix) {
		UniformManager<String[], Integer[]>.Uniform<VPMatrix> u = RenderingMain.multiUpload.register(new String[] { "vpMatrix", "projectionMatrix", "viewMatrix" }, VPMatrixSystem);

		RenderingMain.system.addUniform(program, RenderingMain.multiUpload, u);
		RenderingMain.system.queueUniformUpload(program, RenderingMain.multiUpload, u, vpMatrix);

		program.shaderSources.put(GLShaderType.VertexShader, RenderingMain.system.loadShaderSourceFromFile(("resources/shaders/3D-unlit.vert")));
		program.shaderSources.put(GLShaderType.FragmentShader, RenderingMain.system.loadShaderSourceFromFile(("resources/shaders/basic-alpha-discard-colour.frag")));

		RenderingMain.system.create(program);
		return u;
	}

	public void preRender(LevelData level, VPMatrix vp, ShaderProgram program) {
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
					if (m != null) {
						m.mesh.singleFrameDiscard = true;
					}
					m = bakeChunk(chunkPos, chunk);
					RenderingMain.system.loadVAO(program, m.mesh);
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
		drawAxis(program);

	}

	Map<Chunk, MeshExt> baked = new HashMap<>();

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
				switch (chunk.tiles[x][y].type) {
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
					case Other:
						colour = new Vector4f(0f, 0f, 1f, 1f);
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

	private void drawAxis(ShaderProgram program) {

		{

			Vector4f colour = new Vector4f(1, 0, 0, 1);
			MeshExt m = new MeshExt();
			WorldRenderer.setupDiscardMesh3D(m, 2);
			m.mesh.singleFrameDiscard = true;

			m.positionAttrib.data.add(0f);
			m.positionAttrib.data.add(0f);
			m.positionAttrib.data.add(0f);

			m.positionAttrib.data.add(100f);
			m.positionAttrib.data.add(0f);
			m.positionAttrib.data.add(0f);

			m.colourAttrib.data.add(colour.x);
			m.colourAttrib.data.add(colour.y);
			m.colourAttrib.data.add(colour.z);
			m.colourAttrib.data.add(colour.w);
			FrameUtils.appendToList(m.offsetAttrib.data, new Vector3f());
			m.mesh.name = "model";
			m.mesh.modelRenderType = GLDrawMode.Line;
			RenderingMain.system.loadVAO(program, m.mesh);
		}
		{

			Vector4f colour = new Vector4f(0, 1, 0, 1);
			MeshExt m = new MeshExt();
			WorldRenderer.setupDiscardMesh3D(m, 2);

			m.positionAttrib.data.add(0f);
			m.positionAttrib.data.add(0f);
			m.positionAttrib.data.add(0f);

			m.positionAttrib.data.add(0f);
			m.positionAttrib.data.add(100f);
			m.positionAttrib.data.add(0f);

			m.colourAttrib.data.add(colour.x);
			m.colourAttrib.data.add(colour.y);
			m.colourAttrib.data.add(colour.z);
			m.colourAttrib.data.add(colour.w);
			FrameUtils.appendToList(m.offsetAttrib.data, new Vector3f());
			m.mesh.name = "model";
			m.mesh.modelRenderType = GLDrawMode.Line;
			RenderingMain.system.loadVAO(program, m.mesh);
		}
		{

			Vector4f colour = new Vector4f(0, 0, 1, 1);
			MeshExt m = new MeshExt();
			WorldRenderer.setupDiscardMesh3D(m, 2);

			m.positionAttrib.data.add(0f);
			m.positionAttrib.data.add(0f);
			m.positionAttrib.data.add(0f);

			m.positionAttrib.data.add(0f);
			m.positionAttrib.data.add(0f);
			m.positionAttrib.data.add(100f);

			m.colourAttrib.data.add(colour.x);
			m.colourAttrib.data.add(colour.y);
			m.colourAttrib.data.add(colour.z);
			m.colourAttrib.data.add(colour.w);
			FrameUtils.appendToList(m.offsetAttrib.data, new Vector3f());
			m.mesh.name = "model";
			m.mesh.modelRenderType = GLDrawMode.Line;
			RenderingMain.system.loadVAO(program, m.mesh);
		}
	}

}
