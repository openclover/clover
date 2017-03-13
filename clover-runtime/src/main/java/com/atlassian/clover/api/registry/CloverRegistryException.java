package com.atlassian.clover.api.registry;

import com.atlassian.clover.api.CloverException;

public abstract class CloverRegistryException extends CloverException {
    public CloverRegistryException(String message) {
        super(message);
    }

    public CloverRegistryException(String message, Throwable t) {
        super(message);
        initCause(t);
    }
}
