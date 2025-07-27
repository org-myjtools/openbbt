package org.myjtools.openbbt.core.util;

import java.util.function.Supplier;

public class Pair<T,U>  {

    public static <T,U> Pair<T,U> of(Supplier<T> value, Supplier<U> fallback) {
        return new Pair<>(value,fallback);
    }

    public static <T,U> Pair<T,U> of(T value, U fallback) {
        return new Pair<>(()->value,()->fallback);
    }


    private final Supplier<T> left;
    private final Supplier<U> right;


    private Pair(Supplier<T> left, Supplier<U> right) {
        this.left = left;
        this.right = right;
    }


    public T left() {
        return left.get();
    }

    public U right() {
        return right.get();
    }


}
