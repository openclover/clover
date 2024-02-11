package org.openclover.eclipse.core.views.actions;

import org.openclover.runtime.api.CloverException;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.editors.cloud.EclipseCloudGenerator;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.builder.PathUtils;
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
