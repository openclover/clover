package org.openclover.util.function;

/**
 * Check if the predicate for given data is true.
 * @deprecated Will be replaced by {@see java.util.function.Predicate} once JDK7 support is dropped.
 */
public interface Predicate<S> {
    boolean test(S data);
}
