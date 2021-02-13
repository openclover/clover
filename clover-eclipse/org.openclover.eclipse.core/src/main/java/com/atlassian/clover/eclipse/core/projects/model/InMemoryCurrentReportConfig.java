package com.atlassian.clover.eclipse.core.projects.model;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.reporters.Current;

public class InMemoryCurrentReportConfig extends Current {
    private DatabaseModel inMemoryModel;

    public InMemoryCurrentReportConfig(DatabaseModel inMemoryModel) {
        super();
        this.inMemoryModel = inMemoryModel;
    }

    public InMemoryCurrentReportConfig(DatabaseModel inMemoryModel, Current other) {
        super(other);
        this.inMemoryModel = inMemoryModel;
    }

    /** We supply the already loaded and decorated CloverDatabase instance, no polishing needed */
    @Override
    public CloverDatabase getCoverageDatabase() throws CloverException {
        return inMemoryModel.getDatabase();
    }

    private HasMetricsFilter getWorkingSetFilter() {
        return
            CloverPlugin.getInstance().isInWorkingSetMode()
                ? new WorkingSetHasMetricsFilter(inMemoryModel.getProject())
                : HasMetricsFilter.ACCEPT_ALL;
    }
}
