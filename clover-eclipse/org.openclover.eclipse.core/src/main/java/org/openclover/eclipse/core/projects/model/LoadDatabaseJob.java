package org.openclover.eclipse.core.projects.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.openclover.core.CloverDatabase;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.openclover.runtime.api.CloverException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.openclover.eclipse.core.CloverPlugin.logError;
import static org.openclover.eclipse.core.CloverPlugin.logVerbose;

public class LoadDatabaseJob extends Job {
    private static final QualifiedName DATABASE_PROPERTY = new QualifiedName(CloverPlugin.ID, "database");

    /* Ensures only one database load occurs at any one time */
    protected static final ISchedulingRule MUTEX_SCHEDULING_RULE = new ISchedulingRule() {
        @Override
        public boolean contains(ISchedulingRule schedulingRule) {
            return schedulingRule == this;
        }

        @Override
        public boolean isConflicting(ISchedulingRule schedulingRule) {
            return schedulingRule == this;
        }
    };

    public static final int LOAD_FAILED = 0;

    protected AtomicBoolean hasRun;
    protected volatile IStatus status;
    protected final CloverProject project;
    protected final DatabasePreLoadDecorator[] preLoadDecorators;
    protected final DatabasePostLoadDecorator[] postLoadDecorators;
    protected final ContextSet contextFilter;

    public LoadDatabaseJob(CloverProject project, CoverageModelChangeEvent changeEvent) {
        this("Loading OpenClover database for project " + project.getName(), project, changeEvent);
    }

    protected LoadDatabaseJob(String description, CloverProject project, CoverageModelChangeEvent changeEvent) {
        super(description);

        setPriority(Job.DECORATE);
        setSystem(!changeEvent.isUserInitiated());
        setUser(changeEvent.isUserInitiated());
        //Ensures only one database load occurs at any one time,
        //and never when something interesting is happening in the project in question
        setRule(
            new MultiRule(
                new ISchedulingRule[] {
                    MUTEX_SCHEDULING_RULE,
                    project.getProject()
                })
            );

        this.hasRun = new AtomicBoolean(false);
        this.project = project;
        //Take a copy in case it changes during load
        this.contextFilter = project.getSettings().getContextFilter();
        this.preLoadDecorators = changeEvent.getPreChangeDecorators();
        this.postLoadDecorators = changeEvent.getPostChangeDecorators();
    }

    protected void setDatabase(CloverDatabase database) {
        setProperty(DATABASE_PROPERTY, database);
    }

    public CloverDatabase getDatabase() {
        return (CloverDatabase)getProperty(LoadDatabaseJob.DATABASE_PROPERTY);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        return doLoad(monitor);
    }

    public IStatus doLoad(IProgressMonitor monitor) {
        if (hasRun.compareAndSet(false, true)) {
            logVerbose("Loading coverage for project " + project.getName());
            ProjectSettings settings = project.getSettings();
            try {
                File registryFile = project.getRegistryFile();
                if (registryFile.exists()) {
                    setDatabase(loadDbAndCoverage(settings, registryFile, monitor));
                } else {
                    setDatabase(createNewDb(false, monitor));
                }
                status = Status.OK_STATUS;
            } catch (Throwable t) {
                try {
                    setDatabase(loadCoverage(createNewDb(true, monitor), monitor));
                } catch (Exception e) {
                    logError("Failed to create a fresh database after failing to load", e);
                }
                status = new Status(
                    Status.WARNING,
                    CloverPlugin.ID,
                    LOAD_FAILED,
                    "Background job: failed to load OpenClover database for project " + project.getName(),
                    t);
            }
        }
        return status;
    }

    private CloverDatabase createNewDb(boolean becauseOfError, IProgressMonitor monitor) {
        preLoadDecorators(monitor);

        //We still try to load coverage because that will clear out old coverage
        final CloverDatabase database = newDbFromSettings(becauseOfError);

        postLoadDectorators(monitor, database);
        return database;
    }

    private CloverDatabase loadDbAndCoverage(ProjectSettings settings, File coverageDbFile, IProgressMonitor monitor) throws Exception {
        preLoadDecorators(monitor);

        final CloverDatabase database = loadCoverage(loadDbNoCoverage(coverageDbFile), monitor);

        postLoadDectorators(monitor, database);
        return database;
    }

    protected CloverDatabase loadCoverage(CloverDatabase database, IProgressMonitor monitor) throws CloverException {
        database.loadCoverageData(project.newCoverageDataSpec(database));
        logVerbose("Finished loading database for project " + project.getName());
        return database;
    }

    private CloverDatabase loadDbNoCoverage(File coverageDbFile) throws CloverException, IOException {
        CloverDatabase database = loadDb(coverageDbFile);
        //Ensure that we correct any timestamp funny business - say a db file is stamped
        //after its version, we will continually reload
        coverageDbFile.setLastModified(database.getRegistry().getVersion());
        return database;
    }

    private CloverDatabase newDbFromSettings(boolean becauseOfError) {
        if (becauseOfError) {
            project.flagStaleRegistryBecause(CloverEclipsePluginMessages.MARKERS_STALE_DB());
        }
        return project.newEmptyDatabase(contextFilter);
    }

    private CloverDatabase loadDb(File registryFile) throws CloverException, IOException {
        return new CloverDatabase(
            registryFile.getCanonicalPath(),
            project.newIncludeFilter(),
            project.getName(),
            project.getSettings().getContextRegistry().getContextsAsString(contextFilter),
            null);
    }

    private void postLoadDectorators(IProgressMonitor monitor, CloverDatabase database) {
        if (postLoadDecorators.length > 0) {
            logVerbose("Executing post-load decorators for project " + project.getName());
            for (DatabasePostLoadDecorator postLoadDecorator : postLoadDecorators) {
                try {
                    postLoadDecorator.decorate(project, database, monitor);
                } catch (Exception e) {
                    logError("Failed executing post-load decotrator " + postLoadDecorator);
                }
            }
        }
    }

    private void preLoadDecorators(IProgressMonitor monitor) {
        if (preLoadDecorators.length > 0) {
            logVerbose("Executing pre-load decorators for project " + project.getName());
            for (DatabasePreLoadDecorator preLoadDecorator : preLoadDecorators) {
                try {
                    preLoadDecorator.decorate(project, monitor);
                } catch (Exception e) {
                    logError("Failed executing pre-load decotrator " + preLoadDecorator);
                }
            }
        }
    }

    /**
     * Used by system to determine if job should be cancelled when Platform.getJobManager().cancel(family) is called
     */
    @Override
    public boolean belongsTo(Object family) {
        return family == this;
    }
}
