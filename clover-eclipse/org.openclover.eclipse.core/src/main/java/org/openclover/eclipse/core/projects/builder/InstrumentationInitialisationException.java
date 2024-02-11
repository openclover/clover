package org.openclover.eclipse.core.projects.builder;

import org.openclover.runtime.api.CloverException;

public class InstrumentationInitialisationException extends InstrumentationException {
    public InstrumentationInitialisationException(CloverException e) {
        super(e);
    }
}
