package org.openclover.eclipse.core.projects.model;

import org.eclipse.core.runtime.IAdaptable;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;

import java.io.File;

public class WorkingSetHasMetricsFilter implements HasMetricsFilter {
    private CloverProject project;
    private File projectLocation;

    public WorkingSetHasMetricsFilter(CloverProject project) {
        this.project = project;
        this.projectLocation = project.getProject().getLocation().toFile();
    }

    @Override
    public boolean accept(HasMetrics metrics) {
        IAdaptable resource = null;

        if (metrics instanceof ProjectInfo) {
            return CloverPlugin.getInstance().getCloverWorkingSet().includesJavaProject(project.getProject());
        } else if (metrics instanceof FileInfo) {
            return
                CloverPlugin.getInstance().getCloverWorkingSet().includesFile(
                    ((FileInfo)metrics).getPhysicalFile());
        } else {
            return true;
        }
    }

    private String getPackageName(HasMetrics hasMetrics) {
        return
            PackageInfo.DEFAULT_PACKAGE_NAME.equals(hasMetrics.getName())
                ? ""
                : (hasMetrics.getName() + ".");
    }
}
