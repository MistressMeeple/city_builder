package com.meeple.components;

public interface IDComponent<T> extends Component {

	public static final String idTag = "name";

	public default T getID() {
		return Component.get(this, idTag);
	}

	public default void setID(T id) {
		getComponents().put(idTag, id);
	}
}
