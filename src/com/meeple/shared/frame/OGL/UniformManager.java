package com.meeple.shared.frame.OGL;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;

public class UniformManager {

	public class Uniform<T> {
		private Uniform() {

		}

		public IShaderUniformUploadSystem<T> uploadSystem;
		public Integer location;
		public String name;

	}

	public <T> Uniform<T> register(String name, IShaderUniformUploadSystem<T> upload) {
		Uniform<T> uniform = new Uniform<>();
		uniform.name = name;
		uniform.uploadSystem = upload;
		return uniform;
	}

	/**
	 * Runs through the uniforms queued uploads, linked to this system from the shader program 
	 * @param program Shader program that uniforms are a part of
	 */
	public <T> void uploadUniforms(Map<UniformManager.Uniform<?>, List<?>> uniforms) {

		iterateUniforms(uniforms, new BiConsumer<UniformManager.Uniform<?>, List<?>>() {

			@Override
			public void accept(UniformManager.Uniform<?> uniform, List<?> queue) {
				synchronized (queue) {
					try (MemoryStack stack = MemoryStack.stackPush()) {
						for (Iterator<?> i = queue.iterator(); i.hasNext();) {
							Object o = i.next();
							if (o != null) {
								upload(uniform.uploadSystem, uniform.location, stack, o);
								i.remove();

							}
						}
					}
				}

			}

		});
	}

	private <T> boolean upload(IShaderUniformUploadSystem<T> upload, Integer location, MemoryStack stack, Object o) {
		try {
			T t = (T) o;
			upload.uploadToShader(t, location, stack);
			return true;
		} catch (Exception e) {

		}
		return false;
	}

	/**
	 * Iterates through all the uniforms attached to system and generates their location Integers
	 * @param programID
	 * @param uniforms
	 */
	public void bindUniformLocations(int programID, Set<UniformManager.Uniform<?>> uniforms) {
		iterateUniforms(uniforms, new Consumer<UniformManager.Uniform<?>>() {

			@Override
			public void accept(UniformManager.Uniform<?> t) {
				t.location = generateID(programID, t.name);
			}
		});
	}

	private void iterateUniforms(Set<UniformManager.Uniform<?>> uniforms, Consumer<Uniform<?>> consumer) {

		if (uniforms != null && !uniforms.isEmpty()) {

			synchronized (uniforms) {
				for (Iterator<UniformManager.Uniform<?>> iterator = uniforms.iterator(); iterator.hasNext();) {
					UniformManager.Uniform<?> baseUniform = iterator.next();

					try (MemoryStack stack = MemoryStack.stackPush()) {
						UniformManager.Uniform<?> uniform = (UniformManager.Uniform<?>) baseUniform;
						if (consumer != null) {
							consumer.accept(uniform);
						}
					} catch (ClassCastException e) {
						System.err.println("Mis-match of uniform types and uniform system types");
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void iterateUniforms(Map<UniformManager.Uniform<?>, List<?>> uniforms, BiConsumer<Uniform<?>, List<?>> consumer) {

		if (uniforms != null && !uniforms.isEmpty()) {

			Set<Entry<UniformManager.Uniform<?>, List<?>>> set = uniforms.entrySet();
			synchronized (uniforms) {
				for (Iterator<Entry<UniformManager.Uniform<?>, List<?>>> iterator = set.iterator(); iterator.hasNext();) {
					Entry<UniformManager.Uniform<?>, List<?>> entry = iterator.next();

					try (MemoryStack stack = MemoryStack.stackPush()) {
						UniformManager.Uniform<?> uniform = (UniformManager.Uniform<?>) entry.getKey();
						List<?> queue = entry.getValue();
						if (consumer != null) {
							consumer.accept(uniform, queue);
						}
					} catch (ClassCastException e) {
						System.err.println("Mis-match of uniform types and uniform system types");
						e.printStackTrace();
					}
				}
			}
		}
	}

	protected Integer generateID(Integer programID, String name){
		int val = GL46.glGetUniformLocation(programID, name);
		return val;
	}

}
