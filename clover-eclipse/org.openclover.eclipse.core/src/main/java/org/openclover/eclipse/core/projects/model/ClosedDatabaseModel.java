package org.openclover.eclipse.core.projects.model;

import org.openclover.eclipse.core.projects.CloverProject;

public class ClosedDatabaseModel
    extends UnloadedDatabaseModel {

    public ClosedDatabaseModel(CloverProject project, CoverageModelChangeEvent changeEvent) {
        super(project, changeEvent);
    }

    @Override
    public void loadDbAndCoverage(CoverageModelChangeEvent changeEvent) {
        if (changeEvent.isUserInitiated()) {
            project.setModel(new LoadingDatabaseModel(this, changeEvent));
        }
    }

    @Override
    public void refreshCoverage(CoverageModelChangeEvent changeEvent) {
        loadDbAndCoverage(changeEvent);
    }

    @Override
    public void close(CoverageModelChangeEvent changeEvent) { }
}
