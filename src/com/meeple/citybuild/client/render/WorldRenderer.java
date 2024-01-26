package com.meeple.citybuild.client.render;

import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferDataManagementType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.RenderableVAO;

public class WorldRenderer extends RendererBase {

	public static class MeshExt {
		public Attribute positionAttrib = new Attribute();
		public Attribute colourAttrib = new Attribute();
		public Attribute offsetAttrib = new Attribute();
		public Attribute rotationAttrib = new Attribute();
		public Attribute zIndexAttrib = new Attribute();
		public RenderableVAO mesh = new RenderableVAO();
	}

	/*
	
		public static void setupQuadModel(MeshExt meshExt, boolean canRotate) {
	
			meshExt.positionAttrib.name = "offset";
			meshExt.positionAttrib.bufferType = BufferType.ArrayBuffer;
			meshExt.positionAttrib.dataType = GLDataType.Float;
			meshExt.positionAttrib.bufferUsage = BufferUsage.DynamicDraw;
			meshExt.positionAttrib.dataSize = 2;
			meshExt.positionAttrib.normalised = false;
			meshExt.positionAttrib.instanced = true;
			meshExt.positionAttrib.instanceStride = 1;
			meshExt.mesh.VBOs.add(meshExt.positionAttrib);
	
			meshExt.colourAttrib.name = "colour";
			meshExt.colourAttrib.bufferType = BufferType.ArrayBuffer;
			meshExt.colourAttrib.dataType = GLDataType.Float;
			meshExt.colourAttrib.bufferUsage = BufferUsage.DynamicDraw;
			meshExt.colourAttrib.dataSize = 4;
			meshExt.colourAttrib.normalised = false;
			meshExt.colourAttrib.instanced = true;
			meshExt.colourAttrib.instanceStride = 1;
			meshExt.mesh.VBOs.add(meshExt.colourAttrib);
	
			if (canRotate) {
				meshExt.rotationAttrib.name = "rotation";
				meshExt.rotationAttrib.bufferType = BufferType.ArrayBuffer;
				meshExt.rotationAttrib.dataType = GLDataType.Float;
				meshExt.rotationAttrib.bufferUsage = BufferUsage.DynamicDraw;
				meshExt.rotationAttrib.dataSize = 1;
				meshExt.rotationAttrib.normalised = false;
				meshExt.rotationAttrib.instanced = true;
				meshExt.rotationAttrib.instanceStride = 1;
				meshExt.mesh.VBOs.add(meshExt.rotationAttrib);
			}
	
			meshExt.mesh.vertexCount = 1;
			meshExt.mesh.modelRenderType = GLDrawMode.Points;
		}
	
		public static void setupPolyMesh(MeshExt meshExt, int vertices) {
	
			meshExt.positionAttrib.name = "position";
			meshExt.positionAttrib.bufferType = BufferType.ArrayBuffer;
			meshExt.positionAttrib.dataType = GLDataType.Float;
			meshExt.positionAttrib.bufferUsage = BufferUsage.DynamicDraw;
			meshExt.positionAttrib.dataSize = 2;
			meshExt.positionAttrib.normalised = false;
			meshExt.mesh.VBOs.add(meshExt.positionAttrib);
	
			meshExt.colourAttrib.name = "colour";
			meshExt.colourAttrib.bufferType = BufferType.ArrayBuffer;
			meshExt.colourAttrib.dataType = GLDataType.Float;
			meshExt.colourAttrib.bufferUsage = BufferUsage.DynamicDraw;
			meshExt.colourAttrib.dataSize = 4;
			meshExt.colourAttrib.normalised = false;
			meshExt.mesh.VBOs.add(meshExt.colourAttrib);
	
			meshExt.rotationAttrib.name = "rotation";
			meshExt.rotationAttrib.bufferType = BufferType.ArrayBuffer;
			meshExt.rotationAttrib.dataType = GLDataType.Float;
			meshExt.rotationAttrib.bufferUsage = BufferUsage.DynamicDraw;
			meshExt.rotationAttrib.dataSize = 1;
			meshExt.rotationAttrib.normalised = false;
			meshExt.rotationAttrib.instanced = true;
			meshExt.rotationAttrib.instanceStride = 1;
			//		meshExt.mesh.VBOs.add(meshExt.rotationAttrib);
	
			meshExt.mesh.vertexCount = vertices;
			if (vertices == 1) {
				meshExt.mesh.modelRenderType = GLDrawMode.Points;
			} else if (vertices == 2) {
				meshExt.mesh.modelRenderType = GLDrawMode.Line;
			} else {
				meshExt.mesh.modelRenderType = GLDrawMode.LineLoop;
			}
		}
	*/
	public static void setupDiscardMesh(MeshExt meshExt, int vertices) {
		meshExt.positionAttrib = ShaderProgramDefinitions.MeshAttributeGenerator.generateVertexAttribute();
		meshExt.positionAttrib.name = "position";
		//meshExt.positionAttrib.bufferType = BufferType.ArrayBuffer;
		//meshExt.positionAttrib.dataType = GLDataType.Float;
		//meshExt.positionAttrib.bufferUsage = BufferUsage.StreamDraw;
		meshExt.positionAttrib.dataSize = 2;
		//meshExt.positionAttrib.normalised = false;
		meshExt.mesh.VBOs.add(meshExt.positionAttrib);

		meshExt.colourAttrib = ShaderProgramDefinitions.MeshAttributeGenerator.generateColourAttribute(1);
		meshExt.colourAttrib.bufferResourceType = BufferDataManagementType.List;
		meshExt.colourAttrib.instanced = true;
		meshExt.mesh.VBOs.add(meshExt.colourAttrib);

		
		meshExt.zIndexAttrib = ShaderProgramDefinitions.MeshAttributeGenerator.generateZIndexAttribute(1);
		meshExt.zIndexAttrib.bufferResourceType = BufferDataManagementType.List;
		meshExt.mesh.VBOs.add(meshExt.zIndexAttrib);

		
		meshExt.offsetAttrib = ShaderProgramDefinitions.MeshAttributeGenerator.generateMeshTransformAttribute(1);
		meshExt.offsetAttrib.name = "offset";
		meshExt.offsetAttrib.bufferResourceType = BufferDataManagementType.List;
		meshExt.mesh.VBOs.add(meshExt.offsetAttrib);

		meshExt.mesh.vertexCount = vertices;
		meshExt.mesh.modelRenderType = GLDrawMode.TriangleFan;
		//meshExt.mesh.singleFrameDiscard = true;
	}

