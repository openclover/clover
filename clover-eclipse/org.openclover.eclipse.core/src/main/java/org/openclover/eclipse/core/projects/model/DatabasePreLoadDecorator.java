package org.openclover.eclipse.core.projects.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openclover.eclipse.core.projects.CloverProject;

public interface DatabasePreLoadDecorator extends CoverageLoadDecorator {
    DatabasePreLoadDecorator[] NONE = new DatabasePreLoadDecorator[] {};
    DatabasePreLoadDecorator NULL = (project, monitor) -> {};

    void decorate(CloverProject project, IProgressMonitor monitor) throws Exception;
}
