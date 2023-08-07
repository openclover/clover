package org.openclover.util.function;

/**
 * @deprecated Will be replaced by {@see java.util.function.Function} once JDK7 support is dropped.
 */
public interface Function<S, T> {
    T apply(S t);
}
