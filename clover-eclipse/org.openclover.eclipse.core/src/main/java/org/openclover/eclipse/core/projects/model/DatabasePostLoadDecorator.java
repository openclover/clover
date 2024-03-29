package org.openclover.eclipse.core.projects.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openclover.core.CloverDatabase;
import org.openclover.eclipse.core.projects.CloverProject;

public interface DatabasePostLoadDecorator extends CoverageLoadDecorator {
    public static final DatabasePostLoadDecorator[] NONE = new DatabasePostLoadDecorator[] {};
    public static final DatabasePostLoadDecorator NULL = (project, database, monitor) -> {};
    
    public void decorate(CloverProject project, CloverDatabase database, IProgressMonitor monitor) throws Exception;
}
