package com.meeple.backend;

import java.nio.FloatBuffer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import com.meeple.backend.ShaderPrograms.InstanceData;
import com.meeple.backend.ShaderPrograms.InstanceData.Instance;
import com.meeple.backend.ShaderPrograms.InstancedAttribute;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;
import com.meeple.shared.frame.OGL.ShaderProgramSystem2;

public class Model {

	public final Map<Mesh, EnumSet<InstancedAttribute>> meshes = new HashMap<>();
	public final InstanceData instances = new InstanceData();
	private EnumSet<InstancedAttribute> enabledAttributes = EnumSet.noneOf(InstancedAttribute.class);
	private FloatBuffer transformMatricies, normalMatricies, colours, materialIndicies;

	public FloatBuffer transformMatricies() {
		return transformMatricies;
	}

	public FloatBuffer colours() {
		return colours;
	}

	public FloatBuffer normalMatricies() {
		return normalMatricies;
	}

	public FloatBuffer materialIndicies() {
		return materialIndicies;
	}

	public void enableAttributes(InstancedAttribute... attributes) {
		this.enabledAttributes.clear();
		for (InstancedAttribute attribute : attributes) {
			this.enabledAttributes.add(attribute);
		}
	}

	/**
	 * Disabled attributes
	 * @param mesh
	 * @param attributes
	 */
	public void addMesh(Mesh mesh, InstancedAttribute... attributes) {
		EnumSet<InstancedAttribute> set = EnumSet.noneOf(InstancedAttribute.class);
		for (InstancedAttribute att : attributes) {
			set.add(att);
		}
		this.meshes.put(mesh, set);
	}

	public void update() {
		build(this.instances.size(), this::createFromInstances);
	}

	public void build(int size, ModelBuildI builderFunction) {
		setupBuffers(size);
		builderFunction.build(transformMatricies, colours, normalMatricies, materialIndicies);
		flipAndUpload(size);
	}

	public static interface ModelBuildI {
		public void build(FloatBuffer transformations, FloatBuffer colours, FloatBuffer normalMatricies, FloatBuffer materialIndicies);
	}

	public void setupBuffers(int size) {

		transformMatricies = null;
		if (enabledAttributes.contains(ShaderPrograms.InstancedAttribute.Transformation)) {
			transformMatricies = BufferUtils.createFloatBuffer(size * 16);
		}
		normalMatricies = null;
		if (enabledAttributes.contains(ShaderPrograms.InstancedAttribute.NormalMatrix)) {
			normalMatricies = BufferUtils.createFloatBuffer(size * 16);
		}
		colours = null;
		if (enabledAttributes.contains(ShaderPrograms.InstancedAttribute.Colour)) {
			colours = BufferUtils.createFloatBuffer(size * 4);
		}
		materialIndicies = null;
		if (enabledAttributes.contains(ShaderPrograms.InstancedAttribute.MaterialIndex)) {
			materialIndicies = BufferUtils.createFloatBuffer(size);
		}
	}

	public void flipAndUpload(int size) {

		if (transformMatricies != null) {
			transformMatricies.flip();
		}
		if (colours != null) {
			colours.flip();
		}
		if (normalMatricies != null) {
			normalMatricies.flip();
		}
		if (materialIndicies != null) {
			materialIndicies.flip();
		}
		synchronized (meshes) {
			for (Iterator<Entry<Mesh, EnumSet<InstancedAttribute>>> iterator = meshes.entrySet().iterator(); iterator.hasNext();) {
				Entry<Mesh, EnumSet<InstancedAttribute>> entry = iterator.next();
				Mesh mesh = entry.getKey();
				EnumSet<InstancedAttribute> disabledAttributes = entry.getValue();
				if (transformMatricies != null && !disabledAttributes.contains(InstancedAttribute.Transformation)) {
					mesh.getAttribute(ShaderPrograms.transformAtt.name).data(transformMatricies).update.lazySet(true);
				}
				if (colours != null && !disabledAttributes.contains(InstancedAttribute.Colour)) {
					mesh.getAttribute(ShaderPrograms.colourAtt.name).data(colours).update.lazySet(true);
				}
				if (normalMatricies != null && !disabledAttributes.contains(InstancedAttribute.NormalMatrix)) {
					mesh.getAttribute(ShaderPrograms.normalMatrixAtt.name).data(normalMatricies).update.lazySet(true);
				}
				if (materialIndicies != null &&!disabledAttributes.contains(InstancedAttribute.MaterialIndex)) {
					mesh.getAttribute(ShaderPrograms.materialIndexAtt.name).data(materialIndicies).update.lazySet(true);
				}
				mesh.renderCount(size);
			}
		}
	}

	private void createFromInstances(FloatBuffer transformations, FloatBuffer colours, FloatBuffer normalMatricies, FloatBuffer materialIndicies) {
		synchronized (instances) {
			for (Iterator<Instance> i2 = instances.getIterator(); i2.hasNext();) {
				Instance instance = i2.next();
				if (instance.visible) {
					if (transformations != null) {
						Matrix4f mat = instance.meshMatrix;

						FrameUtils.appendToBuffer(transformations, mat);
					}
					if (colours != null) {
						Vector4f colour = instance.colour;
						FrameUtils.appendToBuffer(colours, colour);
					}
					if (normalMatricies != null) {

						Matrix3f normal = new Matrix3f();
						normal.set(instance.meshMatrix).invert().transpose();
						Matrix4f normalMat = new Matrix4f(normal);
						FrameUtils.appendToBuffer(normalMatricies, normalMat);

					}
					if (materialIndicies != null) {
						materialIndicies.put((Float) (instance.materialIndex * 1f));
					}
				}
			}
		}
	}

	public Instance addInstance(Matrix4f translation, Vector4f colour) {
		Instance i = instances.new Instance();
		i.colour = colour;
		i.meshMatrix = translation;
		return i;
	}

	public void loadVAOs(GLContext glContext, ShaderProgram program) {
		synchronized (meshes) {
			for (Iterator<Mesh> iterator = meshes.keySet().iterator(); iterator.hasNext();) {
				Mesh mesh = iterator.next();
				ShaderProgramSystem2.loadVAO(glContext, program, mesh);
			}
		}
		// TODO get which attributes are enabled
		/*
		 * for (Iterator<Entry<String, GLSLAttribute>> i =
		 * program.atts.entrySet().iterator(); i.hasNext();) { Entry<String,
		 * GLSLAttribute> entry = i.next(); entry.getValue(). }
		 */
	}

	public void render() {
		synchronized (meshes) {
			for (Iterator<Mesh> iterator = meshes.keySet().iterator(); iterator.hasNext();) {
				Mesh mesh = iterator.next();
				ShaderProgramSystem2.tryFullRenderMesh(mesh);
			}
		}
	}
	/*
	 * private Matrix4f recursive() { // if (parent != null) { // return
	 * parent.recursive().mul(transform, new Matrix4f()); // } // return transform;
	 * return new Matrix4f(); }
	 */
}
