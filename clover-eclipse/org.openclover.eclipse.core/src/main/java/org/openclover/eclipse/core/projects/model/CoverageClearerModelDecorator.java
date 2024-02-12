package org.openclover.eclipse.core.projects.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.openclover.eclipse.core.projects.CloverProject;

import java.io.File;

public class CoverageClearerModelDecorator implements DatabasePreLoadDecorator {

    @Override
    public void decorate(CloverProject project, final IProgressMonitor monitor) throws Exception {
        final File coverageDbFile = project.getRegistryFile();

        CoverageFilesUtils.deleteCoverageFiles(coverageDbFile, false, monitor);

        monitor.subTask("Refreshing Clover working directory");

        IFile coverageDb = project.getCoverageDbIFile();
        if (coverageDb != null && coverageDb.getParent().exists()) {
            coverageDb.getParent().refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
    }
}