package org.myjtools.openbbt.core.util;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class Functions {

	private Functions() { }

	public static <T,U> U let(T value, Function<T,U> mapper) {
		if (value == null) return null;
		return mapper.apply(value);
	}

	public static <T> T or(T value, T fallback) {
		return value == null ? fallback : value;
	}

	public static <T> T or(T value, Supplier<T> fallback) {
		return value == null ? fallback.get() : value;
	}

	public static <T> T also(T value, Consumer<T> action) {
		if (value == null) return null;
		action.accept(value);
		return value;
	}

	public static <T> void ifPresent(T value, Consumer<T> action) {
		if (value != null) action.accept(value);
	}

	public static <T, U extends T> U cast(T value, Class<U> type) {
		if (value == null) return null;
		return type.isInstance(value) ? type.cast(value) : null;
	}

	public static <T> T first(List<T> list) {
		return list.isEmpty() ? null : list.get(0);
	}

	public static <T,U> List<U> indexMapped(List<T> list, BiFunction<Integer,T,U> mapper) {
		if (list == null) return List.of();
		List<U> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) {
			result.add(mapper.apply(i,list.get(i)));
		}
		return List.copyOf(result);
	}

	public static <T,U> List<U> mapped(List<T> list, Function<T,U> mapper) {
		if (list == null) return List.of();
		return list.stream().map(mapper).toList();
	}

	public static <T> List<T> concat(List<T> list1, List<T> list2) {
		return Stream.concat(list1.stream(), list2.stream()).toList();
	}

}
