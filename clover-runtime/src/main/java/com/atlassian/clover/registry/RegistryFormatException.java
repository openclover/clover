package com.atlassian.clover.registry;

import com.atlassian.clover.api.registry.CloverRegistryException;

public abstract class RegistryFormatException extends CloverRegistryException {
    protected RegistryFormatException(String reason) {
        super(reason);
    }

    public RegistryFormatException(String pathToRegistry, String probableCauseMessage, Throwable cause) {
        super("Clover encountered a problem reading the instrumentation registry \"" + pathToRegistry + "\". " + probableCauseMessage, cause);
    }
}
