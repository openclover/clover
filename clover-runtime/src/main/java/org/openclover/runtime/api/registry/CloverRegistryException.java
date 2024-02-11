package org.openclover.runtime.api.registry;

import org.openclover.runtime.api.CloverException;

public abstract class CloverRegistryException extends CloverException {
    public CloverRegistryException(String message) {
        super(message);
    }

    public CloverRegistryException(String message, Throwable t) {
        super(message);
        initCause(t);
    }
}
