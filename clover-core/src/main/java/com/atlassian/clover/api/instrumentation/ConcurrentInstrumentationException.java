package com.atlassian.clover.api.instrumentation;

import com.atlassian.clover.api.registry.CloverRegistryException;

public class ConcurrentInstrumentationException extends CloverRegistryException {
    public ConcurrentInstrumentationException(String message) {
        super(message);
    }
}