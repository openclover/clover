package com.atlassian.clover.services;

public class ServiceNotAvailableException extends RuntimeException {
    public ServiceNotAvailableException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
