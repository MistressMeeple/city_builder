package com.meeple.backend.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46;

import com.meeple.backend.Model;
import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.ShaderPrograms.Program;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.IndexBufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;

public class ModelManager {

	private Map<Class<? extends EntityBase>, Model> registersModels = new HashMap<>();
	private Map<Class<? extends EntityBase>, Model> currentModels = new HashMap<>();
	public boolean showBounds = true;
	public boolean showPositions = false;

	private final Model bb = new Model();
	private final Model points = new Model();

	public void setup(GLContext glContext) {
		setup();
		bind(glContext);
	}

	public void setup() {
		{
			Mesh pointsMesh = new Mesh();
			pointsMesh.addAttribute(ShaderPrograms.vertAtt.build().data(new float[] { 0, 0, 0 }));
			pointsMesh.addAttribute(ShaderPrograms.colourAtt.build().instanced(true, 1).data(new float[] { 1, 0, 0, 1 }));
			pointsMesh.addAttribute(ShaderPrograms.transformAtt.build());
			pointsMesh.vertexCount = 1;
			pointsMesh.renderMode(GLDrawMode.Points);
			this.points.addMesh(pointsMesh);
			this.points.enableAttributes(ShaderPrograms.InstancedAttribute.Transformation);
		}
		{
			Mesh bbMesh = new Mesh();

			bbMesh.addAttribute(ShaderPrograms.vertAtt.build().data(new float[] { 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1 }));
			int[] iBuff = { 0, 1, 1, 2, 2, 3, 3, 0, 0, 4, 1, 5, 2, 6, 3, 7, 4, 5, 5, 6, 6, 7, 7, 4 };
			bbMesh.addAttribute(ShaderPrograms.colourAtt.build().instanced(true, 1).data(new float[] { 1, 0, 0, 1 }));
			bbMesh.addAttribute(ShaderPrograms.transformAtt.build().data(new Matrix4f().get(new float[16])));
			IndexBufferObject ibo = new IndexBufferObject().bufferUsage(BufferUsage.StaticDraw).data(iBuff);
			bbMesh.index(ibo);
			bbMesh.vertexCount = 8 * 3;
			bbMesh.renderMode(GLDrawMode.Line);

			bb.addMesh(bbMesh.renderMode(GLDrawMode.Line));
			bb.addInstance(new Matrix4f(), new Vector4f(1, 0, 0, 1));
			bb.enableAttributes(ShaderPrograms.InstancedAttribute.Colour, ShaderPrograms.InstancedAttribute.Transformation);

		}

	}

	public void bind(GLContext glContext) {
		points.loadVAOs(glContext, Program._3D_Unlit_Flat.program);
		bb.loadVAOs(glContext, Program._3D_Unlit_Flat.program);
	}

	public void register(Class<? extends EntityBase> entity, Model model) {
		registersModels.put(entity, model);
	}

	public void render(Set<EntityBase> entities) {
		points.setupBuffers(entities.size());
		bb.setupBuffers(entities.size());
		this.currentModels.clear();
		Map<Model, Integer> renderCounts = new HashMap<>();

		synchronized (entities) {
			for (Iterator<EntityBase> iterator = entities.iterator(); iterator.hasNext();) {
				EntityBase entity = iterator.next();
				if (showPositions) {
					// point start
					{
						if (points.transformMatricies() != null) {
							FrameUtils.appendToBuffer(points.transformMatricies(), entity.transformation());
						}
						if (points.colours() != null) {
							FrameUtils.appendToBuffer(points.colours(), new Vector4f());
						}
						if (points.normalMatricies() != null) {
							FrameUtils.appendToBuffer(points.normalMatricies(), FrameUtils.normalFromTransform(entity.transformation()));

						}
					}
					// point end
				}
				if (showBounds) {
					// bounding box start
					{
						if (bb.transformMatricies() != null) {
							Matrix4f transform = new Matrix4f();

							float w = entity.bounds().maxX - entity.bounds().minX;
							float d = entity.bounds().maxY - entity.bounds().minY;
							float h = entity.bounds().maxZ - entity.bounds().minZ;
							transform.scale(w, d, h);

							Vector3f pos = entity.transformation().getTranslation(new Vector3f());
							transform.setTranslation(pos.x + entity.bounds().minX, pos.y + entity.bounds().minY, pos.z + entity.bounds().minZ);

							FrameUtils.appendToBuffer(bb.transformMatricies(), transform);

						}
						if (bb.colours() != null) {
							FrameUtils.appendToBuffer(bb.colours(), new Vector4f());
						}
						if (bb.normalMatricies() != null) {
							FrameUtils.appendToBuffer(bb.normalMatricies(), FrameUtils.normalFromTransform(entity.transformation()));
						}
					}
				}
				{
					// models start
					try {
						Class<? extends EntityBase> clazz = entity.getClass();
						Model model = currentModels.get(clazz);
						if (model == null) {
							model = registersModels.get(clazz);
							model.setupBuffers(entities.size());
							currentModels.put(clazz, model);
						}
						int current = renderCounts.getOrDefault(model, 0);
						renderCounts.put(model, current + 1);

						if (model.transformMatricies() != null) {
							FrameUtils.appendToBuffer(model.transformMatricies(), entity.transformation());
						}
						if (model.colours() != null) {
							FrameUtils.appendToBuffer(model.colours(), new Vector4f(1,0,0,1));
						}
						if (model.normalMatricies() != null) {
							FrameUtils.appendToBuffer(model.normalMatricies(), FrameUtils.normalFromTransform(entity.transformation()));
						}
					} catch (Exception e) {
//						e.printStackTrace();

					}

					// models end
				}

			}
		}

		bb.flipAndUpload(entities.size());
		points.flipAndUpload(entities.size());
		for (Model model : currentModels.values()) {
			model.flipAndUpload(renderCounts.get(model));
			model.render();
		}

		int oldPointSize = GL46.glGetInteger(GL46.GL_POINT_SIZE);
		GL46.glPointSize(10f);

		if (this.showBounds) {
			bb.render();
		}
		if (this.showPositions) {
			points.render();
		}
		GL46.glPointSize(oldPointSize);
	}

}