	/**
	 * @deprecated This is not the proper way to create temporary meshes
	 * @param meshExt
	 * @param vertices
	 */
	public static void setupDiscardMesh3D(MeshExt meshExt, int vertices) {

		meshExt.positionAttrib.name = "position";
		meshExt.positionAttrib.bufferType = BufferType.ArrayBuffer;
		meshExt.positionAttrib.dataType = GLDataType.Float;
		meshExt.positionAttrib.bufferUsage = BufferUsage.StreamDraw;
		meshExt.positionAttrib.dataSize = 3;
		meshExt.positionAttrib.normalised = false;
		meshExt.mesh.VBOs.add(meshExt.positionAttrib);

		meshExt.colourAttrib.name = "colour";
		meshExt.colourAttrib.bufferType = BufferType.ArrayBuffer;
		meshExt.colourAttrib.dataType = GLDataType.Float;
		meshExt.colourAttrib.bufferUsage = BufferUsage.StreamDraw;
		meshExt.colourAttrib.dataSize = 4;
		meshExt.colourAttrib.normalised = false;
		meshExt.colourAttrib.instanced = true;
		meshExt.colourAttrib.instanceStride = 1;
		meshExt.mesh.VBOs.add(meshExt.colourAttrib);

		meshExt.offsetAttrib.name = "offset";
		meshExt.offsetAttrib.bufferType = BufferType.ArrayBuffer;
		meshExt.offsetAttrib.dataType = GLDataType.Float;
		meshExt.offsetAttrib.bufferUsage = BufferUsage.StreamDraw;
		meshExt.offsetAttrib.dataSize = 3;
		meshExt.offsetAttrib.normalised = false;
		meshExt.offsetAttrib.instanced = true;
		meshExt.offsetAttrib.instanceStride = 1;
		meshExt.mesh.VBOs.add(meshExt.offsetAttrib);

		meshExt.mesh.vertexCount = vertices;
		meshExt.mesh.modelRenderType = GLDrawMode.TriangleFan;
	}

	/*
		void setupDiscardInkingMesh(MeshExt meshExt, int vertices) {
	
			meshExt.positionAttrib.name = "position";
			meshExt.positionAttrib.bufferType = BufferType.ArrayBuffer;
			meshExt.positionAttrib.dataType = GLDataType.Float;
			meshExt.positionAttrib.bufferUsage = BufferUsage.StreamDraw;
			meshExt.positionAttrib.dataSize = 2;
			meshExt.positionAttrib.normalised = false;
			meshExt.mesh.VBOs.add(meshExt.positionAttrib);
	
			meshExt.colourAttrib.name = "colour";
			meshExt.colourAttrib.bufferType = BufferType.ArrayBuffer;
			meshExt.colourAttrib.dataType = GLDataType.Float;
			meshExt.colourAttrib.bufferUsage = BufferUsage.StreamDraw;
			meshExt.colourAttrib.dataSize = 4;
			meshExt.colourAttrib.normalised = false;
			meshExt.colourAttrib.instanced = true;
			meshExt.colourAttrib.instanceStride = 1;
			meshExt.mesh.VBOs.add(meshExt.colourAttrib);
	
			meshExt.mesh.vertexCount = vertices;
			meshExt.mesh.modelRenderType = GLDrawMode.LineLoop;
			meshExt.mesh.singleFrameDiscard = true;
		}
	*/
}
