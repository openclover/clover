package org.openclover.eclipse.core.projects.model;

import com.atlassian.clover.CloverDatabase;
import org.openclover.eclipse.core.projects.CloverProject;
import org.eclipse.core.runtime.IProgressMonitor;

public interface DatabasePostLoadDecorator extends CoverageLoadDecorator {
    public static final DatabasePostLoadDecorator[] NONE = new DatabasePostLoadDecorator[] {};
    public static final DatabasePostLoadDecorator NULL = new DatabasePostLoadDecorator() {
        @Override
        public void decorate(CloverProject project, CloverDatabase database, IProgressMonitor monitor) {}
    };
    
    public void decorate(CloverProject project, CloverDatabase database, IProgressMonitor monitor) throws Exception;
}
