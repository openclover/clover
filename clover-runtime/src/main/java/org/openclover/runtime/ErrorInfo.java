package org.openclover.runtime;

import java.io.Serializable;

/**
 * A lightweight class to hold data about a test failure/error.
 */
public class ErrorInfo implements Serializable {
    private final String message;
    private final String stackTrace;

    public ErrorInfo(String message, String stackTrace) {
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public String toString() {
        return "ErrorInfo{" +
                "message='" + message + '\'' +
                ", stackTrace='" + stackTrace + '\'' +
                '}';
    }
}
