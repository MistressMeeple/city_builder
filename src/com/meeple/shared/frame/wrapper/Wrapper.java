package com.meeple.shared.frame.wrapper;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Standard implimentation is {@link WrapperImpl}
 * @author Megan
 *
 * @param <T>
 */
public interface Wrapper<T> extends Serializable, Supplier<T> {

	public T get();

	public T getOrDefault(T defaultValue);

	public void set(T wrapped);
}
