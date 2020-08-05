package com.meeple.backend.game.world;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.function.BiFunction;

import org.apache.log4j.Logger;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import com.meeple.backend.Model;
import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.ShaderPrograms.Program;
import com.meeple.backend.entity.EntityBase;
import com.meeple.backend.entity.ModelManager;
import com.meeple.backend.events.RegionGenerationEvent;
import com.meeple.backend.events.TerrainGenerationEvent;
import com.meeple.backend.game.world.features.TreeFeature;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.IndexBufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2.ShaderClosable;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;

public class WorldClient {

	private static Logger logger = Logger.getLogger(WorldClient.class);

	private Map<Terrain, Mesh> terrainMeshes = Collections.synchronizedMap(new WeakHashMap<>());
	private Map<EntityBase, Model> visibleEntities = Collections.synchronizedMap(new WeakHashMap<>());
	private BiFunction<Float, Float, TerrainSampleInfo> sampler;

	/**
	 * Basic pair class for ease of use when sending data between threads. only
	 * WorldClient uses this class
	 * 
	 * @author Megan
	 *
	 */
	private class TerrainMeshUpload {
		Terrain terrain;
		Mesh mesh;
	}

	/**
	 * The queue of meshes to upload to OGL context
	 */
	private Queue<TerrainMeshUpload> toUpload = new ConcurrentLinkedQueue<>();

	/*
	 * This is a potential optimisation. not needed when terrains have a low count
	 */
	// protected Map<Terrain, Set<EntityBase>> sortedEntities = new HashMap<>();

	public Set<Mesh> visibleTerrains = Collections.synchronizedSet(new HashSet<>());

	public ModelManager modelManager = new ModelManager();

	/**
	 * Used to check progress when loading a world.
	 */
	private Phaser barrier;
	private final ExecutorService service;

	public WorldClient(BiFunction<Float, Float, TerrainSampleInfo> sampler, ExecutorService service) {
		this.sampler = sampler;
		this.service = service;
	}

	public void setupProgram(GLContext glc) {
		ShaderPrograms.initAndCreate(glc, Program._3D_Unlit_Flat);

		treeModel = new Model();
		treeModel.addMesh(tree);

		TreeFeature tf = new TreeFeature();
		tf.setup();

		tree = tf.mesh;
		tree.getAttribute(ShaderPrograms.transformAtt.name).data(new Matrix4f().get(new float[16]));
		tree.getAttribute(ShaderPrograms.colourAtt.name).data(new float[] { 1, 0, 0, 0 });
		tree.visible = true;

		ShaderProgramSystem2.loadVAO(glc, Program._3D_Unlit_Flat.program, tree);

		modelManager.setup(glc);

	}

	Mesh tree;
	Model treeModel;

	public void cameraCheck(World world, Matrix4f camera, EntityBase... nonRendering) {

		// TODO optimise render check.
		visibleTerrains.clear();

		FrustumIntersection fu = new FrustumIntersection(camera);
		for (Entry<Vector2i, Map<Vector2i, Terrain>> entry : world.getStorage().terrains.entrySet()) {
			Vector2i regionIndex = entry.getKey();
			Map<Vector2i, Terrain> regionData = entry.getValue();
			boolean test = false;
			if (true) {
				int intersectTest = fu.intersectAab(
					regionIndex.x * World.RegionalWorldSize, regionIndex.y * World.RegionalWorldSize, -10, (regionIndex.x + 1) * World.RegionalWorldSize, (regionIndex.y + 1) * World.RegionalWorldSize, 10);
				test = intersectTest == FrustumIntersection.INSIDE || intersectTest == FrustumIntersection.INTERSECT;
			}
			for (Terrain terrain : regionData.values()) {
				synchronized (terrainMeshes) {

					Mesh mesh = terrainMeshes.get(terrain);
					if (mesh != null) {
						if (test) {
							visibleTerrains.add(mesh);

							// } else {
							// visibleTerrains.remove(mesh);
						}
					}
				}

			}
		}
		synchronized (world.entities) {
			for (Iterator<EntityBase> iterator = world.entities.iterator(); iterator.hasNext();) {
				EntityBase entity = iterator.next();
				boolean render = true;
				for (EntityBase eb : nonRendering) {
					if (entity.equals(eb)) {
						render = false;
						break;
					}
				}
				if (render) {
					// is the bounding box of the entity visible
					Vector3f position = entity.transformation().getTranslation(new Vector3f());
					float minX = (2f * entity.bounds().minX) + position.x;
					float minY = (2f * entity.bounds().minY) + position.y;
					float minZ = (2f * entity.bounds().minZ) + position.z;
					float maxX = (2f * entity.bounds().maxX) + position.x;
					float maxY = (2f * entity.bounds().maxY) + position.y;
					float maxZ = (2f * entity.bounds().maxZ) + position.z;
					int intersectTest = fu.intersectAab(minX, minY, minZ, maxX, maxY, maxZ);
					boolean test = intersectTest == FrustumIntersection.INSIDE || intersectTest == FrustumIntersection.INTERSECT;
					if (test) {
						visibleEntities.put(entity, null);
					} else {
						visibleEntities.remove(entity);
					}
				} else {
					visibleEntities.remove(entity);
				}
			}
		}

	}

