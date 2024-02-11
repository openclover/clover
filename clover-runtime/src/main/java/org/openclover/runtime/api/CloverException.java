package org.openclover.runtime.api;

public class CloverException extends Exception {
    public CloverException(String reason) {
        super(reason);
    }

    public CloverException(Throwable cause) {
        super(cause.toString() != null ? cause.toString() : cause.getClass().getName(), cause);
    }


    public CloverException(String message, Throwable cause) {
        super(message, cause);
    }
}
