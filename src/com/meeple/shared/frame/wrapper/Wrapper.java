package com.meeple.shared.frame.wrapper;

import java.io.Serializable;

/**
 * Standard implimentation is {@link WrapperImpl}
 * @author Megan
 *
 * @param <T>
 */
public interface Wrapper<T> extends Serializable {

	public T getWrapped();

	public T getWrappedOrDefault(T defaultValue);

	public void setWrapped(T wrapped);
}
