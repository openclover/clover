package com.atlassian.clover.registry.format;

import com.atlassian.clover.api.registry.CloverRegistryException;

public class InaccessibleRegFileException extends CloverRegistryException {
    public InaccessibleRegFileException(String message) {
        super(message);
    }
}
