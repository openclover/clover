package org.openclover.eclipse.core.projects.model;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.projects.DatabaseChangeListener;
import org.openclover.eclipse.core.ui.projects.DatabaseChangeEvent;
import org.openclover.eclipse.core.settings.InstallationSettings;
import org.openclover.eclipse.core.projects.CloverProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Runs a job to detect coverage changes and propgate these to listeners.
 */
public class CoverageModelsMonitor {
    private final Object OWNER_KEY = new Object();
    private final List<DatabaseChangeListener> listeners;

    public CoverageModelsMonitor() {
        this.listeners = Collections.synchronizedList(new ArrayList<DatabaseChangeListener>(2));
    }

    public void start() {
        final MonitoringJob job = new MonitoringJob();
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                if (event.getResult().isOK()) {
                } else if (event.getResult().getSeverity() != IStatus.CANCEL) {
                    CloverPlugin.logWarning("Job " + job.getName() + " failed: " + event.getResult(), event.getResult().getException());
                }
            }
        });
        job.schedule();
    }

    public void stop() {
        Job.getJobManager().cancel(OWNER_KEY);
    }

    public void addCoverageChangeListener(DatabaseChangeListener listener) {
        listeners.add(listener);
    }

    public void removeCoverageChangeListener(DatabaseChangeListener listener) {
        listeners.remove(listener);
    }

    public void fireCoverageChange() {
        fireCoverageChange(null, null, null);
    }

    public void fireCoverageChange(CloverProject project) {
        DatabaseModel model = project.getModel();
        fireCoverageChange(project, model, model);
    }

    public void fireCoverageChange(CloverProject project, DatabaseModel oldModel, DatabaseModel newModel) {
        Collection<DatabaseChangeListener> listenersCopy;
        synchronized (this.listeners) {
            listenersCopy = Collections.unmodifiableList(new ArrayList<DatabaseChangeListener>(this.listeners));
        }

        DatabaseChangeEvent event = new DatabaseChangeEvent(project, oldModel, newModel);
        for (DatabaseChangeListener listener : listenersCopy) {
            try {
                listener.databaseChanged(event);
            }
            catch (Throwable t) {
                CloverPlugin.logError("Error while notifying listener of coverage change", t);
            }
        }
    }

    private void refreshAllOpenCloverProjects() {
        CloverProject.refreshAllModels(false, false);
    }

    class MonitoringJob extends Job {
        public MonitoringJob() {
            super("Clover Coverage Monitor");

            //It's unclear at this stage which is most appropriate. LONG seemed reasonable.
            setPriority(Job.LONG);

            //Don't want a progress bar
            setSystem(true);
        }

        @Override
        public boolean belongsTo(Object family) {
            return family == OWNER_KEY;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                //CloverPlugin.logDebug("Job " + this.getName() + " running");

                //If auto refresh turned off, we still run periodically, we just don't refresh
                if (CloverPlugin.getInstance().getInstallationSettings().isAutoRefreshingCoverage()) {
                    refreshAllOpenCloverProjects();
                }

                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                } else {
                    return Status.OK_STATUS;
                }
            } catch (Exception e) {
                CloverPlugin.logError("Failed to refresh coverage", e);
                //Try again, without getting in the user's face
                return Status.OK_STATUS;
            } finally {
                if (!monitor.isCanceled()) {
                    long msUntilNextRun = 0;
                    if (CloverPlugin.getInstance().getInstallationSettings().isAutoRefreshingCoverage()) {
                        //Ensure we only ever run at most every 5 seconds, regardless of what
                        //the preferences say
                        msUntilNextRun =
                            Math.max(
                                InstallationSettings.Values.FIVE_SECONDS_COVERAGE_REFRESH_PERIOD,
                                CloverPlugin.getInstance().getInstallationSettings().getCoverageRefreshPeriod());
                    } else {
                        msUntilNextRun = InstallationSettings.Values.TEN_SECONDS_COVERAGE_REFRESH_PERIOD;
                    }
                    schedule(msUntilNextRun);
                }
            }
        }
    }
}
