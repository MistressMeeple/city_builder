package com.meeple.shared.frame.wrapper;

import java.io.Serializable;

/**
 * Standard implimentation is {@link WrapperImpl}
 * @author Megan
 *
 * @param <T>
 */
public interface Wrapper<T> extends Serializable {

	public T get();

	public T getOrDefault(T defaultValue);

	public void set(T wrapped);
}
