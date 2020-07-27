package com.meeple.backend.events;

public class BasicEventWrapper<Wrapped> extends EventBase {
	private final Wrapped wrapped;

	public BasicEventWrapper(Wrapped wrapped) {
		this.wrapped = wrapped;
	}

	public Wrapped getWrapped() {
		return wrapped;
	}

}
