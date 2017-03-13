package com.atlassian.clover.registry;

public class IncompatibleRegistryFormatException extends RegistryFormatException {
    public IncompatibleRegistryFormatException(String reason) {
        super(reason);
    }
}
