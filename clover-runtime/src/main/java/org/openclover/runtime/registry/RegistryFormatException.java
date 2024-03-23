package org.openclover.runtime.registry;

import org.openclover.runtime.api.registry.CloverRegistryException;

public abstract class RegistryFormatException extends CloverRegistryException {
    protected RegistryFormatException(String reason) {
        super(reason);
    }

    public RegistryFormatException(String pathToRegistry, String probableCauseMessage, Throwable cause) {
        super("OpenClover encountered a problem reading the instrumentation registry \"" + pathToRegistry + "\". " + probableCauseMessage, cause);
    }
}
