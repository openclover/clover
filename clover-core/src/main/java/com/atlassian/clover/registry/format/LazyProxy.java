package com.atlassian.clover.registry.format;

/** A registry-specific lazy proxy */
public interface LazyProxy<T> {
    /** For where lazy loading is not necessary */
    public static final class Preloaded<T> implements LazyProxy<T> {
        private final T result;

        public Preloaded(T result) {
            this.result = result;
        }

        @Override
        public T get() {
            return result;
        }
    }

    public T get() throws RegistryLoadException;

}
