package com.meeple.shared.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class CollectionSuppliers {

	public static final class SetSupplier<T> implements Supplier<Set<T>> {

		@Override
		public Set<T> get() {
			return Collections.synchronizedSet(new HashSet<>());
		}
	}

	public static final class MapSupplier<K, V> implements Supplier<Map<K, V>> {

		@Override
		public Map<K, V> get() {
			return Collections.synchronizedMap(new HashMap<>());
		}
	}
	
	public static final class ListSupplier<T> implements Supplier<List<T>> {

		@Override
		public List<T> get() {
			return Collections.synchronizedList(new ArrayList<>());
		}
	}


}
