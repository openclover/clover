package com.atlassian.clover.registry.format;

public class RegistryLoadException extends RuntimeException {
    public RegistryLoadException(Throwable cause) {
        super(cause);
    }

    public RegistryLoadException(String message, Exception cause) {
        super(message, cause);
    }
}
