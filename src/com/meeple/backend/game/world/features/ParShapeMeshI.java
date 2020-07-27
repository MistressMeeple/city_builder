package com.meeple.backend.game.world.features;

import org.lwjgl.util.par.ParShapesMesh;

import com.meeple.backend.ShaderPrograms;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.IndexBufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;

public interface ParShapeMeshI {

	public ParShapesMesh generate();

	public default Mesh convert(ParShapesMesh meshIn) {

		Mesh mesh = new Mesh();
		int count = meshIn.npoints() * 3;
		mesh.addAttribute(ShaderPrograms.vertAtt.build().data(meshIn.points(count)));
		mesh.addAttribute(ShaderPrograms.colourAtt.build().instanced(true, 1));// .data(new float[] { 1, 0, 0, 1 }));
		mesh.addAttribute(ShaderPrograms.transformAtt.build());// .data(new Matrix4f().get(new float[16])));
		
		/*
		 * ParShapes.par_shapes_compute_normals(meshIn); FloatBuffer normals =
		 * meshIn.normals(mesh.vertexCount * 3); if (normals != null) {
		 * glBufferSubData(GL_ARRAY_BUFFER, mesh.vertexCount * (3 + 0) * 4, normals);
		 * glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, mesh.vertexCount * (3 + 0) *
		 * 4); }
		 */

		int tc = meshIn.ntriangles();
		IndexBufferObject ibo = new IndexBufferObject().bufferUsage(BufferUsage.StaticDraw).data(meshIn.triangles(tc * 3));

		mesh.index(ibo);
		mesh.vertexCount = tc * 3 * 3;

		return mesh;
	}
}
