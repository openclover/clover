package org.openclover.eclipse.core.projects.model;

import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.reporters.Current;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.runtime.api.CloverException;

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
