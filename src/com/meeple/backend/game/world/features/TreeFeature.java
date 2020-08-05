package com.meeple.backend.game.world.features;

import static org.lwjgl.util.par.ParShapes.par_shapes_create_lsystem;

import java.util.Map;
import java.util.Random;

import org.lwjgl.util.par.ParShapes;
import org.lwjgl.util.par.ParShapesMesh;

import com.meeple.backend.Model;
import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.game.world.TerrainFeature;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.IndexBufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;

public class TreeFeature extends TerrainFeature implements ParShapeMeshI {

	public static enum TreeLeafType {
		Round, Pyramid, Oval, VShaped, Column, Generated;
	}

	Model finalModel;
	Map<ParShapeMeshI, Integer> parMeshes = new CollectionSuppliers.MapSupplier<ParShapeMeshI, Integer>().get();
	

	private abstract class TreePart implements ParShapeMeshI {
	}

	private abstract class TreeLeaveMesh extends TreePart {
	}

	private abstract class TreeTrunkMesh extends TreePart {
	}

	public class TrerLeaveMesh_Round extends TreeLeaveMesh {

		@Override
		public ParShapesMesh generate() {

			ParShapesMesh leaves_pmesh = ParShapes.par_shapes_create_subdivided_sphere(1);
			return leaves_pmesh;
		}

		@Override
		public Mesh convert(ParShapesMesh meshIn) {
			
			Mesh mesh = new Mesh();
			int count = meshIn.npoints() * 3;
			mesh.addAttribute(ShaderPrograms.vertAtt.build().data(meshIn.points(count)));
			mesh.addAttribute(ShaderPrograms.colourAtt.build().instanced(true, 1).data(new float[] { 1, 0, 0, 1 }));
			mesh.addAttribute(ShaderPrograms.transformAtt.build());// .data(new Matrix4f().get(new float[16])));


			int tc = meshIn.ntriangles();
			IndexBufferObject ibo = new IndexBufferObject().bufferUsage(BufferUsage.StaticDraw).data(meshIn.triangles(tc * 3));

			mesh.index(ibo);
			mesh.vertexCount = tc * 3 * 3;
			return mesh;
		}
	}

	public class TreeTrunkMesh_Normal extends TreeTrunkMesh {

		@Override
		public ParShapesMesh generate() {
			ParShapesMesh trunk_pmesh = ParShapes.par_shapes_create_cylinder(32, 3);
			return trunk_pmesh;
		}

	}


	public Mesh mesh;

	public TreeFeature() {

	}

	public void setup() {
		ParShapesMesh pmesh = generate();
		mesh = convert(pmesh);
		mesh.name= "tree";
		ParShapes.par_shapes_free_mesh(pmesh);
		finalModel = new Model();
		finalModel.addMesh(mesh);

	}

	@Override
	public ParShapesMesh generate() {
		ParShapesMesh trunk = ParShapes.par_shapes_create_cylinder(16, 4);
		ParShapesMesh leaves = ParShapes.par_shapes_create_subdivided_sphere(2);
		ParShapes.par_shapes_scale(trunk, 0.2f, 0.2f, 2);
		ParShapes.par_shapes_scale(leaves, 1, 1, 2);
		ParShapes.par_shapes_translate(leaves, -0f, -0f, 2.5f);
		ParShapes.par_shapes_translate(trunk, 0, 0, -1);
		ParShapes.par_shapes_merge_and_free(trunk, leaves);
		return trunk;
	}

	private static ParShapesMesh Generated() {
		Random random = new Random(1);// ThreadLocalRandom.current();
		if (true) {
			int limbs = random.nextInt(4) + 2;
			for (int i = 0; i < limbs; i++) {
				boolean rot = random.nextBoolean();
				int rotX = 15;// random.nextInt(30) - 15;
				int rotY = 0;// random.nextInt(30) - 15;

				float branchHeight = i + (random.nextFloat() * 2 - 1);
			}
		}

		String program = " " +
			" sx 2 sy 2" +
			" ry 90 rx 90"
		// " sz " + treeHeight + " shape tube sz 0.1" +
		// " tz " + treeHeight;
		;

		int limbs = random.nextInt(4) + 2;
		if (false) {
			for (int i = 0; i < limbs; i++) {
				boolean rot = random.nextBoolean();
				int rotX = 15;// random.nextInt(30) - 15;
				int rotY = 0;// random.nextInt(30) - 15;

				if (rot) {
					rotX = 0;
					rotY = 15;
				}

				boolean rLimb = random.nextBoolean();
				String negRX = "rx " + (rotX < 0 ? rotX : "-" + rotX);
				String negRY = "ry " + (rotY < 0 ? rotY : "-" + rotY);
				String rule = " shape tube rx " + rotX + " ry " + rotY + " call " + (rLimb ? "rlimb" : "llimb") + " " + negRX + " " + negRY;
				program += rule;
			}
		} else {

			double rotPerLimb = 15;// Math.toRadians(360f / (float) limbs);
			for (int i = 0; i < limbs; i++) {
				program += " tz 5 shape tube rx " + rotPerLimb + " call test  ";
			}

		}

		program += " rule test" +
			"  sx 0.9 sy 0.9 sz 1.1 tz 4 rx 1.2 " +
			" shape connect ";
		ParShapesMesh pmesh = par_shapes_create_lsystem(program, 32, 60);
		ParShapes.par_shapes_export(pmesh, "lsystem.obj");
		// correct to z up
		// par_shapes_rotate(pmesh, (float) Math.toRadians(90), new float[] { 1, 0, 0
		// });

		return pmesh;
	}

}
