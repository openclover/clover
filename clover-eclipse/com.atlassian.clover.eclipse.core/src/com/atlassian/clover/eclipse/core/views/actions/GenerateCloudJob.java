package com.atlassian.clover.eclipse.core.views.actions;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.eclipse.core.CloverEclipsePluginMessages;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.ui.editors.cloud.EclipseCloudGenerator;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.eclipse.core.projects.builder.PathUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import java.io.File;
import java.io.IOException;

public abstract class GenerateCloudJob extends Job {
    private IProject project;

    public GenerateCloudJob(IProject project) {
        super(CloverEclipsePluginMessages.GENERATING_CLOUD_FOR(project.getName()));
        this.project = project;
        setUser(true);
        setPriority(Job.BUILD);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            monitor.beginTask("Generating cloud", 4);
            try {
                generateReport(monitor);
            } catch (Throwable e) {
                return new Status(Status.ERROR, CloverPlugin.ID, 0, CloverEclipsePluginMessages.FAILED_TO_GENERATE_CLOUD(), e);
            }

            return activateEditor();
        } finally {
            monitor.done();
        }
    }

    protected abstract IStatus activateEditor();

    protected void generateReport(IProgressMonitor monitor) throws Exception, CloverException, IOException {
        new EclipseCloudGenerator(
                CloverProject.getFor(project).getModel().getDatabase(),
                ensureReportFolderCreated(monitor)).execute();
    }

    protected File ensureReportFolderCreated(IProgressMonitor monitor) throws CoreException {
        IFolder folder = CloverProject.getFor(project).getReportDir();
        if (!folder.exists()) {
            PathUtils.makeDerivedFolder(folder);
        }

        return folder.getLocation().toFile();
    }

}
