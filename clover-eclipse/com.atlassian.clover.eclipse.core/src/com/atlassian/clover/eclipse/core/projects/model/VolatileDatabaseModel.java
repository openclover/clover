package com.atlassian.clover.eclipse.core.projects.model;

import com.atlassian.clover.eclipse.core.projects.CloverProject;

public abstract class VolatileDatabaseModel extends DatabaseModel {
    public VolatileDatabaseModel(CloverProject project, CoverageModelChangeEvent changeEvent) {
        super(project, changeEvent);
    }

    @Override
    public void loadDbAndCoverage(CoverageModelChangeEvent changeEvent) {
        //Do nothing as we are already loading
    }

    @Override
    public void refreshCoverage(CoverageModelChangeEvent changeEvent) {
        //Do nothing as we are already loading
    }
}
