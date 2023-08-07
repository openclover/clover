package org.openclover.util.function;

/**
 * @deprecated Will be replaced by {@see java.util.function.Predicate#negate()} once JDK7 is dropped.
 */
public abstract class Predicates {
    public static <T> Predicate<T> negate(Predicate<T> predicate) {
        return new Not(predicate);
    }

    private static class Not<S> implements Predicate<S> {
        private final Predicate<S> predicate;

        public Not(Predicate<S> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean test(S data) {
            return !predicate.test(data);
        }
    }
}
