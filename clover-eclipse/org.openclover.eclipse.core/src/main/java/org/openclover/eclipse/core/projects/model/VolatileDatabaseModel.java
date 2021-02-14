package org.openclover.eclipse.core.projects.model;

import org.openclover.eclipse.core.projects.CloverProject;

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
