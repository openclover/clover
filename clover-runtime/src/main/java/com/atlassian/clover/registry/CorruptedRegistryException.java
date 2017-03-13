package com.atlassian.clover.registry;

public class CorruptedRegistryException extends RegistryFormatException {
    public CorruptedRegistryException(String reason) {
        super(reason);
    }

    public CorruptedRegistryException(String pathToRegistry, Throwable cause) {
        super(pathToRegistry, "This database may have been corrupted. Please regenerate.", cause);
    }
}
