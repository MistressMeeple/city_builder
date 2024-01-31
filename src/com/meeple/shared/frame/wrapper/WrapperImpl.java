package com.meeple.shared.frame.wrapper;

public class WrapperImpl<T> implements Wrapper<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8872279501217129707L;

	public WrapperImpl() {
	}

	public WrapperImpl(T init) {
		this.wrapped = init;
	}

	private T wrapped;

	@Override
	public T get() {
		return wrapped;
	}

	public void set(T wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public T getOrDefault(T defaultValue) {
		if (wrapped == null) {
			return defaultValue;
		}
		return wrapped;
	}
}
