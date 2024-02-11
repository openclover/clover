package org.openclover.eclipse.core.reports;

import org.eclipse.core.runtime.jobs.Job;
import org.openclover.core.reporters.Current;

/**
 *
 */
abstract class ReportJob extends Job {
    public ReportJob() {
        super("Clover report generation");
    }

    public abstract Current getConfig();
}
