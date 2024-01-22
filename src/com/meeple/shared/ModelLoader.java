package com.meeple.shared;

import static org.lwjgl.assimp.Assimp.*;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIPropertyStore;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;

import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferDataManagementType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDrawMode;
import com.meeple.shared.frame.OGL.ShaderProgram.RenderableVAO;
import com.meeple.shared.frame.nuklear.IOUtil;

public class ModelLoader {

    private static Logger logger = Logger.getLogger(ModelLoader.class);

    private static final String transformMatName = "meshTransformMatrix", 
        materialIndexName = "meshMaterialIndex",
        normalMatName = "meshNormalMatrix", 
        colourName = "colour";

    /**
     * Imports all meshes from a file. Powered by AssImp
     * 
     * @param fileLocation      Location of the file, either internal, relayive or
     *                          absolute
     * @param maxModelInstances Maximum instances of a mesh that will be rendered
     * @param postConvertMesh   Consumer that is called after the conversion of
     *                          every mesh
     * @return A map, mesh names to mesh
     * @throws IOException           if file is not found
     * @throws IllegalStateException if AssImp fails for whatever reason
     */
    public static Map<String, RenderableVAO> loadModelFromFile(String fileLocation, int maxModelInstances, Consumer<ShaderProgram.RenderableVAO> postConvertMesh) throws IOException {

        // read the resource into a buffer, and load the scene from the buffer
        ByteBuffer fileContent = IOUtil.ioResourceToByteBuffer(fileLocation, 2048 * 8);
        AIPropertyStore store = aiCreatePropertyStore();
        aiSetImportPropertyInteger(store, AI_CONFIG_PP_SBP_REMOVE, Assimp.aiPrimitiveType_LINE | Assimp.aiPrimitiveType_POINT);
        // aiSetImportPropertyInteger(store, AI_CONFIG_PP_FD_CHECKAREA , 0);
        AIScene scene = aiImportFileFromMemoryWithProperties(
                fileContent,
                0 |
                // aiProcess_JoinIdenticalVertices |
                        aiProcess_Triangulate
                        // aiProcessPreset_TargetRealtime_MaxQuality |
                        // | aiProcess_FindDegenerates
                        | aiProcess_GenNormals | aiProcess_FixInfacingNormals | aiProcess_GenSmoothNormals
                        // aiProcess_MakeLeftHanded |
                        // aiProcess_ImproveCacheLocality |
                        // | aiProcess_findi
                        | aiProcess_JoinIdenticalVertices | aiProcess_SortByPType,
                (ByteBuffer) null,
                store);
        if (scene == null) {
            throw new IllegalStateException(aiGetErrorString());
        }
        int meshCount = scene.mNumMeshes();
        Map<String, RenderableVAO> meshes = new HashMap<>(meshCount);
        PointerBuffer meshesBuffer = scene.mMeshes();
        for (int i = 0; i < meshCount; ++i) {
            AIMesh mesh = AIMesh.create(meshesBuffer.get(i));
            String meshName = mesh.mName().dataString();
            logger.trace("Mesh with name: " + meshName + " just been imported");
            ShaderProgram.RenderableVAO dmesh = setupMesh(mesh, maxModelInstances);

            meshes.put(meshName, dmesh);
            if (postConvertMesh != null) {
                postConvertMesh.accept(dmesh);
            }
            mesh.clear();
            mesh.free();
        }
        aiReleaseImport(scene);
        return meshes;
    }

    private static IntBuffer convertElementBuffer(AIFace.Buffer facesBuffer, int faceCount, int elementCount) {
        IntBuffer elementArrayBufferData = BufferUtils.createIntBuffer(elementCount);
        for (int i = 0; i < faceCount; ++i) {
            AIFace face = facesBuffer.get(i);
            if (face.mNumIndices() != 3) {
                logger.trace("not 3 verts in a face. actually had " + face.mNumIndices());
            } else {
                elementArrayBufferData.put(face.mIndices());
            }
        }
        elementArrayBufferData.flip();
        return elementArrayBufferData;
    }

