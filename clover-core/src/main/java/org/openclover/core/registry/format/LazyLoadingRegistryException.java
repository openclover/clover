package org.openclover.core.registry.format;

public class LazyLoadingRegistryException extends RuntimeException {
    public LazyLoadingRegistryException(String message) {
        super(message);
    }

    public LazyLoadingRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
