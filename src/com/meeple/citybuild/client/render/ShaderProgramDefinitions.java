package com.meeple.citybuild.client.render;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL46;

import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferDataManagementType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferObject;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferUsage;
import com.meeple.shared.frame.OGL.ShaderProgram.GLDataType;
import com.meeple.shared.frame.structs.Light;
import com.meeple.shared.frame.structs.Material;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;

public class ShaderProgramDefinitions {
    public static class ViewMatrices {
        public Matrix4f fixMatrix = new Matrix4f(ShaderProgramDefinitions.fixMatrix);
        public Matrix4f projectionMatrix = new Matrix4f();
        public Matrix4f viewMatrix = new Matrix4f();

        public AtomicBoolean fixMatrixUpdate = new AtomicBoolean(true);
        public AtomicBoolean projectionMatrixUpdate = new AtomicBoolean(true);
        public AtomicBoolean viewMatrixUpdate = new AtomicBoolean(true);

        protected Matrix4f viewProjectionMatrix = new Matrix4f();
        protected Matrix4f viewProjectionFixMatrix = new Matrix4f();

    }

    /* matrix to convert from z forward to z up, aka the fix matrix */
    public static final Matrix4f fixMatrix = new Matrix4f(
            1, 0, 0, 0,
            0, 0, 1, 0,
            0, 1, 0, 0,
            0, 0, 0, 1);

    /**
     * Maximum lights to calculate in the shaders. more lights is more intensive GPU
     * wise
     * Unknown good limits for the game yet
     */
    protected static final int maxLights = 10;
    protected static final int maxMaterials = 15;

    /**
     * Binding locations for the shared uniforms.
     * not linked to anything but must be unique
     */
    private static final int matrixBindingpoint = 2, lightBufferBindingPoint = 3, materialBufferBindingPoint = 4,
            ambientBrightnessBindingPoint = 5, UIProjectionMatrixBindingPoint = 6;

    private static int genMatrixBuffer(GLContext glc) {
        int matrixBuffer = glc.genUBO(matrixBindingpoint,
                /* 4x4 matrix */ 16 * ShaderProgram.GLDataType.Float.getBytes() * 5);
        return matrixBuffer;
    }

    private static int genLightBuffer(GLContext glc) {
        int lightBuffer = glc.genUBO(lightBufferBindingPoint,
                Light.sizeOf * ShaderProgram.GLDataType.Float.getBytes() * maxLights);
        return lightBuffer;
    }

    protected static int genMaterialBuffer(GLContext glc) {
        int materialBuffer = glc.genUBO(materialBufferBindingPoint,
                Material.sizeOf * ShaderProgram.GLDataType.Float.getBytes() * maxMaterials);
        return materialBuffer;
    }

    private static int genAmbientBrightnessBuffer(GLContext glc) {
        int ambientBrightnessBuffer = glc.genUBO(ambientBrightnessBindingPoint,
                ShaderProgram.GLDataType.Float.getBytes());
        return ambientBrightnessBuffer;
    }
    private static int genUIProjectionMatrixBuffer(GLContext glc){
        int projectionMatrixBuffer = glc.genUBO(UIProjectionMatrixBindingPoint, ShaderProgram.GLDataType.Float.getBytes() * 16);
        return projectionMatrixBuffer;
    }

    public static ShaderProgramCollection collection = new ShaderProgramCollection();

    public static class ShaderProgramCollection {
        private ShaderProgramCollection() {
        }

        public ShaderProgramDefinition_3D_lit_flat _3D_lit_flat = new ShaderProgramDefinition_3D_lit_flat();
        public ShaderProgramDefinition_3D_lit_mat _3D_lit_mat = new ShaderProgramDefinition_3D_lit_mat();
        public ShaderProgramDefinition_3D_unlit_flat _3D_unlit_flat = new ShaderProgramDefinition_3D_unlit_flat();
        public ShaderProgramDefinition_UI UI = new ShaderProgramDefinition_UI();

        private int matrixBuffer;
        private int lightBuffer;
        private int materialBuffer;
        private int ambientBrightnessBuffer;
        private int UIProjectionMatrixBuffer;

        public void create(GLContext glc) throws AssertionError {
            ShaderProgramSystem2.create(glc, _3D_lit_flat);
            ShaderProgramSystem2.create(glc, _3D_lit_mat);
            ShaderProgramSystem2.create(glc, _3D_unlit_flat);

            setupUBOs(glc);

        }

