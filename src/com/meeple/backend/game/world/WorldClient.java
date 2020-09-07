package com.meeple.backend.game.world;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.apache.log4j.Logger;
import org.joml.FrustumIntersection;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL46;

import com.meeple.backend.Model;
import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.ShaderPrograms.Program;
import com.meeple.backend.entity.EntityBase;
import com.meeple.backend.entity.ModelManager;
import com.meeple.backend.events.RegionGenerationEvent;
import com.meeple.backend.events.TerrainGenerationEvent;
import com.meeple.backend.game.world.World.Region;
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

public class WorldClient {

	private static Logger logger = Logger.getLogger(WorldClient.class);

	private Map<Terrain, Mesh> terrainMeshes = Collections.synchronizedMap(new WeakHashMap<>());
	private Map<Terrain, Mesh> terrainOutlineMeshes = Collections.synchronizedMap(new WeakHashMap<>());
	private Map<EntityBase, Model> visibleEntities = Collections.synchronizedMap(new WeakHashMap<>());
	//	private BiFunction<Float, Float, TerrainSampleInfo> sampler;
	private boolean showOutlines = true;

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
		public Mesh outline;
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
	public Set<Mesh> visibleTerrainOutlines = Collections.synchronizedSet(new HashSet<>());

	public ModelManager modelManager = new ModelManager();

	/**
	 * Used to check progress when loading a world.
	 */
	private Phaser barrier;
	private final ExecutorService service;

