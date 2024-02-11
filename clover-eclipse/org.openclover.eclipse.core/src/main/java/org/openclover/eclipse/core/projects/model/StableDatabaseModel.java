package org.openclover.eclipse.core.projects.model;

import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.core.CloverDatabase;
import org.eclipse.core.runtime.IProgressMonitor;

public abstract class StableDatabaseModel extends DatabaseModel {
    protected StableDatabaseModel(CloverProject project, CoverageModelChangeEvent changeEvent) {
        super(project, changeEvent);
    }

    @Override
    public void loadDbAndCoverage(CoverageModelChangeEvent changeEvent) {
        project.compareAndSetModel(this, new LoadingDatabaseModel(this, changeEvent));
    }

    @Override
    public void refreshCoverage(CoverageModelChangeEvent changeEvent) {
        project.compareAndSetModel(this, new LoadingDatabaseModel(this, changeEvent));
    }

    @Override
    public void close(CoverageModelChangeEvent changeEvent) {
        project.compareAndSetModel(this, new ClosedDatabaseModel(project, changeEvent));
    }

    @Override
    public CloverDatabase forcePrematureLoad(IProgressMonitor monitor) { return null; }
}
