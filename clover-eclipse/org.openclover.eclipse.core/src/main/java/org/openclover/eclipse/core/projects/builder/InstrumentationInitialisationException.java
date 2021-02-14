package org.openclover.eclipse.core.projects.builder;

import com.atlassian.clover.api.CloverException;

public class InstrumentationInitialisationException extends InstrumentationException {
    public InstrumentationInitialisationException(CloverException e) {
        super(e);
    }
}
