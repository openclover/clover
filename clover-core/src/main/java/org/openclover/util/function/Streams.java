package org.openclover.util.function;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @deprecated Will be replaced by Java streams API once support for JDK7 is dropped.
 */
public abstract class Streams {

    public static <S> Collection<S> filter(Collection<S> source, Predicate<? super S> predicate) {
        final Collection<S> target = new ArrayList<>();
        for (S element : source) {
            if (predicate.test(element)) {
                target.add(element);
            }
        }
        return target;
    }

    public static <S, T> Collection<T> map(Collection<S> source, Function<? super S, T> function) {
        final Collection<T> target = new ArrayList<>();
        for (S element : source) {
            target.add(function.apply(element));
        }
        return target;
    }

    public static <S> boolean matchesAny(Collection<S> source, Predicate<? super S> predicate) {
        for (S element : source) {
            if (predicate.test(element)) {
                return true;
            }
        }
        return false;
    }
}
