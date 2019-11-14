package com.meeple.components;

public interface NamedComponent extends Component {
	public static final String nameTag= "name";

	public default String getName() {
		return Component.get(this, nameTag);
	}

	public default void setName(String name) {
		getComponents().put(nameTag, name);
	}
}
