package org.openclover.core.services;

public class ServiceNotAvailableException extends RuntimeException {
    public ServiceNotAvailableException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
