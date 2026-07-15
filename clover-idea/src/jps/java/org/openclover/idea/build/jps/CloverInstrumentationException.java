package org.openclover.idea.build.jps;

import org.jetbrains.jps.builders.java.JavaSourceTransformer;

/**
 * Exception thrown in case when code instrumentation fails
 */
public class CloverInstrumentationException extends JavaSourceTransformer.TransformError {
    public CloverInstrumentationException(Throwable cause) {
        super(cause);
    }

    public CloverInstrumentationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloverInstrumentationException(String message) {
        super(message);
    }
}
