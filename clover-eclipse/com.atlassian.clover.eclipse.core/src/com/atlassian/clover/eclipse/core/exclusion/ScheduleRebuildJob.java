package com.atlassian.clover.eclipse.core.exclusion;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;

public class ScheduleRebuildJob extends Job {
    private final IProject project;
    private final int kind;

    public ScheduleRebuildJob(IProject project) {
        this(project, IncrementalProjectBuilder.FULL_BUILD);
    }

    public ScheduleRebuildJob(IProject project, int kind) {
        super("Rebuilding project");
        this.project = project;
        this.kind = kind;
        setRule(project.getWorkspace().getRuleFactory().buildRule());
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            project.build(kind, monitor);
            return Status.OK_STATUS;
        } catch (CoreException e1) {
            return e1.getStatus();
        }
    }
}
