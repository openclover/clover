package org.openclover.eclipse.core.projects.model;

import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.core.CloverDatabase;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

public class RefreshCoverageJob extends LoadDatabaseJob {
    private final CloverDatabase database;

    public RefreshCoverageJob(CloverProject project, CloverDatabase database, CoverageModelChangeEvent changeEvent) {
        super("Loading coverage for project " + project.getName(), project, changeEvent);
        this.database = database;
    }

    @Override
    public IStatus doLoad(IProgressMonitor monitor) {
        if (hasRun.compareAndSet(false, true)) {
            try {
                database.loadCoverageData(project.newCoverageDataSpec(database));
                status = Status.OK_STATUS;
            } catch (Throwable t) {
                CloverPlugin.logError("Failed to load coverage data", t);
                status = new Status(
                    Status.WARNING,
                    CloverPlugin.ID,
                    LOAD_FAILED,
                    "Background job: failed to load coverage data for project " + project.getName(),
                    t);
            }
            setDatabase(database);
        }
        return status;
    }
}
