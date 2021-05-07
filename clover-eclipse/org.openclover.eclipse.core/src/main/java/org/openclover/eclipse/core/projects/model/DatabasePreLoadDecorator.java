package org.openclover.eclipse.core.projects.model;

import org.openclover.eclipse.core.projects.CloverProject;
import org.eclipse.core.runtime.IProgressMonitor;

public interface DatabasePreLoadDecorator extends CoverageLoadDecorator {
    public static final DatabasePreLoadDecorator[] NONE = new DatabasePreLoadDecorator[] {};
    public static final DatabasePreLoadDecorator NULL = new DatabasePreLoadDecorator() {
        @Override
        public void decorate(CloverProject project, IProgressMonitor monitor) {}
    };

    public void decorate(CloverProject project, IProgressMonitor monitor) throws Exception;
}