	public Map<EntityBase, Model> visibleEntities() {
		return visibleEntities;
	}

	public void preRender(GLContext glc) {
		synchronized (toUpload) {
			for (Iterator<TerrainMeshUpload> i = toUpload.iterator(); i.hasNext();) {
				TerrainMeshUpload terrMesh = i.next();
				Terrain terrain = terrMesh.terrain;
				Mesh mesh = terrMesh.mesh;
				ShaderProgramSystem2.loadVAO(glc, Program._3D_Unlit_Flat.program, mesh);
				visibleTerrains.add(mesh);
				terrainMeshes.put(terrain, mesh);
				i.remove();
			}
		}
	}

	public void render() {
		try (ShaderClosable sc = ShaderProgramSystem2.useProgram(Program._3D_Unlit_Flat.program)) {
			for (Mesh terrain : visibleTerrains) {
				ShaderProgramSystem2.tryFullRenderMesh(terrain);
			}
			ShaderProgramSystem2.tryFullRenderMesh(tree);
			modelManager.render(visibleEntities.keySet());
			modelManager.showBounds = true;
			modelManager.showPositions = true;
		}
	}

	public void StartHold() {
		barrier = new Phaser();
		barrier.register();
	}

	public void hold() throws InterruptedException {
		if (barrier != null) {
			barrier.awaitAdvanceInterruptibly(barrier.arrive());
			barrier = null;
		}
	}

	public void free() {
		if (barrier != null) {
			if (barrier.getUnarrivedParties() == 0) {
				barrier.forceTermination();
				barrier = null;
			}
		}
	}

	public float progress() {
		if (barrier != null) {

//			float arrived = barrier.getArrivedParties();
//			float registered = barrier.getRegisteredParties() - 1;
//			logger.info("progress: " + (arrived / registered));

			return ((float) barrier.getArrivedParties() / (float) (barrier.getRegisteredParties() - 1));
		}
		return 1f;
	}

	public void terrainGenerated(TerrainGenerationEvent terrainEvent) {
		/*
		 * Runnable newGen = () -> {
		 * 
		 * Mesh mesh = generator(terrainEvent.getTerrain(), sampler); TerrainMeshUpload
		 * upload = new TerrainMeshUpload(); upload.terrain = terrainEvent.getTerrain();
		 * upload.mesh = mesh; toUpload.add(upload);
		 * 
		 * };
		 * 
		 * if (barrier != null) { barrier.register(); } queuedGenerators.add(newGen);
		 */
	}

	public void regionGenerated(RegionGenerationEvent regionEvent) {

		Collection<Terrain> vals = regionEvent.getRegion().values();
		Spliterator<Terrain> mainSplit = vals.spliterator();
		// Spliterator<Terrain> scndSplit = mainSplit.trySplit();

		logger.info("submitting region " + regionEvent.getRegionIndex());
		Runnable r1 = submit(regionEvent.getRegionIndex(), mainSplit);

		service.execute(r1);/*
							 * Runnable r2 = submit(scndSplit); service.execute(() -> { r2.run(); if
							 * (barrier != null) { barrier.arrive(); } });
							 */
	}

	private Runnable submit(Vector2i key, Spliterator<Terrain> terrains) {
		long size = terrains.getExactSizeIfKnown();
		if (size == -1) {
			size = terrains.estimateSize();
		}
		int fSize = (int) size;

		Runnable result = () ->
		{
			innerSubmit(fSize, key, terrains, barrier);
		};
		if (barrier != null) {
			barrier.bulkRegister((int) fSize);
			logger.info("Now waiting on " + barrier.getRegisteredParties());
		}
		logger.info("Now waiting on " + barrier.getRegisteredParties());
		return result;
	}

