package org.openclover.eclipse.core.projects.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openclover.core.CloverDatabase;
import org.openclover.eclipse.core.projects.CloverProject;

public interface DatabasePostLoadDecorator extends CoverageLoadDecorator {
    DatabasePostLoadDecorator[] NONE = new DatabasePostLoadDecorator[] {};
    DatabasePostLoadDecorator NULL = (project, database, monitor) -> {};
    
    void decorate(CloverProject project, CloverDatabase database, IProgressMonitor monitor);
}