	public WorldClient(BiFunction<Float, Float, TerrainSampleInfo> sampler, ExecutorService service) {
		//		this.sampler = sampler;
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
	private AtomicInteger failedBuilds = new AtomicInteger(0);

	public void cameraCheck(World world, Matrix4f vpMatrix, EntityBase... nonRendering) {

		// TODO optimise render check.
		visibleTerrains.clear();
		visibleTerrainOutlines.clear();

		FrustumIntersection fu = new FrustumIntersection(vpMatrix, false);

		//OPTIMISE by only searching nearby regions rather than all terrains ever
		//currently the bigger the loaded world the longer this check will take
		for (Entry<Vector2i, Region> entry : world.getStorage().regions.entrySet()) {
			Vector2i regionIndex = entry.getKey();
			Region region = entry.getValue();
			Terrain[][] regionData = region.terrains;
			int test = 0;
			if (true) {
				float minX = regionIndex.x * World.RegionalWorldSize;
				float minY = regionIndex.y * World.RegionalWorldSize;
				float maxX = (regionIndex.x + 1) * World.RegionalWorldSize;
				float maxY = (regionIndex.y + 1) * World.RegionalWorldSize;
				test = fu.intersectAab(minX, minY, -100, maxX, maxY, 100);

			}
			if (test == FrustumIntersection.INSIDE || test == FrustumIntersection.INTERSECT) {
				for (int x = 0; x < World.TerrainsPerRegion; x++) {
					for (int y = 0; y < World.TerrainsPerRegion; y++) {

						Terrain terrain = regionData[x][y];
						if (terrain.needsRemesh()) {
							Runnable remeshTerrain = () ->
							{
								genTerrainMesh(terrain);
							};
							Future<?> task = service.submit(remeshTerrain);
							if (false) {
								Object result;
								try {
									result = task.get();
									if (result instanceof Throwable) {
										((Throwable) result).printStackTrace();
									}
								} catch (InterruptedException | ExecutionException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}
						}
						int padding = World.TerrainSize / 2;
						boolean terrainInView = fu.testAab(
							terrain.bounds[0].x - padding, terrain.bounds[0].y - padding, terrain.bounds[0].z - padding, terrain.bounds[1].x + padding, terrain.bounds[1].y + padding, terrain.bounds[1].z + padding);
//						logger.trace("terrain " + terrain.worldX + " " + terrain.worldY + " " + terrainInView);
						if (terrainInView) {

							synchronized (terrainMeshes) {

								Mesh mesh = terrainMeshes.get(terrain);
								if (mesh != null) {
									visibleTerrains.add(mesh);

									// } else {
									// visibleTerrains.remove(mesh);
								}

							}
							synchronized (terrainOutlineMeshes) {
								Mesh outline = terrainOutlineMeshes.get(terrain);
								if (outline != null && showOutlines) {
									visibleTerrainOutlines.add(outline);
								}
							}

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
				Mesh terrainMesh = terrMesh.mesh;
				Mesh outlineMesh = terrMesh.outline;

				ShaderProgramSystem2.loadVAO(glc, Program._3D_Unlit_Flat.program, terrainMesh);
				ShaderProgramSystem2.loadVAO(glc, Program._3D_Unlit_Flat.program, outlineMesh);

				terrainMeshes.put(terrain, terrainMesh);
				terrainOutlineMeshes.put(terrain, outlineMesh);

				i.remove();
			}
		}
	}

	public void render() {
		try (ShaderClosable sc = ShaderProgramSystem2.useProgram(Program._3D_Unlit_Flat.program)) {
//			logger.info("rendering " + visibleTerrains.size() + " terrains");
			for (Mesh terrain : visibleTerrains) {
				ShaderProgramSystem2.tryFullRenderMesh(terrain);
			}
			for (Mesh terrainOutline : visibleTerrainOutlines) {
				GL46.glLineWidth(1f);
				ShaderProgramSystem2.tryFullRenderMesh(terrainOutline);
				GL46.glLineWidth(1f);

				//				ShaderProgramSystem2.tryFullRenderMesh(terrainOutline);
			}
			//			ShaderProgramSystem2.tryFullRenderMesh(tree);
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
		if (regionEvent.getRegion().needsRemesh.get()) {

			logger.info("submitting region " + regionEvent.getRegionIndex());

			Runnable result = null;
			{

				int fSize = (int) World.TerrainsPerRegion * World.TerrainsPerRegion;

				result = () ->
				{
					boolean finished = false;

					long start = System.nanoTime();

					logger.info("Processing " + fSize + " regionional chunks for Region[" + regionEvent.getRegionIndex().x + "." + regionEvent.getRegionIndex().y + "]");
					int actual = 0;

					for (int x = 0; x < World.TerrainsPerRegion; x++) {
						for (int y = 0; y < World.TerrainsPerRegion; y++) {
							Terrain terrain = regionEvent.getRegion().terrains[x][y];

							if (!Thread.currentThread().isInterrupted() && terrain.needsRemesh()) {

								boolean generated = genTerrainMesh(terrain);
								if (generated) {
									actual += 1;
								} else {
									finished &= generated;
								}

							}
							if (barrier != null) {
								barrier.arrive();
							}
						}
					}
					long end = System.nanoTime();
					float time = FrameUtils.nanosToSeconds(end - start);
					logger.info("Processed " + actual + " regionional chunks for Region[" + regionEvent.getRegionIndex().x + "." + regionEvent.getRegionIndex().y + "]\r\n\t\t" +
						" - took " + time + "s, With an average of " + ((float) time / (float) actual) + "s per terrain");

					regionEvent.getRegion().needsRemesh.lazySet(!finished);
				};
				if (barrier != null) {
					barrier.bulkRegister((int) fSize);
					//					logger.info("Now waiting on " + barrier.getRegisteredParties());
				}
			}

			service.execute(result);
		}

	}

	private boolean genTerrainMesh(Terrain terrain) {
		boolean hasMeshed = false;
		if (!Thread.currentThread().isInterrupted() && terrain.needsRemesh()) {
			logger.trace("Generating terrain[" + terrain.worldX + "," + terrain.worldY + "] mesh");
			TerrainMeshUpload upload = new TerrainMeshUpload();

			Mesh terrainMesh = new Mesh();
			Mesh outlineMesh = new Mesh();

			boolean generated = false;
			try {
				generated = generator(terrain, terrainMesh, outlineMesh);
				if (generated) {
					upload.terrain = terrain;
					upload.mesh = terrainMesh;
					upload.outline = outlineMesh;
					toUpload.add(upload);
					terrain.needsRemesh(false);
					logger.trace("Successfully to generate terrain[" + terrain.worldX + "," + terrain.worldY + "] mesh");
				} else {
					logger.trace("Failed to generate terrain[" + terrain.worldX + "," + terrain.worldY + "] mesh");
				}
				hasMeshed = generated;
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		}
		return hasMeshed;
	}

	public static boolean generator(Terrain currentTerrain, Mesh main, Mesh outline) {
		final int vertCount = World.TerrainSampleSize + 1;
		int count = vertCount * vertCount;
		FloatBuffer vertices;
		FloatBuffer normals;
		FloatBuffer colours;
		IntBuffer indices;
		IntBuffer indicesOutline;

		try {
			vertices = BufferUtils.createFloatBuffer(count * 3);
			normals = BufferUtils.createFloatBuffer(count * 3);
			colours = BufferUtils.createFloatBuffer(count * 4);
			int renderCount = 6 * (vertCount - 1) * (vertCount - 1);
			indices = BufferUtils.createIntBuffer(renderCount);

			int renderCountOutine = 6 * (vertCount - 1) * (vertCount - 1);
			indicesOutline = BufferUtils.createIntBuffer(renderCountOutine);
			float minZ = Float.MAX_VALUE;
			float maxZ = Float.MIN_VALUE;

			boolean completed = true;

			for (int i = 0; i < vertCount; i++) {
				if (Thread.currentThread().isInterrupted()) {
					completed = false;
					break;
				}
				for (int j = 0; j < vertCount; j++) {
					if (Thread.currentThread().isInterrupted()) {
						completed = false;
						break;
					}

					float x1 = (float) j / ((float) vertCount - 1);
					float y1 = (float) i / ((float) vertCount - 1);

					float x = x1 * (World.TerrainSize);
					float y = y1 * (World.TerrainSize);

					//[-1] for 0-index arrays, [+1] for terrain overlap
					int tileX = (int) (x1 * (World.TerrainSampleSize - 1 + 1));
					int tileY = (int) (y1 * (World.TerrainSampleSize - 1 + 1));

					TerrainSampleInfo sample = currentTerrain.tiles[tileX][tileY];

					float height = sample.height;
					minZ = Math.min(minZ, height);
					maxZ = Math.max(maxZ, height);

					switch (sample.type) {
					case Beach:
						colours.put(0.7f);
						colours.put(0.7f);
						colours.put(0f);
						colours.put(1f);
						break;
					case Ground:
						float colVal = 0.5f;
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

					if (i < vertCount - 1 && j < vertCount - 1) {
						int topLeft = (i * vertCount) + j;
						int topRight = topLeft + 1;
						int bottomLeft = ((i + 1) * vertCount) + j;
						int bottomRight = bottomLeft + 1;
						indices.put(topLeft);
						indices.put(bottomLeft);
						indices.put(topRight);
						indices.put(topRight);
						indices.put(bottomLeft);
						indices.put(bottomRight);

						indicesOutline.put(topRight);
						indicesOutline.put(topLeft);

						indicesOutline.put(topLeft);
						indicesOutline.put(bottomLeft);

						indicesOutline.put(bottomLeft);
						indicesOutline.put(topRight);
						/*
											indicesOutline.put(topRight);
											indicesOutline.put(bottomRight);
											
											indicesOutline.put(bottomRight);					
											indicesOutline.put(bottomLeft);*/
					}

				}
			}
			if (completed) {
				main.drawMode(GLDrawMode.Triangles);
				main.name("terrain" + currentTerrain.worldX + "." + currentTerrain.worldY);
				main.vertexCount(renderCount);
				main.index(new IndexBufferObject().data(indices).bufferUsage(BufferUsage.StaticDraw).dataSize(3));
				main.addAttribute(ShaderPrograms.vertAtt.build().bufferUsage(BufferUsage.StaticDraw).data(vertices));
				main.addAttribute(ShaderPrograms.colourAtt.build().data(colours));
				main.addAttribute(ShaderPrograms.transformAtt.build().data(new Matrix4f().translate(currentTerrain.worldX, currentTerrain.worldY, 0).get(new float[16])));

				outline.drawMode(GLDrawMode.Line);
				outline.name("terrain_outline_" + currentTerrain.worldX + "." + currentTerrain.worldY);
				outline.vertexCount(renderCountOutine);
				outline.index(new IndexBufferObject().data(indicesOutline).bufferUsage(BufferUsage.StaticDraw).dataSize(3));
				outline.addAttribute(ShaderPrograms.vertAtt.build().bufferUsage(BufferUsage.StaticDraw).data(vertices));
				outline.addAttribute(ShaderPrograms.colourAtt.build().instanced(true, 1).data(new float[] { 0, 0, 0, 1 }));
				outline.addAttribute(ShaderPrograms.transformAtt.build().data(new Matrix4f().translate(currentTerrain.worldX, currentTerrain.worldY, 0).get(new float[16])));

				currentTerrain.bounds[0].z = minZ;
				currentTerrain.bounds[1].z = maxZ;
			}

			return completed;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}

	}

	public ShaderProgram getShaderProgram() {
		return Program._3D_Unlit_Flat.program;
	}

}
