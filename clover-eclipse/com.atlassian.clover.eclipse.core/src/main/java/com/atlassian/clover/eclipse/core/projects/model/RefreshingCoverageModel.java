package com.atlassian.clover.eclipse.core.projects.model;

import com.atlassian.clover.CloverDatabase;

public class RefreshingCoverageModel extends LoadingDatabaseModel {
    private final CloverDatabase database;

    public RefreshingCoverageModel(LoadedDatabaseModel model, CloverDatabase database, CoverageModelChangeEvent event) {
        super(model, event);
        this.database = database;
    }

    @Override
    protected LoadDatabaseJob createJob() {
        return new RefreshCoverageJob(project, database, changeEvent);
    }
}
