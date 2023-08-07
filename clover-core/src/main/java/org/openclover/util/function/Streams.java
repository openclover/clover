package org.openclover.util.function;

import java.util.Collection;

/**
 * @deprecated Will be replaced by Java streams API once support for JDK7 is dropped.
 */
public abstract class Streams {

    public static <S> Collection<S> filter(Collection<S> source, Predicate<? super S> predicate) {
        return null; // TODO implement
    }

    public static <S, T> Collection<T> map(Collection<S> source, Function<? super S, T> function) {
        return null; // TODO implement
    }
}
