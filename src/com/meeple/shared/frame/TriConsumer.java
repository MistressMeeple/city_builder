package com.meeple.shared.frame;

@FunctionalInterface
public interface TriConsumer<O1, O2, O3> {

	/**
	 * Performs this operation on the given arguments.
	 *
	 * @param t the first input argument
	 * @param u the second input argument
	 */
	void accept(O1 o1, O2 o2, O3 o3);

}
