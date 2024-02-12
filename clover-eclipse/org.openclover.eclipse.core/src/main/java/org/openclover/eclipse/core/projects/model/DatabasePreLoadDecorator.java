package org.openclover.eclipse.core.projects.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openclover.eclipse.core.projects.CloverProject;

public interface DatabasePreLoadDecorator extends CoverageLoadDecorator {
    public static final DatabasePreLoadDecorator[] NONE = new DatabasePreLoadDecorator[] {};
    public static final DatabasePreLoadDecorator NULL = (project, monitor) -> {};

    public void decorate(CloverProject project, IProgressMonitor monitor) throws Exception;
}
