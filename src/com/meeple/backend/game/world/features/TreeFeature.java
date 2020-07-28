package com.meeple.backend.game.world.features;

import static org.lwjgl.util.par.ParShapes.par_shapes_create_lsystem;
import static org.lwjgl.util.par.ParShapes.par_shapes_rotate;

import org.lwjgl.util.par.ParShapesMesh;

import com.meeple.backend.game.world.TerrainFeature;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;

public class TreeFeature extends TerrainFeature implements ParShapeMeshI {

	private int slices = 32,
		stacks = 32,
		seed = 1,
		subdivisions = 4;
	public Mesh mesh;

	public void setup() {
		mesh = convert(generate());
	}

	@Override
	public ParShapesMesh generate() {
		String program =
			" sx 2 sy 2" +
				" ry 90 rx 90" +
				" shape tube rx 15  call rlimb rx -15" +
				" shape tube rx -15 call llimb rx 15" +
				" shape tube ry 15  call rlimb ry -15" +
				" shape tube ry 15  call llimb ry -15" +
				" rule rlimb" +
				"     sx 0.925 sy 0.925 tz 1 rx 1.2" +
				"     call rlimb2" +
				" rule rlimb2.1" +
				"     shape connect" +
				"     call rlimb" +
				" rule rlimb2.1" +
				"     rx 15  shape tube call rlimb rx -15" +
				"     rx -15 shape tube call llimb rx 15" +
				" rule rlimb.1" +
				"     call llimb" +
				" rule llimb.1" +
				"     call rlimb" +
				" rule llimb.10" +
				"     sx 0.925 sy 0.925" +
				"     tz 1" +
				"     rx -1.2" +
				"     shape connect" +
				"     call llimb";

		ParShapesMesh pmesh = par_shapes_create_lsystem(program, slices, 60);
		//correct to z up
		par_shapes_rotate(pmesh, (float) Math.toRadians(90), new float[] { 1, 0, 0 });

		return pmesh;
	}
}
