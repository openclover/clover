package org.openclover.eclipse.core.projects.builder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.runtime.api.CloverException;

public class InstrumentationException extends CoreException {
    public InstrumentationException(CloverException e) {
        super(new Status(Status.ERROR, CloverPlugin.ID, 0, e.getMessage(), e));
    }
}
