package org.openclover.eclipse.core.projects.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.eclipse.core.CloverPlugin;

public class LoadingDatabaseModel
    extends VolatileDatabaseModel {

    protected StableDatabaseModel currentModel;
    protected LoadDatabaseJob loadingJob;

    public LoadingDatabaseModel(StableDatabaseModel currentModel, CoverageModelChangeEvent changeEvent) {
        super(currentModel.project, changeEvent);
        this.currentModel = currentModel;
    }

    @Override
    public CloverDatabase getDatabase() {
        return currentModel.getDatabase();
    }

    @Override
    public void close(CoverageModelChangeEvent changeEvent) {
        project.setModel(new ClosedDatabaseModel(project, changeEvent));
    }

    @Override
    public boolean isLoaded() { return currentModel.isLoaded(); }
    @Override
    public boolean isLoading() { return true; }

    @Override
    public boolean isCoverageOutOfDate() { return false; }
    @Override
    public boolean isRegistryOfDate() { return false; }

    protected void onLoadOK(IJobChangeEvent event) {
        CloverPlugin.logVerbose("Database load succeeded for project " + project.getName());

        CloverPlugin.logVerbose("Attempting to set coverage model to loaded for project " + project.getName());
        final boolean includeFailedCoverage = CloverPlugin.getInstance().getInstallationSettings().isIncludeFailedCoverage();
        project.compareAndSetModel(this,
                new LoadedDatabaseModel(project, ((LoadDatabaseJob)event.getJob()).getDatabase(), changeEvent, includeFailedCoverage) );
    }

    protected void onLoadNotOK(IJobChangeEvent event) {
        CloverPlugin.logVerbose("Database load failed: " + (event.getResult() == null ? "unknown reason" : event.getResult().getMessage()));

        final CloverDatabase database = ((LoadDatabaseJob)event.getJob()).getDatabase();
        final boolean includeFailedCoverage = CloverPlugin.getInstance().getInstallationSettings().isIncludeFailedCoverage();
        if (database != null) {
            project.compareAndSetModel(this,
                    new LoadedDatabaseModel(project, database, changeEvent, includeFailedCoverage) );
        } else {
            project.compareAndSetModel(this,
                    new LoadedDatabaseModel(project, project.newEmptyDatabase(loadingJob.contextFilter), changeEvent, includeFailedCoverage) );
        }
    }

    @Override
    public void onActivation(DatabaseModel predecessor) {
        super.onActivation(predecessor);

        loadingJob = createJob();
        loadingJob.addJobChangeListener(
            new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    if (event.getResult().isOK()) {
                        onLoadOK(event);
                    } else {
                        onLoadNotOK(event);
                    }
                }
            }
        );
        CloverPlugin.logVerbose("Starting database load for " + this);
        loadingJob.schedule();
    }

    protected LoadDatabaseJob createJob() {
        return new LoadDatabaseJob(project, changeEvent);
    }

    /**
     * Brings forward the database load for the calling thread. Needed to avoid
     * deadlock where, on launch, an autobuild starts with (scheduling rule == project)
     * and there's a load coverage job (scheduling rule == project) but the
     * auto build needs the db loaded first.
     *
     * This method does not change the status of the loading job, merely brings forward
     * the work it does. Subsequent execution of the job will (once scheduling permits)
     * will simply just reuse the work done here. 
     */
    @Override
    public CloverDatabase forcePrematureLoad(IProgressMonitor monitor) {
        final CloverDatabase database;
        if (loadingJob != null) {
            loadingJob.doLoad(monitor);
            database = loadingJob.getDatabase();
        } else {
            database = null;
        }
        return database;
    }

    @Override
    public void onDeactication(DatabaseModel successor) {
        if (loadingJob != null) {
            if (loadingJob.getState() != Job.NONE) {
                CloverPlugin.logVerbose("Cancelling database load for " + this);
            }
            Job.getJobManager().cancel(loadingJob);
        }
    }

    @Override
    public ProjectInfo getFullProjectInfo() {
        return currentModel.getFullProjectInfo();
    }

    @Override
    public ProjectInfo getTestOnlyProjectInfo() {
        return currentModel.getTestOnlyProjectInfo();
    }

    @Override
    public ProjectInfo getAppOnlyProjectInfo() {
        return currentModel.getAppOnlyProjectInfo();
    }

    @Override
    public HasMetrics getPackageInfoOrFragment(IPackageFragment pack, MetricsScope scope) {
        return currentModel.getPackageInfoOrFragment(pack, scope);
    }

    @Override
    public FileInfo getSourceFileInfo(ICompilationUnit cu, MetricsScope scope) {
        return currentModel.getSourceFileInfo(cu, scope);
    }

    @Override
    public ClassInfo getTypeInfo(IType type, MetricsScope scope) {
        return currentModel.getTypeInfo(type, scope);
    }

    @Override
    public TestCaseInfo getTestCaseInfo(IMethod method, MetricsScope scope) {
        return currentModel.getTestCaseInfo(method, scope);
    }

    @Override
    public TestCaseInfo[] getTestCaseInfos(IMethod method, MetricsScope scope) {
        return currentModel.getTestCaseInfos(method, scope);
    }

    @Override
    public FullMethodInfo getMethodInfo(IMethod method, MetricsScope scope) {
        return currentModel.getMethodInfo(method, scope);
    }

    @Override
    public HasMetrics metricsProviderFor(Object projectArtifact, MetricsScope scope) {
        return currentModel.metricsProviderFor(projectArtifact, scope);
    }
}