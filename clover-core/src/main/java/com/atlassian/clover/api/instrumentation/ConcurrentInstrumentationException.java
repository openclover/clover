package com.atlassian.clover.api.instrumentation;

import org.openclover.runtime.api.registry.CloverRegistryException;

public class ConcurrentInstrumentationException extends CloverRegistryException {
    public ConcurrentInstrumentationException(String message) {
        super(message);
    }
}