    /**
     * sets up the mesh with attributes/VBOs and uses the AIMesh data provided
     * 
     * @param aim       mesh data to read from
     * @param maxMeshes maximum instances of the mesh
     * @return Mesh to be rendered with shader program
     */
    private static ShaderProgram.RenderableVAO setupMesh(AIMesh aim, long maxMeshes) {
        ShaderProgram.RenderableVAO mesh = new ShaderProgram.RenderableVAO();
        {
            Attribute vertexAttrib = new Attribute();
            vertexAttrib.name = "vertex";
            vertexAttrib.bufferType = BufferType.ArrayBuffer;
            vertexAttrib.dataType = GLDataType.Float;
            vertexAttrib.bufferUsage = BufferUsage.StaticDraw;
            vertexAttrib.dataSize = 3;
            vertexAttrib.normalised = false;

            AIVector3D.Buffer vertices = aim.mVertices();
            vertexAttrib.bufferAddress = vertices.address();

            aim.mVertices().free();
            vertices.clear();
            vertexAttrib.bufferLen = (long) (AIVector3D.SIZEOF * vertices.remaining());
            vertexAttrib.bufferResourceType = BufferDataManagementType.Address;
            mesh.VBOs.add(vertexAttrib);
        }
        {
            Attribute normalAttrib = new Attribute();
            normalAttrib.name = "normal";
            normalAttrib.bufferType = BufferType.ArrayBuffer;
            normalAttrib.dataType = GLDataType.Float;
            normalAttrib.bufferUsage = BufferUsage.StaticDraw;
            normalAttrib.dataSize = 3;
            normalAttrib.normalised = false;
            normalAttrib.instanced = false;
            AIVector3D.Buffer normals = aim.mNormals();
            normalAttrib.bufferAddress = normals.address();
            normalAttrib.bufferLen = (long) (AIVector3D.SIZEOF * normals.remaining());
            normalAttrib.bufferResourceType = BufferDataManagementType.Address;
            mesh.VBOs.add(normalAttrib);
        }
        {
            BufferObject elementAttrib = new BufferObject();
            elementAttrib.bufferType = BufferType.ElementArrayBuffer;
            elementAttrib.bufferUsage = BufferUsage.StaticDraw;
            elementAttrib.dataType = GLDataType.UnsignedInt;
            // elementAttrib.dataSize = 3;
            mesh.VBOs.add(elementAttrib);
            AIFace.Buffer facesBuffer = aim.mFaces();   
            int faceCount = aim.mNumFaces();
            int elementCount = faceCount * 3;
            elementAttrib.bufferResourceType = BufferDataManagementType.Buffer;

            IntBuffer elementArrayBufferData = convertElementBuffer(facesBuffer, faceCount, elementCount);
            elementAttrib.buffer = elementArrayBufferData;

            mesh.index = new WeakReference<ShaderProgram.BufferObject>(elementAttrib);
            mesh.vertexCount = elementCount;
        }

        {
            Attribute materialIndexAttrib = new Attribute();
            materialIndexAttrib.name = "materialIndex";
            materialIndexAttrib.bufferType = BufferType.ArrayBuffer;
            materialIndexAttrib.dataType = GLDataType.Float;
            materialIndexAttrib.bufferUsage = BufferUsage.DynamicDraw;
            materialIndexAttrib.dataSize = 1;
            materialIndexAttrib.normalised = false;
            materialIndexAttrib.instanced = true;
            materialIndexAttrib.instanceStride = 1;
            materialIndexAttrib.bufferResourceType = BufferDataManagementType.Empty;
            materialIndexAttrib.bufferLen = maxMeshes;
            mesh.VBOs.add(materialIndexAttrib);
            //mesh.instanceAttributes.put(materialIndexName, new WeakReference<>(materialIndexAttrib));
        }

        {
            Attribute meshTransformAttrib = new Attribute();
            meshTransformAttrib.name = "modelMatrix";
            meshTransformAttrib.bufferType = BufferType.ArrayBuffer;
            meshTransformAttrib.dataType = GLDataType.Float;
            meshTransformAttrib.bufferUsage = BufferUsage.DynamicDraw;
            meshTransformAttrib.dataSize = 16;
            meshTransformAttrib.normalised = false;
            meshTransformAttrib.instanced = true;
            meshTransformAttrib.instanceStride = 1;
            meshTransformAttrib.bufferResourceType = BufferDataManagementType.Empty;
            meshTransformAttrib.bufferLen = maxMeshes;
            mesh.VBOs.add(meshTransformAttrib);
            // FrameUtils.appendToList(meshTransformAttrib.data, modelMatrix);
            //mesh.instanceAttributes.put(transformMatName, new WeakReference<>(meshTransformAttrib));
        }
        /**
         * It is important to use a data size of 16 rather than 9 because for some
         * reason the buffer adds padding to vec3 to 4 floats
         * easier to just make it a 4 float array
         */
        {
            Attribute meshNormalMatrixAttrib = new Attribute();
            meshNormalMatrixAttrib.name = "normalMatrix";
            meshNormalMatrixAttrib.bufferType = BufferType.ArrayBuffer;
            meshNormalMatrixAttrib.dataType = GLDataType.Float;
            meshNormalMatrixAttrib.bufferUsage = BufferUsage.DynamicDraw;
            meshNormalMatrixAttrib.dataSize = 16;
            meshNormalMatrixAttrib.normalised = false;
            meshNormalMatrixAttrib.instanced = true;
            meshNormalMatrixAttrib.instanceStride = 1;
            meshNormalMatrixAttrib.bufferResourceType = BufferDataManagementType.Empty;
            meshNormalMatrixAttrib.bufferLen = maxMeshes;
            mesh.VBOs.add(meshNormalMatrixAttrib);
            // FrameUtils.appendToList(meshTransformAttrib.data, modelMatrix);
            //mesh.instanceAttributes.put(normalMatName, new WeakReference<>(meshNormalMatrixAttrib));
        }
        mesh.modelRenderType = GLDrawMode.Triangles;

        return mesh;

    }
}