        public void setupUBOs(GLContext glc) {
            /* All programs using the Matrix/Matrices UBO */
            ShaderProgram[] all_programs = { _3D_lit_flat, _3D_lit_mat, _3D_unlit_flat };
            ShaderProgram[] material_programs = { _3D_lit_mat };
            ShaderProgram[] lit_programs = { _3D_lit_flat, _3D_lit_mat };

            setupMatrixUBO(glc, all_programs);
            setupLightUBO(glc, lit_programs);
            setupAmbientBrightnessUBO(glc, lit_programs);
            setupMaterialUBO(glc, material_programs);
            
            writeFixMatrix(fixMatrix);

            ShaderProgram[] ui_programs = {UI};
            setupUIProjectionMatrixUBO(glc, ui_programs);

        }

        public void setupMatrixUBO(GLContext glc, ShaderProgram... programs) {
            this.matrixBuffer = ShaderProgramDefinitions.genMatrixBuffer(glc);
            glc.bindUBONameToIndex("Matrices", matrixBindingpoint, programs);
        }

        private void setupLightUBO(GLContext glc, ShaderProgram... programs) {
            this.lightBuffer = ShaderProgramDefinitions.genLightBuffer(glc);
            glc.bindUBONameToIndex("LightBlock", lightBufferBindingPoint, programs);
        }

        private void setupMaterialUBO(GLContext glc, ShaderProgram... programs) {
            this.materialBuffer = ShaderProgramDefinitions.genMaterialBuffer(glc);
            glc.bindUBONameToIndex("MaterialBlock", materialBufferBindingPoint, programs);
        }

        private void setupAmbientBrightnessUBO(GLContext glc, ShaderProgram... programs) {
            this.ambientBrightnessBuffer = genAmbientBrightnessBuffer(glc);
            glc.bindUBONameToIndex("ambientBrightness", ambientBrightnessBindingPoint, programs);
        }

        public void setupUIProjectionMatrixUBO(GLContext glc, ShaderProgram... programs) {
            this.UIProjectionMatrixBuffer = genUIProjectionMatrixBuffer(glc);
            glc.bindUBONameToIndex("MatrixBlock", UIProjectionMatrixBindingPoint, programs);
        }

        /**
         * upload given light(s) to the shared light buffer object
         * 
         * @param startAt the offset of the light(s) in the buffer
         * @param lights  any amount of lights to update
         */
        public void updateLights(int startAt, Light... lights) {
            glBindBuffer(GL46.GL_UNIFORM_BUFFER, lightBuffer);
            float[] store = new float[Light.sizeOf];
            for (int i = 0; i < lights.length && i + startAt < ShaderProgramDefinitions.maxLights; i++) {
                lights[i].toArray(store, 0);

                glBufferSubData(GL46.GL_UNIFORM_BUFFER,
                        Light.sizeOf * ShaderProgram.GLDataType.Float.getBytes() * (startAt + i), store);
            }
            glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
        }

        /**
         * upload given materials(s) to the shared material buffer object
         * 
         * @param startAt   the offset of the material(s) in the buffer
         * @param materials any amount of materials to update
         */
        public void updateMaterials(int startAt, Material... materials) {
            glBindBuffer(GL46.GL_UNIFORM_BUFFER, materialBuffer);
            float[] store = new float[Material.sizeOf];
            for (int i = 0; i < materials.length && i + startAt < ShaderProgramDefinitions.maxMaterials; i++) {
                materials[i].toArray(store, 0);
                glBufferSubData(GL46.GL_UNIFORM_BUFFER,
                        Material.dataOffset(Material.sizeOf, i + startAt) * ShaderProgram.GLDataType.Float.getBytes(),
                        store);
                // glBufferSubData(GL46.GL_UNIFORM_BUFFER, Material.sizeOf *
                // ShaderProgram.GLDataType.Float.getBytes() * ( startAt + i ), store);
            }
            glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
        }

        public void updateAmbientBrightness(float value) {
            glBindBuffer(GL46.GL_UNIFORM_BUFFER, ambientBrightnessBuffer);
            glBufferData(GL46.GL_UNIFORM_BUFFER, new float[] { value }, GL46.GL_DYNAMIC_DRAW);
        }

        public void updateUIProjectionMatrix(Matrix4f UIProjectionMatrix){
            glBindBuffer(GL46.GL_UNIFORM_BUFFER, UIProjectionMatrixBuffer);
            glBufferData(GL46.GL_UNIFORM_BUFFER, UIProjectionMatrix.get(new float[16]),GL46.GL_DYNAMIC_DRAW);
        }

