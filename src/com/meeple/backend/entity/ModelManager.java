package com.meeple.backend.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL46;

import com.meeple.backend.Model;
import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.ShaderPrograms.Program;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.IndexBufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;

public class ModelManager {

	private static Supplier<TreeMap<Float, Model>> supplier = () ->
	{
		return new TreeMap<>();
	};

	private Map<Class<? extends EntityBase>, TreeMap<Float, Model>> registersModels = new HashMap<>();

	/**
	 * Per frame models used and sent of to render.
	 */
	private Set<Model> currentModels = new CollectionSuppliers.SetSupplier<Model>().get();
	public boolean showBounds = true;
	public boolean showPositions = false;

	/**
	 * The model for the bounding box of entities. if enabled in the class this will
	 * put a
	 */
	private final Model bb = new Model();
	/**
	 * This model is just a point that shows the actual position of an entity.
	 * {@link EntityBase#transformation()} This is very useful for models that arent
	 * showing, or when confuguring bounding boxes properly compared to the location
	 * of the entity
	 */
	private final Model points = new Model();

	/**
	 * 
	 * <b>This is optional if you do not intend to use the bounding box or
	 * points.</b> This sets up and loads the bounding-box and poitns models to the
	 * shader program.
	 * 
	 * @param glContext to load the VAO's with
	 */
	public void setup(GLContext glContext) {

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

			// hard coded cube
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
		points.loadVAOs(glContext, Program._3D_Unlit_Flat.program);
		bb.loadVAOs(glContext, Program._3D_Unlit_Flat.program);

	}

	/**
	 * Register an {@link EntityBase} class to a Model. Calling this again with the
	 * same class will add a variation option for a model.
	 * 
	 * @param entityClass Class to bind a model to
	 * @param model       Model to be bound
	 */
	public void register(Class<? extends EntityBase> entityClass, Model model) {
		register(entityClass, model, 1);
	}

	/**
	 * Register an {@link EntityBase} class to a Model. Calling this again with the
	 * same class will add a variation option for a model.
	 * 
	 * @param entityClass Class to bind a model to
	 * @param model       Model to be bound
	 * @param weight      the weight of the variation to use
	 */
	public void register(Class<? extends EntityBase> entityClass, Model model, float weight) {
		TreeMap<Float, Model> entry = registersModels.get(entityClass);
		if (entry == null) {
			entry = supplier.get();
			registersModels.put(entityClass, entry);
		}
		Float max;
		try {
			max = entry.lastKey();
		} catch (Exception e) {
			max = 0f;
		}
		entry.put(max + weight, model);
	}

	public void render(Set<EntityBase> entities) {
		// cache the options in case they change mid render
		boolean _showPositions = showPositions;
		boolean _showBounds = showBounds;

		// no need to set up buffers if not using positions
		if (_showPositions) {
			points.setupBuffers(entities.size());
		}
		if (_showBounds) {
			bb.setupBuffers(entities.size());
		}
		this.currentModels.clear();
		Map<Model, Integer> renderCounts = new HashMap<>();

		synchronized (entities) {
			for (Iterator<EntityBase> iterator = entities.iterator(); iterator.hasNext();) {
				EntityBase entity = iterator.next();
				if (_showPositions) {
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
				if (_showBounds) {
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

						// get the model from the ones used in this frame
						Model model = chooseModel(entity, clazz);
						if (!currentModels.contains(model)) {
							model.setupBuffers(entities.size());
							currentModels.add(model);
						}
						int current = renderCounts.getOrDefault(model, 0);
						renderCounts.put(model, current + 1);

						if (model.transformMatricies() != null) {
							FrameUtils.appendToBuffer(model.transformMatricies(), entity.transformation());
						}
						if (model.colours() != null) {
							FrameUtils.appendToBuffer(model.colours(), new Vector4f(1, 0, 0, 1));
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

		// only upload if enabled
		if (_showPositions) {
			points.flipAndUpload(entities.size());
		}
		if (_showBounds) {
			bb.flipAndUpload(entities.size());
		}

		for (Model model : currentModels) {
			model.flipAndUpload(renderCounts.get(model));
			model.render();
		}

		if (_showBounds) {
			bb.render();
		}
		if (_showPositions) {
			int oldPointSize = GL46.glGetInteger(GL46.GL_POINT_SIZE);
			GL46.glPointSize(10f);

			points.render();
			GL46.glPointSize(oldPointSize);
		}
	}

	private Model chooseModel(EntityBase entity, Class<? extends EntityBase> clazz) {

		TreeMap<Float, Model> modelMap = registersModels.get(clazz);

		Model result = null;
		result = modelMap.firstEntry().getValue();
		if (modelMap.size() > 1) {
			float max = modelMap.lastKey();
			Random random = new Random(entity.UUID().getLeastSignificantBits());
			float rndFloat = random.nextFloat();
			Entry<Float, Model> entry = modelMap.ceilingEntry(rndFloat * max);
			if (entry != null) {
				result = entry.getValue();
			}

		}
		return result;

	}
}