	private void innerSubmit(int fSize, Vector2i key, Spliterator<Terrain> terrains, Phaser phaser) {

		long start = System.nanoTime();

		logger.info("Processing " + fSize + " regionional chunks for Region[" + key.x + "." + key.y + "]");
		Wrapper<Integer> actual = new WrapperImpl<>(0);
		terrains.forEachRemaining((terrain) ->
		{
			if (!Thread.currentThread().isInterrupted()) {
				TerrainMeshUpload upload = new TerrainMeshUpload();

				// setup the actual tile storage
				for (int _x = 0; _x < World.TerrainSize; _x++) {
					for (int _y = 0; _y < World.TerrainSize; _y++) {
						terrain.tiles[_x][_y] = this.sampler.apply(terrain.worldX + _x, terrain.worldY + _y);
					}
				}

				Mesh mesh = generator(terrain, sampler);
				upload.terrain = terrain;
				upload.mesh = mesh;

				toUpload.add(upload);

				if (phaser != null) {
					phaser.arrive();
				}
				actual.setWrapped(actual.getWrappedOrDefault(0) + 1);
			} else {
				return;
			}
		});
		long end = System.nanoTime();
		float time = FrameUtils.nanosToSeconds(end - start);
		logger.info("Processed " + actual.getWrapped() + " regionional chunks for Region[" + key.x + "." + key.y + "]\r\n\t\t" + " - took " + time + "s, With an average of " + (time / actual.getWrapped()) + "s per terrain");

	}

	public static Mesh generator(Terrain currentTerrain, BiFunction<Float, Float, TerrainSampleInfo> sampler) {

		int count = World.TerrainVertexCount * World.TerrainVertexCount;

		FloatBuffer vertices = BufferUtils.createFloatBuffer(count * 3);
		FloatBuffer normals = BufferUtils.createFloatBuffer(count * 3);
		FloatBuffer colours = BufferUtils.createFloatBuffer(count * 4);
		int renderCount = 6 * (World.TerrainVertexCount - 1) * (World.TerrainVertexCount - 1);
		IntBuffer indices = BufferUtils.createIntBuffer(renderCount);

		for (int i = 0; i < World.TerrainVertexCount; i++) {

			for (int j = 0; j < World.TerrainVertexCount; j++) {

				float x1 = (float) j / ((float) World.TerrainVertexCount - 1);
				float y1 = (float) i / ((float) World.TerrainVertexCount - 1);
				float x = x1 * World.TerrainSize;
				float y = y1 * World.TerrainSize;
				float sampleX = (currentTerrain.chunkX + x1) * World.TerrainSampleSize;
				float sampleY = (currentTerrain.chunkY + y1) * World.TerrainSampleSize;

				TerrainSampleInfo sample = sampler.apply(sampleX, sampleY);

				float height = sample.height * World.TerrainHeightScale;
				switch (sample.type) {
				case Beach:
					colours.put(0.7f);
					colours.put(0.7f);
					colours.put(0f);
					colours.put(1f);
					break;
				case Ground:
					float colVal = 0.5f + (1.5f * (sample.height - 0.5f));
					colours.put(0f);
					colours.put(colVal);
					colours.put(0f);
					colours.put(1f);
					break;
				case Water:

					colours.put(0f);
					colours.put(0f);
					colours.put(1f);
					colours.put(1f);
					height -= 10f;
					break;
				default:
					break;

				}

				vertices.put(x);
				vertices.put(y);
				vertices.put(height);

				normals.put(0f);
				normals.put(0f);
				normals.put(1f);

				if (i < World.TerrainVertexCount - 1 && j < World.TerrainVertexCount - 1) {
					int topLeft = (i * World.TerrainVertexCount) + j;
					int topRight = topLeft + 1;
					int bottomLeft = ((i + 1) * World.TerrainVertexCount) + j;
					int bottomRight = bottomLeft + 1;
					indices.put(topLeft);
					indices.put(bottomLeft);
					indices.put(topRight);
					indices.put(topRight);
					indices.put(bottomLeft);
					indices.put(bottomRight);
				}

			}
		}
		Mesh mesh = new Mesh().drawMode(GLDrawMode.Triangles).name("terrain" + currentTerrain.worldX + "."
			+ currentTerrain.worldY).vertexCount(renderCount).index(new IndexBufferObject().data(indices).bufferUsage(BufferUsage.StaticDraw).dataSize(3)).addAttribute(ShaderPrograms.vertAtt.build().bufferUsage(BufferUsage.StaticDraw).data(vertices)).addAttribute(ShaderPrograms.colourAtt.build().data(colours)).addAttribute(ShaderPrograms.transformAtt.build().data(new Matrix4f().translate(currentTerrain.worldX, currentTerrain.worldY, 0).get(new float[16])));

		return mesh;

	}

	public ShaderProgram getShaderProgram() {
		return Program._3D_Unlit_Flat.program;
	}

}
