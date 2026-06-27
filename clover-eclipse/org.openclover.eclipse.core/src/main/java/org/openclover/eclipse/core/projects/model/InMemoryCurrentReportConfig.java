package org.openclover.eclipse.core.projects.model;

import org.openclover.core.CloverDatabase;
import org.openclover.core.reporters.Current;

public class InMemoryCurrentReportConfig extends Current {
    private final DatabaseModel inMemoryModel;

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
    public CloverDatabase getCoverageDatabase() {
        return inMemoryModel.getDatabase();
    }

}
