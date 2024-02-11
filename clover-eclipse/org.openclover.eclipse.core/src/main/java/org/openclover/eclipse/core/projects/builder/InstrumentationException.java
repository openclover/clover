package org.openclover.eclipse.core.projects.builder;

import org.openclover.runtime.api.CloverException;
import org.openclover.eclipse.core.CloverPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

public class InstrumentationException extends CoreException {
    public InstrumentationException(CloverException e) {
        super(new Status(Status.ERROR, CloverPlugin.ID, 0, e.getMessage(), e));
    }
}
