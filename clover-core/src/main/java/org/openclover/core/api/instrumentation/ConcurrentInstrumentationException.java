package org.openclover.core.api.instrumentation;

import org.openclover.runtime.api.registry.CloverRegistryException;

public class ConcurrentInstrumentationException extends CloverRegistryException {
    public ConcurrentInstrumentationException(String message) {
        super(message);
    }
}