        protected void writeFixMatrix(Matrix4f fix) {
            // no need to be in a program bidning, since this is shared between multiple
            // programs
            glBindBuffer(GL46.GL_UNIFORM_BUFFER, matrixBuffer);
            float[] store = new float[16];
            if (fix != null)
                glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 0, fix.get(store));
            glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
        }

        public void writeVPFMatrix(Matrix4f projection, Matrix4f view, Matrix4f vp, Matrix4f vpf) {

            // no need to be in a program bidning, since this is shared between multiple
            // programs
            glBindBuffer(GL46.GL_UNIFORM_BUFFER, matrixBuffer);
            float[] store = new float[16];
            if (projection != null)
                glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 1, projection.get(store));
            if (view != null)
                glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 2, view.get(store));
            if (vp != null)
                glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 3, vp.get(store));
            if (vpf != null)
                glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 4, vpf.get(store));

            glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
        }

        public void writeVPFMatrix(ViewMatrices matrices) {
            glBindBuffer(GL46.GL_UNIFORM_BUFFER, matrixBuffer);
            float[] store = new float[16];

            boolean fixUpdate = matrices.fixMatrixUpdate.compareAndSet(true, false);
            boolean projectionUpdate = matrices.projectionMatrixUpdate.compareAndSet(true, false);
            boolean viewUpdate = matrices.viewMatrixUpdate.compareAndSet(true, false);

            if (fixUpdate) {
                glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 0, matrices.fixMatrix.get(store));
            }
            if (projectionUpdate) {
                glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 1, matrices.projectionMatrix.get(store));
            }

            if (viewUpdate) {
                glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 2, matrices.viewMatrix.get(store));
            }

            if (viewUpdate || projectionUpdate)
                matrices.projectionMatrix.mul(matrices.viewMatrix, matrices.viewProjectionMatrix);
            glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 3, matrices.viewProjectionMatrix.get(store));

            if (viewUpdate || projectionUpdate || fixUpdate)
                matrices.viewProjectionMatrix.mul(matrices.fixMatrix, matrices.viewProjectionFixMatrix);
            glBufferSubData(GL46.GL_UNIFORM_BUFFER, 64 * 4, matrices.viewProjectionFixMatrix.get(store));

            glBindBuffer(GL46.GL_UNIFORM_BUFFER, 0);
        }

    }

    protected abstract static class BaseShaderProgram<E extends BaseShaderProgram<E>.Mesh> extends ShaderProgram {

        public class Mesh extends RenderableVAO {
            public Attribute vertexAttribute;
            public Attribute meshTransformAttribute;

            protected Mesh() {
            }
        }

        protected abstract E newMesh();

        public E createMesh(long maxInstances) {
            E mesh = newMesh();
            if (mesh.vertexAttribute == null) {
                mesh.vertexAttribute = MeshAttributeGenerator.generateVertexAttribute();
                mesh.VBOs.add(mesh.vertexAttribute);
            }

            if (mesh.meshTransformAttribute == null) {
                mesh.meshTransformAttribute = MeshAttributeGenerator.generateMeshTransformAttribute(maxInstances);
                mesh.VBOs.add(mesh.meshTransformAttribute);
                mesh.instanceAttributes.put(meshTransform_AttributeName,
                        new WeakReference<ShaderProgram.Attribute>(mesh.meshTransformAttribute));
            }

            return mesh;

        }
    }
    

    public static class ShaderProgramDefinition_UI extends BaseShaderProgram<ShaderProgramDefinition_UI.Mesh> {

        public class Mesh extends BaseShaderProgram<Mesh>.Mesh {
            public Attribute colourAttribute;
            public Attribute zIndexAttribute;

            protected Mesh() {

            }
        }

        protected ShaderProgramDefinition_UI() {
            String vertSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/2D_UI.vert"));
            String fragSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/basic-alpha-discard-colour.frag"));

            shaderSources.put(GLShaderType.VertexShader, vertSource);
            shaderSources.put(GLShaderType.FragmentShader, fragSource);
        }

        protected Mesh newMesh() {
            return new Mesh();
        }

        public Mesh createMesh(long maxInstances) {
            Mesh mesh = (Mesh) super.createMesh(maxInstances);
            mesh.vertexAttribute.dataSize = 2;
            mesh.meshTransformAttribute.name = "meshTransform";
            mesh.meshTransformAttribute.bufferResourceType = BufferDataManagementType.List;

            if (mesh.colourAttribute == null) {
                mesh.colourAttribute = MeshAttributeGenerator.generateColourAttribute(maxInstances);
                mesh.colourAttribute.bufferResourceType = BufferDataManagementType.List;
                mesh.colourAttribute.instanced = true;
                mesh.VBOs.add(mesh.colourAttribute);
                mesh.instanceAttributes.put(colour_AttributeName, new WeakReference<ShaderProgram.Attribute>(mesh.colourAttribute));
            }
            if(mesh.zIndexAttribute == null){
                mesh.zIndexAttribute = MeshAttributeGenerator.generateZIndexAttribute(maxInstances);
                mesh.zIndexAttribute.bufferResourceType = BufferDataManagementType.List;
                mesh.VBOs.add(mesh.zIndexAttribute);
                mesh.instanceAttributes.put(zIndex_AttributeName, new WeakReference<ShaderProgram.Attribute>(mesh.zIndexAttribute));
            }
            mesh.modelRenderType = GLDrawMode.TriangleFan;

            return mesh;

        }
    }

    protected abstract static class _3DShaderProgram<E extends _3DShaderProgram<E>.Mesh> extends BaseShaderProgram<E> {

        public class Mesh extends BaseShaderProgram<E>.Mesh {

            protected Mesh() {
            }
        }

    }

    public static class ShaderProgramDefinition_3D_unlit_flat
            extends _3DShaderProgram<ShaderProgramDefinition_3D_unlit_flat.Mesh> {

        public class Mesh extends _3DShaderProgram<Mesh>.Mesh {
            public Attribute colourAttribute;

            protected Mesh() {

            }
        }

        protected ShaderProgramDefinition_3D_unlit_flat() {

            String vertSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_nolit_flat.vert"));

            String fragSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_nolit_flat.frag"));

            shaderSources.put(GLShaderType.VertexShader, vertSource);
            shaderSources.put(GLShaderType.FragmentShader, fragSource);
        }

        protected Mesh newMesh() {
            return new Mesh();
        }

        public Mesh createMesh(long maxInstances) {
            Mesh mesh = (Mesh) super.createMesh(maxInstances);
            if (mesh.colourAttribute == null) {
                mesh.colourAttribute = MeshAttributeGenerator.generateColourAttribute(maxInstances);
                mesh.VBOs.add(mesh.colourAttribute);
                mesh.instanceAttributes.put(colour_AttributeName,
                        new WeakReference<ShaderProgram.Attribute>(mesh.colourAttribute));
            }
            mesh.modelRenderType = GLDrawMode.Line;

            return mesh;

        }
    }

    public static abstract class LitShaderProgramDefinition<E extends LitShaderProgramDefinition<E>.Mesh>
            extends _3DShaderProgram<E> {

        public class Mesh extends _3DShaderProgram<E>.Mesh {
            public Attribute normalAttribute;
            public BufferObject elementAttribute;
            public Attribute meshNormalMatrixAttribute;

            protected Mesh() {

            }
        }

        protected abstract E newMesh();

        public E createMesh(long maxInstances) {
            E mesh = super.createMesh(maxInstances);
            if (mesh.vertexAttribute == null) {
                mesh.vertexAttribute = MeshAttributeGenerator.generateVertexAttribute();
                mesh.VBOs.add(mesh.vertexAttribute);
            }
            if (mesh.normalAttribute == null) {
                mesh.normalAttribute = MeshAttributeGenerator.generateNormalAttribute();
                mesh.VBOs.add(mesh.normalAttribute);
            }

            if (mesh.elementAttribute == null) {
                mesh.elementAttribute = MeshAttributeGenerator.generateElementAttribute();
                mesh.VBOs.add(mesh.elementAttribute);
                mesh.index = new WeakReference<ShaderProgram.BufferObject>(mesh.elementAttribute);
            }
            if (mesh.meshNormalMatrixAttribute == null) {
                mesh.meshNormalMatrixAttribute = MeshAttributeGenerator.generateMeshNormalMatrixAttribute(maxInstances);
                mesh.VBOs.add(mesh.meshNormalMatrixAttribute);
                mesh.instanceAttributes.put(normalMatrix_AttributeName,
                        new WeakReference<ShaderProgram.Attribute>(mesh.meshNormalMatrixAttribute));
            }
            mesh.modelRenderType = GLDrawMode.Triangles;

            return mesh;

        }
    }

    public static class ShaderProgramDefinition_3D_lit_flat
            extends LitShaderProgramDefinition<ShaderProgramDefinition_3D_lit_flat.Mesh> {

        public class Mesh extends LitShaderProgramDefinition<ShaderProgramDefinition_3D_lit_flat.Mesh>.Mesh {
            public Attribute colourAttribute = new Attribute();

        }

        protected ShaderProgramDefinition_3D_lit_flat() {

            String vertSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_lit_flat.vert"));
            vertSource = vertSource.replaceAll("\\{maxlights\\}", maxLights + "");

            String fragSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_lit_flat.frag"));
            fragSource = fragSource.replaceAll("\\{maxlights\\}", maxLights + "");

            shaderSources.put(GLShaderType.VertexShader, vertSource);
            shaderSources.put(GLShaderType.FragmentShader, fragSource);
        }

        @Override
        public Mesh createMesh(long maxInstances) {
            Mesh mesh = (Mesh) super.createMesh(maxInstances);
            if (mesh.colourAttribute == null) {
                mesh.colourAttribute = MeshAttributeGenerator.generateColourAttribute(maxInstances);
                mesh.VBOs.add(mesh.colourAttribute);
                mesh.instanceAttributes.put(colour_AttributeName,
                        new WeakReference<ShaderProgram.Attribute>(mesh.colourAttribute));
            }
            return mesh;
        }

        @Override
        protected Mesh newMesh() {
            return new Mesh();
        }

    }

    public static class ShaderProgramDefinition_3D_lit_mat
            extends LitShaderProgramDefinition<ShaderProgramDefinition_3D_lit_mat.Mesh> {

        public class Mesh extends LitShaderProgramDefinition<ShaderProgramDefinition_3D_lit_mat.Mesh>.Mesh {
            public Attribute materialIndexAttribute;

            protected Mesh() {

            }
        }

        protected ShaderProgramDefinition_3D_lit_mat() {

            String vertSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_lit_mat.vert"));
            vertSource = vertSource.replaceAll("\\{maxlights\\}", maxLights + "");

            String fragSource = ShaderProgramSystem2.loadShaderSourceFromFile(("resources/shaders/3D_lit_mat.frag"));
            fragSource = fragSource.replaceAll("\\{maxmats\\}", "" + maxMaterials);
            fragSource = fragSource.replaceAll("\\{maxlights\\}", maxLights + "");

            shaderSources.put(GLShaderType.VertexShader, vertSource);
            shaderSources.put(GLShaderType.FragmentShader, fragSource);
        }

        @Override
        public Mesh createMesh(long maxInstances) {
            Mesh mesh = (Mesh) super.createMesh(maxInstances);
            if (mesh.materialIndexAttribute == null) {
                mesh.materialIndexAttribute = MeshAttributeGenerator.generateMaterialIndexAttribute(maxInstances);
                mesh.VBOs.add(mesh.materialIndexAttribute);
                mesh.instanceAttributes.put(materialIndex_AttributeName,
                        new WeakReference<ShaderProgram.Attribute>(mesh.materialIndexAttribute));
            }
            return mesh;
        }

        @Override
        protected Mesh newMesh() {
            return new Mesh();
        }
    }
    

    public static final String vertex_AttributeName = "vertex",
            zIndex_AttributeName = "zIndex",
            colour_AttributeName = "colour",
            normal_AttributeName = "normal",
            meshTransform_AttributeName = "modelMatrix",
            normalMatrix_AttributeName = "normalMatrix",
            materialIndex_AttributeName = "materialIndex";

    public static class MeshAttributeGenerator {
        
        public static Attribute generateVertexAttribute() {
            Attribute vertexAttribute = new Attribute();
            vertexAttribute.name = vertex_AttributeName;
            vertexAttribute.bufferType = BufferType.ArrayBuffer;
            vertexAttribute.dataType = GLDataType.Float;
            vertexAttribute.bufferUsage = BufferUsage.StaticDraw;
            vertexAttribute.dataSize = 3;
            vertexAttribute.normalised = false;
            return vertexAttribute;
        }

        public static Attribute generateZIndexAttribute(long maxInstances) {
            Attribute zIndexAttribute = new Attribute();
            zIndexAttribute.name = zIndex_AttributeName;
            zIndexAttribute.bufferType = BufferType.ArrayBuffer;
            zIndexAttribute.dataType = GLDataType.Float;
            zIndexAttribute.bufferUsage = BufferUsage.DynamicDraw;
            zIndexAttribute.dataSize = 1;
            zIndexAttribute.normalised = false;
            zIndexAttribute.instanced = true;
            zIndexAttribute.instanceStride = 1;
            zIndexAttribute.bufferResourceType = BufferDataManagementType.Empty;
            zIndexAttribute.bufferLen = maxInstances;
            return zIndexAttribute;
        }

        public static Attribute generateColourAttribute(long maxInstances) {
            Attribute colourAttribute = new Attribute();
            colourAttribute.name = colour_AttributeName;
            colourAttribute.bufferType = BufferType.ArrayBuffer;
            colourAttribute.dataType = GLDataType.Float;
            colourAttribute.bufferUsage = BufferUsage.StaticDraw;
            colourAttribute.dataSize = 4;
            colourAttribute.normalised = false;
            colourAttribute.instanced = false;
            colourAttribute.instanceStride = 1;
            colourAttribute.bufferResourceType = BufferDataManagementType.Empty;
            colourAttribute.bufferLen = maxInstances;
            return colourAttribute;
        }

        public static Attribute generateNormalAttribute() {
            Attribute normalAttribute = new Attribute();
            normalAttribute.name = normal_AttributeName;
            normalAttribute.bufferType = BufferType.ArrayBuffer;
            normalAttribute.dataType = GLDataType.Float;
            normalAttribute.bufferUsage = BufferUsage.StaticDraw;
            normalAttribute.dataSize = 3;
            normalAttribute.normalised = false;
            normalAttribute.instanced = false;

            return normalAttribute;
        }

        public static BufferObject generateElementAttribute() {
            BufferObject elementAttribute = new BufferObject();
            elementAttribute.bufferType = BufferType.ElementArrayBuffer;
            elementAttribute.dataType = GLDataType.UnsignedInt;
            elementAttribute.bufferUsage = BufferUsage.StaticDraw;

            // mesh.VBOs.add(mesh.elementAttribute);
            // mesh.index = new
            // WeakReference<ShaderProgram.BufferObject>(mesh.elementAttribute);
            return elementAttribute;
        }

        public static Attribute generateMeshTransformAttribute(long maxInstances) {
            Attribute meshTransformAttribute = new Attribute();
            meshTransformAttribute.name = meshTransform_AttributeName;
            meshTransformAttribute.bufferType = BufferType.ArrayBuffer;
            meshTransformAttribute.dataType = GLDataType.Float;
            meshTransformAttribute.bufferUsage = BufferUsage.DynamicDraw;
            meshTransformAttribute.dataSize = 16;
            meshTransformAttribute.normalised = false;
            meshTransformAttribute.instanced = true;
            meshTransformAttribute.instanceStride = 1;
            meshTransformAttribute.bufferResourceType = BufferDataManagementType.Empty;
            meshTransformAttribute.bufferLen = maxInstances;
            return meshTransformAttribute;
        }

        public static Attribute generateMeshNormalMatrixAttribute(long maxInstances) {
            Attribute meshNormalMatrixAttribute = new Attribute();
            meshNormalMatrixAttribute.name = normalMatrix_AttributeName;
            meshNormalMatrixAttribute.bufferType = BufferType.ArrayBuffer;
            meshNormalMatrixAttribute.dataType = GLDataType.Float;
            meshNormalMatrixAttribute.bufferUsage = BufferUsage.DynamicDraw;
            meshNormalMatrixAttribute.dataSize = 16;
            meshNormalMatrixAttribute.normalised = false;
            meshNormalMatrixAttribute.instanced = true;
            meshNormalMatrixAttribute.instanceStride = 1;
            meshNormalMatrixAttribute.bufferResourceType = BufferDataManagementType.Empty;
            meshNormalMatrixAttribute.bufferLen = maxInstances;
            return meshNormalMatrixAttribute;
        }

        public static Attribute generateMaterialIndexAttribute(long maxInstances) {
            Attribute materialIndexAttribute = new Attribute();
            materialIndexAttribute.name = materialIndex_AttributeName;
            materialIndexAttribute.bufferType = BufferType.ArrayBuffer;
            materialIndexAttribute.dataType = GLDataType.Float;
            materialIndexAttribute.bufferUsage = BufferUsage.DynamicDraw;
            materialIndexAttribute.dataSize = 1;
            materialIndexAttribute.normalised = false;
            materialIndexAttribute.instanced = true;
            materialIndexAttribute.instanceStride = 1;
            materialIndexAttribute.bufferResourceType = BufferDataManagementType.Empty;
            materialIndexAttribute.bufferLen = maxInstances;
            return materialIndexAttribute;
        }
        

    }

}
